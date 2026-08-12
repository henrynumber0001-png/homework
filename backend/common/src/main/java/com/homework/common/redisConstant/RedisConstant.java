package com.homework.common.redisConstant;

public final class RedisConstant {

    public static final String EMAIL_VERIFY_CODE = "auth:email-code:";
    public static final String EMAIL_VERIFY_CODE_RESEND = "auth:email-code:resend:";
    public static final String EMAIL_SECURE_TICKET = "auth:secure-ticket:";

    //全局限流
    public static final String EMAIL_LIMIT_GLOBAL = "auth:email-code:limit:global:";
    //同一 IP 限流
    public static final String EMAIL_LIMIT_IP = "auth:email-code:limit:ip:";
    //同一邮箱限流
    public static final String EMAIL_LIMIT_ACCOUNT = "auth:email-code:limit:account:";

    private RedisConstant() {
    }
}
