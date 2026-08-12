package com.homework.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.encoder.VerifyCodeGenerator;
import com.homework.common.exception.HomeworkException;
import com.homework.common.redisConstant.RedisConstant;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.utils.TurnstileService;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.web.app.dto.EmailSendDTO;
import com.homework.web.app.dto.EmailVerifyDTO;
import com.homework.web.app.mapper.UserAuthIdentityMapper;
import com.homework.web.app.service.EmailCodeSender;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class EmailCodeService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private final UserAuthIdentityMapper userAuthIdentityMapper;
    private final TurnstileService turnstileService;
    private final StringRedisTemplate stringRedisTemplate;
    private final VerifyCodeGenerator verifyCodeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailCodeSender emailCodeSender;

    private static final int EMAIL_VERIFY_CODE_TTL_SEC = 60;
    private static final int EMAIL_SECURE_TICKET_TTL_SEC = 15*60;
    private final EmailCodeRateLimiter emailCodeRateLimiter;


    public void sendEmailCode(EmailSendDTO emailSendDTO, HttpServletRequest request) {
        //首先验证Turnstile通过了没有
        String remoteIp = request == null ? null : request.getRemoteAddr();
        boolean result = turnstileService.verify(emailSendDTO.getTurnstileToken(), remoteIp);
        if(!result){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_TURNSTILE_VERIFY_ERROR);
        }

        //校验该用户的邮箱是否已经注册过
        LambdaQueryWrapper<UserAuthIdentity> authQueryWrapper = new LambdaQueryWrapper<>();
        authQueryWrapper.eq(UserAuthIdentity::getAccount,normalizeEmail(emailSendDTO.getEmail()))
                .eq(UserAuthIdentity::getProvider, UserAuthIdentityProvider.EMAIL_PASSWORD);

        UserAuthIdentity userAuthIdentity = userAuthIdentityMapper.selectOne(authQueryWrapper);
        if(userAuthIdentity != null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EXIST);
        }

        //生成邮箱的十六进制字符（避免在 Redis Key 中直接暴露邮箱地址）
        String emailHash = hashEmail(normalizeEmail(emailSendDTO.getEmail()));

        //校验全局/IP/Account验证次数
        emailCodeRateLimiter.check(emailHash,remoteIp);

        //通过了之后，要查看是否有 60秒发送锁
        //重复发送锁 = resendKey
        String resendKey = RedisConstant.EMAIL_VERIFY_CODE_RESEND + emailHash; //同一个 email 的 emailHash 不会变

        //如果resendKey不存在，就存入redis（起到占位的作用）
        //如果resendKey存在，就抛异常提示不能再次发送了
        Boolean allowed = stringRedisTemplate.opsForValue().setIfAbsent(
                resendKey, //针对相同的 email，resendKey 和 redisKey 永远不会变
                "1", //因为这个 resendKey 其实没有实质性的对应的 value，这里输入1完全是因为要占位
                EMAIL_VERIFY_CODE_TTL_SEC,
                TimeUnit.SECONDS);

        //如果redis里有这个 resendKey，那么就会插入失败，于是返回 false，那么就是不能再次发送的意思
        if(!Boolean.TRUE.equals(allowed)){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_RESEND_LOCK);
        }

        //如果是第一次发送，或者能够再次发送，那么就生成一个6位数的安全验证码
        String verifyCode = verifyCodeGenerator.generateVerifyCode();
        String redisKey = RedisConstant.EMAIL_VERIFY_CODE + emailHash;

        //本方法没有采取 stringRedisTemplate.opsForValue().set()的模式
        //因为这样写，需要设置两个redisKey，一个是正常用于校验前端返回的验证码的 redisKey，另一个是用于统计验证码错误输入次数的 attemptKey
        //而如果使用stringRedisTemplate.opsForHash().put()，则只需要管理一个正常的 redisKey
        stringRedisTemplate.opsForHash().put(redisKey,"codeHash", passwordEncoder.encode(verifyCode));
        stringRedisTemplate.opsForHash().put(redisKey,"attempt", "0");
        //StringRedisTemplate 不能保存 Integer 类型的 Hash value, 后续的 increment() 仍然可以对字符串 "0" 执行数值递增。

        //设置 redisKey 的过期时间
        stringRedisTemplate.expire(redisKey, EMAIL_VERIFY_CODE_TTL_SEC, TimeUnit.SECONDS);

        //准备就绪，就发送验证码到腾讯云服务器
        try{
            emailCodeSender.sendCode(normalizeEmail(emailSendDTO.getEmail()), verifyCode);
        }catch (Exception exception){
            //其实删除的主要目的是删除这个resendKey，因为它站着当前内存，用户在1分钟内没办法再次成功发送验证码（但前端会因为发生失败而不显示倒计时）
            //所以，发送失败，删除resendKey，那么用户就可以立即再次发送验证码
            stringRedisTemplate.delete(resendKey);
            //删除redisKey，只是为了业务逻辑上一致，实际上你就算不删除 redisKey，新的 verifyCode 出来之后，使用 put()/set() 依然会覆盖原来的 redis键值对
            stringRedisTemplate.delete(redisKey);
            throw exception;
        }
    }


    private String normalizeEmail(String email){
        if(!StringUtils.hasText(email)){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EMPTY);
        }
        String normalizedEmail = email.trim();
        if(!EMAIL_PATTERN.matcher(normalizedEmail).matches()){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String standardEmail = normalizedEmail.toLowerCase(Locale.ROOT);
        return standardEmail;
    }

    //只是为了生成不直接暴露邮箱地址的 Redis Key
    //SHA-256 在这里的作用只是让 Redis Key 不直接显示邮箱，它是确定性的、不可逆哈希，不是加密
    private String hashEmail(String standardEmail) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); //SHA-256 是确定性算法，同一个输入始终产生相同结果
            byte[] hash = digest.digest(standardEmail.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }


    //用户通过邮箱收到验证码之后，输入这条验证码，开启验证功能
    public String verifyEmailCode(EmailVerifyDTO emailVerifyDTO) {
        if(!StringUtils.hasText(emailVerifyDTO.getEmail()) || !StringUtils.hasText(emailVerifyDTO.getCode())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        //首先校验该用户的邮箱是否已经注册过
        LambdaQueryWrapper<UserAuthIdentity> authQueryWrapper = new LambdaQueryWrapper<>();
        authQueryWrapper.eq(UserAuthIdentity::getAccount,normalizeEmail(emailVerifyDTO.getEmail()));

        UserAuthIdentity userAuthIdentity = userAuthIdentityMapper.selectOne(authQueryWrapper);
        if(userAuthIdentity != null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EXIST);
        }

        String redisKey = RedisConstant.EMAIL_VERIFY_CODE + hashEmail(normalizeEmail(emailVerifyDTO.getEmail()));

        //注意：BCrypt 哈希值 本身就是 字符串类型，通过stringRedisTemplate 存入的也是字符串类型，
        //只是在 get 时，因为链式调用的问题，Java 编译器把值类型 HV 推断成了 Object，因此需要强转一下
        String codeHash = (String)stringRedisTemplate.opsForHash().get(redisKey, "codeHash");
        if(!StringUtils.hasText(codeHash)){
            throw new HomeworkException(ResultCodeEnum.EMAIL_CODE_EXPIRED);
        }
        //这里可以用Lua进行原子操作

        //BCrypt 哈希值 本来就是字符串，所以 matches方法的参数使用字符串，是没问题的
        if(!passwordEncoder.matches(emailVerifyDTO.getCode(), codeHash)){
            Long attempt = stringRedisTemplate.opsForHash().increment(redisKey, "attempt", 1);
            //redisKey 的过期时间，已经在 sendEmailCode 里设置过了
            //这里有一个非常细的细节：
            //限额5次，那么必须是让 attempt >= 5 而不是 attempt > 5, 因为第五次输错，就应该立刻删除
            //如果第五次还没删除，但第六次成功了，这就不符合限额5次的安全标准了
            if (attempt >= 5) {
                stringRedisTemplate.delete(redisKey);
            }
            throw new HomeworkException(ResultCodeEnum.EMAIL_CODE_ERROR);
        }

        //如果校验成功了
        stringRedisTemplate.delete(redisKey);

        //生成一个邮箱验证成功token给到前端，防止有人绕过前端直接操作后端进入账户
        String secureTicket = verifyCodeGenerator.generateSecureTicket();

        //把这个邮箱验证成功token 装入 redis 中，secureTicketKey : email
        String secureTicketKey = RedisConstant.EMAIL_SECURE_TICKET + secureTicket;
        stringRedisTemplate.opsForValue().set(secureTicketKey, normalizeEmail(emailVerifyDTO.getEmail()),EMAIL_SECURE_TICKET_TTL_SEC,TimeUnit.SECONDS);

        return secureTicket;

    }



}
