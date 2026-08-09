package com.homework.web.app.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.model.enums.UserAuthIdentityStatus;
import com.homework.model.enums.UserInfoStatus;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.utils.JwtUtil;
import com.homework.common.utils.TurnstileService;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.model.entity.UserInfo;
import com.homework.web.app.dto.*;
import com.homework.web.app.mapper.UserAuthIdentityMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.service.ThirdPartyAuthService;
import com.homework.web.app.service.UserAuthIdentityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserAuthIdentityServiceImpl extends ServiceImpl<UserAuthIdentityMapper, UserAuthIdentity> implements UserAuthIdentityService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");


    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private TurnstileService turnstileService;
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ThirdPartyAuthService thirdPartyAuthService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String registerByEmail(EmailRegisterDTO emailRegisterDTO, HttpServletRequest request) {

        if(emailRegisterDTO == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if(!StringUtils.hasText(emailRegisterDTO.getEmail())){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EMPTY);
        }

        if(!StringUtils.hasText(emailRegisterDTO.getPassword()) || !StringUtils.hasText(emailRegisterDTO.getPasswordConfirm())){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_PASSWORD_EMPTY);
        }

        if(!emailRegisterDTO.getPassword().equals(emailRegisterDTO.getPasswordConfirm())){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_PASSWORD_CONFIRM_ERROR);
        }

        if(!StringUtils.hasText(emailRegisterDTO.getDisplayName())){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_DISPLAY_NAME_EMPTY);
        }

        String remoteIp = request == null ? null : request.getRemoteAddr();
        Boolean result = turnstileService.verify(emailRegisterDTO.getTurnstileToken(), remoteIp);
        if(!result){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_TURNSTILE_VERIFY_ERROR);
        }

        String identifier = identifierOfEmail(emailRegisterDTO.getEmail());
        String identifierNormalized = normalizeEmail(identifier); //应该按照normalized之后的查，扩大查重范围，否则大小写同名邮箱会被认为是不重复；
        LambdaQueryWrapper<UserAuthIdentity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAuthIdentity::getIdentifierNormalized, identifierNormalized);

        if(this.getOne(queryWrapper) != null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EXIST);
        }


        UserInfo userInfo = new UserInfo();
        UserAuthIdentity userAuthIdentity = new UserAuthIdentity();

        userInfo.setDisplayName(emailRegisterDTO.getDisplayName());
        userInfo.setStatus(UserInfoStatus.ACTIVE);
        insertUserInfoWithAccountNo(userInfo);

        userAuthIdentity.setUserId(userInfo.getId());
        userAuthIdentity.setProvider(UserAuthIdentityProvider.EMAIL_PASSWORD);
        userAuthIdentity.setIdentifier(identifier);
        userAuthIdentity.setIdentifierNormalized(identifierNormalized);
        userAuthIdentity.setStatus(UserAuthIdentityStatus.VERIFIED);
        userAuthIdentity.setPasswordHash(bCryptPasswordEncoder.encode(emailRegisterDTO.getPassword()));
        userAuthIdentity.setVerifiedTime(LocalDateTime.now());
        userAuthIdentity.setLastUsedTime(LocalDateTime.now());
        this.save(userAuthIdentity);

        String token = jwtUtil.createToken(userInfo.getId(),userInfo.getAccountNo());
        return token;
    }

    @Override
    public String registerByOAuth(ThirdPartyRegisterDTO thirdPartyRegisterDTO,HttpServletRequest request) {

        if(thirdPartyRegisterDTO == null || thirdPartyRegisterDTO.getIdentityProvider() == null || !StringUtils.hasText(thirdPartyRegisterDTO.getAuthCode())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        String remoteIp = request == null ? null : request.getRemoteAddr();
        Boolean result = turnstileService.verify(thirdPartyRegisterDTO.getTurnstileToken(), remoteIp);
        if(!result){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_TURNSTILE_VERIFY_ERROR);
        }

        ThirdPartyUser thirdPartyUser = thirdPartyAuthService.verifyAndGetUser(thirdPartyRegisterDTO.getIdentityProvider(), thirdPartyRegisterDTO.getAuthCode());
        //在 registerByOAuth中，只有 ThirdPartyUser
        //ThirdPartyUser -> ThirdPartyAuthHandler -> ThirdPartyAuthHandlerImpl -> GoogleAuthHandler -> GoogleIdToken

        String identifier = thirdPartyUser.getExternalUserId();
        String identifierNormalized = identifier.trim(); //没有什么实际的业务意义，因为第三方返回的不可能带空格，所以主要是用来和邮箱/手机号的identifierNormalized保持统一

        LambdaQueryWrapper<UserAuthIdentity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAuthIdentity::getProvider, thirdPartyRegisterDTO.getIdentityProvider())
                .eq(UserAuthIdentity::getIdentifierNormalized, identifierNormalized);

        if (this.getOne(wrapper) != null) {
            throw new HomeworkException(ResultCodeEnum.REPEAT_SUBMIT); // 或者定义 THIRD_PARTY_ACCOUNT_EXIST
        }


        UserInfo userInfo = new UserInfo();
        UserAuthIdentity userAuthIdentity = new UserAuthIdentity();

        String displayName = thirdPartyUser.getDisplayName();//从第三方返回的displayName是有可能为null的，所以要做一个兜底条件
        if(!StringUtils.hasText(displayName)){
            displayName = thirdPartyUser.getEmail(); //ThirdPartyUser 中的 email 属性，用于 displayName 返回缺失时候的兜底条件
        }
        if(!StringUtils.hasText(displayName)){ //第二个兜底，因为如果email也返回null，那么就要自创一个displayName
            displayName = thirdPartyRegisterDTO.getIdentityProvider().getName() + "用户" + RandomUtil.randomNumbers(6);
        }
        userInfo.setDisplayName(displayName);
        userInfo.setStatus(UserInfoStatus.ACTIVE);
        insertUserInfoWithAccountNo(userInfo);

        userAuthIdentity.setUserId(userInfo.getId());
        userAuthIdentity.setProvider(thirdPartyUser.getProvider()); // ThirdPartyUser 是服务端验证成功后生成的结果，更可信。
        userAuthIdentity.setIdentifier(identifier);
        userAuthIdentity.setIdentifierNormalized(identifierNormalized);
        userAuthIdentity.setStatus(UserAuthIdentityStatus.VERIFIED);
        userAuthIdentity.setVerifiedTime(LocalDateTime.now());
        userAuthIdentity.setLastUsedTime(LocalDateTime.now());
        this.save(userAuthIdentity);

        String token = jwtUtil.createToken(userInfo.getId(),userInfo.getAccountNo());
        return token;

    }

    @Override
    public String loginByEmail(EmailLoginDTO emailLoginDTO,HttpServletRequest request) {
        if(emailLoginDTO == null){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        if(!StringUtils.hasText(emailLoginDTO.getEmail())){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EMPTY);
        }

        if(!StringUtils.hasText(emailLoginDTO.getPassword())){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_PASSWORD_EMPTY);
        }

        String remoteIp = request == null ? null : request.getRemoteAddr();
        Boolean result = turnstileService.verify(emailLoginDTO.getTurnstileToken(), remoteIp);
        if(!result){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_TURNSTILE_VERIFY_ERROR);
        }


        //前置校验通过后，开始查询用户身份信息
        //先查用户信息是否存在，如果存在，再用前端传来的密码和数据库中保存的密码进行匹配
        String identifier = identifierOfEmail(emailLoginDTO.getEmail());
        String identifierNormalized = normalizeEmail(identifier);

        LambdaQueryWrapper<UserAuthIdentity> userAuthIdentityWrapper = new LambdaQueryWrapper<>();
        userAuthIdentityWrapper.eq(UserAuthIdentity::getIdentifierNormalized,identifierNormalized)
                .select(UserAuthIdentity::getUserId,UserAuthIdentity::getPasswordHash,UserAuthIdentity::getStatus);//只查需要的字段，提高查询性能

        UserAuthIdentity fetchedUserAuthIdentity = this.getOne(userAuthIdentityWrapper);

        if(fetchedUserAuthIdentity == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }

        //用户存在，再用前端传来的密码和数据库中保存的密码进行匹配

        boolean isPasswordMatch = bCryptPasswordEncoder.matches(emailLoginDTO.getPassword(), fetchedUserAuthIdentity.getPasswordHash());
        if(!isPasswordMatch){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_PASSWORD_ERROR);
        }

        //最后验证用户状态
        if(fetchedUserAuthIdentity.getStatus() != UserAuthIdentityStatus.VERIFIED){
            throw new HomeworkException(ResultCodeEnum.APP_ACCOUNT_STATUS_ERROR);
        }

        //全部验证通过，加上accountNo之后，就可以发JWT了
        Long userId = fetchedUserAuthIdentity.getUserId();
        String accountNo = userInfoMapper.selectById(userId).getAccountNo();

        String token = jwtUtil.createToken(userId,accountNo);
        return token;


    }

    @Override
    public String loginByOAuth(ThirdPartyLoginDTO thirdPartyLoginDTO,HttpServletRequest request) {
        if(thirdPartyLoginDTO == null || thirdPartyLoginDTO.getIdentityProvider() == null || !StringUtils.hasText(thirdPartyLoginDTO.getAuthCode())){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }

        ThirdPartyUser thirdPartyUser = thirdPartyAuthService.verifyAndGetUser(thirdPartyLoginDTO.getIdentityProvider(), thirdPartyLoginDTO.getAuthCode());
        if(thirdPartyUser == null){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_USER_NOT_EXIST);
        }

        String remoteIp = request == null ? null : request.getRemoteAddr();
        Boolean result = turnstileService.verify(thirdPartyLoginDTO.getTurnstileToken(), remoteIp);
        if(!result){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_TURNSTILE_VERIFY_ERROR);
        }

        String identifier = thirdPartyUser.getExternalUserId();
        String identifierNormalized = identifier.trim();

        LambdaQueryWrapper<UserAuthIdentity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAuthIdentity::getProvider, thirdPartyLoginDTO.getIdentityProvider())
                .eq(UserAuthIdentity::getIdentifierNormalized, identifierNormalized)
                .select(UserAuthIdentity::getUserId,UserAuthIdentity::getStatus);

        UserAuthIdentity fetchedAuthIdentity = this.getOne(queryWrapper);
        if(fetchedAuthIdentity == null){
            UserInfo userInfo = new UserInfo();
            UserAuthIdentity userAuthIdentity = new UserAuthIdentity();

            String displayName = thirdPartyUser.getDisplayName();
            if(!StringUtils.hasText(displayName)){
                userInfo.setDisplayName(thirdPartyUser.getEmail());
            }
            if(!StringUtils.hasText(displayName)){
                displayName = thirdPartyUser.getProvider().getName() + "用户" + RandomUtil.randomNumbers(6);
            }

            userInfo.setDisplayName(displayName);
            userInfo.setStatus(UserInfoStatus.ACTIVE);
            userInfo.setCreatedTime(LocalDateTime.now());
            userInfo.setUpdatedTime(LocalDateTime.now());
            insertUserInfoWithAccountNo(userInfo);

            userAuthIdentity.setUserId(userInfo.getId());
            userAuthIdentity.setProvider(thirdPartyUser.getProvider());
            userAuthIdentity.setIdentifier(identifier);
            userAuthIdentity.setIdentifierNormalized(identifierNormalized);
            userAuthIdentity.setStatus(UserAuthIdentityStatus.VERIFIED);
            userAuthIdentity.setVerifiedTime(LocalDateTime.now());
            userAuthIdentity.setLastUsedTime(LocalDateTime.now());
            this.save(userAuthIdentity);

            //设计思路是“第一次使用 Google/微信登录时自动创建账号”，那么保存以后要把新身份赋给原变量
            fetchedAuthIdentity = userAuthIdentity;
        }

        if(fetchedAuthIdentity.getStatus()!= UserAuthIdentityStatus.VERIFIED){
            throw new HomeworkException(ResultCodeEnum.APP_ACCOUNT_STATUS_ERROR);
        }
        Long userId = fetchedAuthIdentity.getUserId();
        String accountNo = userInfoMapper.selectById(userId).getAccountNo();
        String token = jwtUtil.createToken(userId,accountNo);
        return token;
    }

    private String identifierOfEmail(String email){
        if(!StringUtils.hasText(email)){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_EMAIL_EMPTY);
        }
        String identifier = email.trim();
        if(!EMAIL_PATTERN.matcher(identifier).matches()){
            throw new HomeworkException(ResultCodeEnum.PARAM_ERROR);
        }
        return identifier;
    }

    public String normalizeEmail(String identifier){
        String identifierNormalized = identifier.toLowerCase(Locale.ROOT);
        return identifierNormalized;
    }

    private void insertUserInfoWithAccountNo(UserInfo userInfo){
        for (int i = 0; i < 5; i++) {
            userInfo.setAccountNo(generateAccountNo());
            try{
                userInfoMapper.insert(userInfo);
                return;
            }catch (DuplicateKeyException e){ //若想实现这一步，必须在数据库里，给 account_no 加 唯一索引：uk_user_info_account_no
                if(i == 4){ //只有当第五次尝试的时候，才会抛异常 并 终止方法的执行，前面四次，在出现DuplicateKeyException后，方法体内没有需要执行的指令，因此不抛异常，继续重试。
                    throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR);
                }
            }
        }
    }
    /*
    i < 5 不是固定标准，是一个“足够保守的重试上限”。
    你的 accountNo 是 10 位随机数字：
    1000000000 ~ 9999999999
    大约有 90 亿个可用号码。正常用户量下，撞号概率非常低。所以通常插入一次就成功，重试只是为了处理极小概率的随机冲突。
    为什么不是无限循环？
    因为如果数据库有问题、生成器有 bug、唯一索引异常、号码池接近耗尽，无限循环会卡住请求。所以必须设置上限。
    为什么 5 次够？
    因为 10 位随机数空间很大。假设已经有 100 万用户，可用空间约 90 亿，单次撞号概率大约是：
    1,000,000 / 9,000,000,000 = 0.011%
    连续 5 次都撞号的概率极低。

    最多尝试生成 5 个 accountNo。
    如果 5 次都撞唯一索引，说明不是普通随机冲突，而是系统状态异常，直接抛 SERVICE_ERROR。
     */

    private String generateAccountNo(){
        return String.valueOf(RandomUtil.randomLong(1_000_000_000L,10_000_000_000L)); //生成一个介于1,000,000,000 和 9999999999 之间的数字
    }
}
