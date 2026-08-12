package com.homework.web.app.service.impl;

import com.homework.common.exception.HomeworkException;
import com.homework.common.redisConstant.RedisConstant;
import com.homework.common.result.ResultCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

@Service
@AllArgsConstructor
public class EmailCodeRateLimiter {

    /**
     * 全局限制：
     * 整个系统一分钟最多允许请求发送 100 次邮箱验证码。
     *
     * 这个限制保护的是腾讯云 SES 配额以及服务器资源，
     * 防止大量不同 IP、不同邮箱一起请求。
     *
     * 注意：这个字段的值 是 次数，不是时间！
     */
    private static final long GLOBAL_LIMIT_PER_MINUTE = 100;
    //这些字段是业务规则，不应该写进 RedisConstant

    /**
     * IP 限制：
     * 同一个 IP 地址一小时最多请求 20 次。
     *
     * 例如某个用户不断更换邮箱注册，
     * 邮箱限制拦不住他，但 IP 限制可以进行第二层拦截。
     */
    private static final long IP_LIMIT_PER_HOUR = 20;

    /**
     * 邮箱限制：
     * 同一个邮箱一小时最多请求 5 次。
     *
     * 这和 60 秒 resendKey 的作用不同：
     *
     * resendKey：60 秒内不能重复发送。
     * EMAIL_LIMIT_PER_HOUR：即使每次都等待 60 秒，
     * 一小时最多也只能发送 5 次。
     */
    private static final long ACCOUNT_LIMIT_PER_HOUR = 5;

    //要么使用 timeout + TimeUnit
    //要么使用 Duration
    //二选一
    //它们最终都会被 Spring Data Redis 转换成 Redis 命令
    //区别就是 timeout 没有时间单位，因此一定要跟 TimeUnit
    //说人话就是：你写的是 Spring Redis 的语法，不是真正的 Redis 语法
    private static final Duration GLOBAL_LIMIT_TTL = Duration.ofMinutes(2);
    private static final Duration IP_LIMIT_TTL = Duration.ofHours(2);
    private static final Duration ACCOUNT_LIMIT_TTL = Duration.ofHours(2);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 依次检查三种限制：
     *
     * 1. 整个系统一分钟的请求数量
     * 2. 当前 IP 一小时的请求数量
     * 3. 当前邮箱一小时的请求数量
     *
     * 任意一项超过上限，都会抛出业务异常，
     * 后面的发送验证码代码便不会执行。
     *
     * @param emailHash 规范化邮箱经过 SHA-256 后的值
     * @param remoteIp  当前请求的客户端 IP
     * 注意：check()方法是接在 EmailCodeService.sendEmailCode()方法内的，所以 emailHash 和 remoteIp 都在 sendMailCode()方法中获得了，不用自己重新创造
     */
    public void check(String emailHash, String remoteIp) {
        //获取当前 Unix 时间戳，单位是秒
        //给机器看的时间戳，UTC全球统一时间
        long now = Instant.now().getEpochSecond();

        /**
         * now / 60 可以得到当前属于哪一个“分钟窗口”。
         * now 是当前时间戳，单位是秒，所以 now / 60 的结果是当前分钟整数。
         *
         * 当前分钟内，globalKey 保持不变（因为分钟数没变）
         * Redis Key 也会随之变化，开始一个新的统计窗口。
         */
        String globalKey = RedisConstant.EMAIL_LIMIT_GLOBAL + now / 60;
        /**
         * 检查整个系统的发送次数。
         *
         * globalKey：
         * 当前分钟使用的 Redis Key。
         *
         * GLOBAL_LIMIT_PER_MINUTE：
         * 最多允许 100 次。
         *
         * Duration.ofMinutes(2)：
         * 让这个统计 Key 在两分钟后自动删除。
         *
         * 当前统计窗口实际是一分钟。
         * 设置两分钟过期，是为了给 Redis 留一点清理余量。
         */
        checkLimit(globalKey, GLOBAL_LIMIT_PER_MINUTE, GLOBAL_LIMIT_TTL); //这个 globalKey，对应的值是 long类型的 100， 两分钟后删除

        /**
         * 不直接把用户 IP 放进 Redis Key。
         *
         * 例如不使用：auth:email-code:limit:ip:192.168.1.10
         *
         * 而是先对 IP 进行 SHA-256：
         * auth:email-code:limit:ip:<ipHash>:<小时窗口>
         * 这样可以避免在 Redis Key 中直接展示用户 IP。
         */
        if (StringUtils.hasText(remoteIp)) {
            /**
             * now / 3600 可以得到当前属于哪一个“小时窗口”。
             * now 是当前时间戳，单位是秒，所以 now / 3600 的结果是当前小时整数。
             *
             * 当前小时内，同一个 IP 生成的 Key 保持不变（因为小时数没变）
             * 进入下一个小时后，Key 会发生变化。
             */
            String ipKey = RedisConstant.EMAIL_LIMIT_IP + sha256(remoteIp) + ":" + now / 3600;
            checkLimit(ipKey, IP_LIMIT_PER_HOUR, IP_LIMIT_TTL);
        }

        String emailKey = RedisConstant.EMAIL_LIMIT_ACCOUNT + emailHash + ":" + now / 3600;
        checkLimit(emailKey, ACCOUNT_LIMIT_PER_HOUR, ACCOUNT_LIMIT_TTL);
    }

    /**
     * 对某一个 Redis Key 执行次数限制。
     *
     * 例如：
     *
     * Key   = auth:email-code:limit:account:xxx:497212
     * Value = 3
     *
     * 表示这个邮箱在当前小时窗口内，
     * 已经请求发送了 3 次验证码。
     *
     * @param key   当前限流对象使用的 Redis Key
     * @param limit 当前时间窗口最多允许多少次
     * @param ttl   Redis Key 多久后自动删除
     */
    private void checkLimit(String key, long limit, Duration ttl) {

        /**
         * 尝试创建一个新的计数器。
         * 它对应的 Redis 思路是：SET key 1 NX EX ttl
         *
         * setIfAbsent 的意思是：
         * 如果 Key 不存在：创建 Key，Value 设置为 "1"，同时设置过期时间；返回 true。
         *
         * 如果 Key 已存在：不修改原来的 Value；返回 false。
         *
         * 为什么初始值是 1？
         * 因为能够执行到这里的当前请求，
         * 就是这个时间窗口内的第一次请求。
         */
        Boolean requestAllowed = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        long requestCount; //计数器的 Key 保存的值，其实等价于请求次数：

        if (Boolean.TRUE.equals(requestAllowed)) {
            //当 requestAllowed 的结果是 true，那说明一定是下一个小时/分钟了，换了新的key（原来的key已经失效删除了）
            //既然是新的key，那么计数器就要重置，从1开始
            requestCount = 1;
        } else {
            /**
             * Key 已经存在，说明此前已经有请求。
             * increment(key) 相当于 Redis 的：INCR key
             *
             * 假设原来的值是 "3"，
             * 执行后 Redis 会把它改成 "4"，
             * 并返回 Long 类型的 4。
             *
             * 虽然 StringRedisTemplate 存的是字符串，
             * 但 Redis 可以对内容为数字的字符串执行 INCR。
             */

            /*
            调用 Redis 的 INCR key 命令，将 key 的值原子地加 1，并返回加 1 之后的新值。
            INCR 的规则是：
            Key 存在并且 Value 是数字：Value 加 1
            Key 不存在：创建 Key，并将 Value 设置为 1
            Key 存在但 Value 不是数字：报错

            increment(key) 返回的是 Long 类型的 key 的 value，是通过INCR将 字符串value 转换成 long类型的value
            但是字符串类型的 value 依然被存储到了 redis 中的 value
            只是 increment(key) 的返回值是 long 类型，即 incremented = long类型的 value（+1之后的）
             */
            Long incremented = stringRedisTemplate.opsForValue().increment(key);
            if(incremented == null){ //null 是一种特殊情况，为了保证安全和稳定性，把计数器设置到超过限制次数，最终让java报错
                requestCount = limit + 1;
            }else{
                requestCount = incremented;
            }

            // 检查当前这个 key 有没有过期时间
            Long remainingTtl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);

            // ttl = -1， 表示这个 key 永不过期； ttl = -2，表示这个 key 已经过期/不存在
            // 这种情况下，重新设置过期时间，避免出现 永不过期的key
            if (remainingTtl != null && remainingTtl < 0) {
                // 重新设置 TTL，避免这个计数 Key 永久保留
                // 即使它属于上一个时间窗口，重新补上 TTL 也没关系：
                // 新的时间窗口会生成新的 Key，旧 Key 不会再参与新窗口的计数。
                // 旧 Key 只是在 Redis 中多保留一段时间，最终仍会自动删除。
                stringRedisTemplate.expire(key, ttl);

            }
        }

        // 尝试次数超过当前key的规定限制，报错
        if (requestCount > limit) {
            throw new HomeworkException(
                    ResultCodeEnum.EMAIL_CODE_RATE_LIMITED
            );
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
