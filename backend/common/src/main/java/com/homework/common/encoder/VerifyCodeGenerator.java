package com.homework.common.encoder;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class VerifyCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateVerifyCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }


    public String generateSecureTicket(){
        //创建一个长度为 32 字节 的空字节数组
        //32 字节 = 256 位（bit）
        //每一位有 0/1 两种可能，总共有 2^256 种组合
        //这是一个极其保守的安全选择：
        //128 位熵已被认为是 "宇宙级不可破解"（2^128 ≈ 3.4×10^38），256 位是 AES-256 的密钥长度，属于军事级安全
        //通常令牌用 16 字节（128 位）就够了，32 字节是加倍保险
        byte[] bytes = new byte[32];

        //用 SecureRandom（加密安全随机数生成器）把 32 字节数组填满随机值，最终生成 32 字节（256 位）的真随机数据。
        secureRandom.nextBytes(bytes);

        //再用 URL 安全且无填充的 Base64 编码成 43 字符的字符串
        return Base64.getUrlEncoder(). //获取 URL 安全的 Base64 编码器
                withoutPadding(). //去掉填充符 =
                encodeToString(bytes); //编码为字符串
    }
}
// %:格式说明符的起始标记，告诉解析器 "后面是格式化指令"
// 0:填充字符，如果生成的数字不足6位，那么前面用0填充
// 6:最小输出 6 个字符，不够就补
// d:转换符，十进制整数

//secureRandom.nextInt：内部 CSPRNG 状态就会不可逆地更新一次，下一次输出与上一次之间没有可推导的数学关系