package com.homework.common.result;


import lombok.Getter;

@Getter
public enum ResultCodeEnum {

    SUCCESS(200, "success"),
    FAIL(500, "failed"),

    PARAM_ERROR(202, "参数不正确"),
    SERVICE_ERROR(203, "服务异常"),
    DATA_ERROR(204, "数据异常"),
    SYSTEM_ERROR(205, "系统异常"),
    REPEAT_SUBMIT(206, "重复提交"),


    APP_LOGIN_NOT_AUTH(501, "未登陆"),
    APP_LOGIN_EMAIL_EMPTY(502, "邮箱为空"),
    APP_LOGIN_EMAIL_EXIST(511,"邮箱已存在"),
    APP_LOGIN_PASSWORD_EMPTY(503, "密码为空"),
    APP_LOGIN_PASSWORD_CONFIRM_ERROR(504, "密码不一致"),
    APP_LOGIN_PASSWORD_ERROR(505,"密码错误"),
    APP_LOGIN_TURNSTILE_VERIFY_ERROR(506, "人机验证失败，请重试"),
    APP_LOGIN_DISPLAY_NAME_EMPTY(507,"昵称为空"),



    APP_LOGIN_PHONE_EMPTY(504, "手机号码为空"),
    APP_LOGIN_CODE_EMPTY(505, "验证码为空"),
    APP_SEND_SMS_TOO_OFTEN(506, "验证法发送过于频繁"),
    APP_LOGIN_CODE_EXPIRED(507, "验证码已过期"),
    APP_LOGIN_CODE_ERROR(508, "验证码错误"),
    APP_LOGIN_USER_NOT_EXIST(509, "用户不存在"),
    APP_ACCOUNT_DISABLED_ERROR(509, "该用户已被禁用"),
    APP_ACCOUNT_STATUS_ERROR(510, "账户状态异常"),


    APP_AI_EVALUATION_EMPTY_SCORE(513, "该用户并未作答"),

    EXAM_EXPIRED(514, "考试已到期，系统已自动交卷"),

    TOKEN_EXPIRED(601, "token过期"),
    TOKEN_INVALID(602, "token非法"),

    HIT_ID_ERROR(701,"Hit ID 不能为空"),
    HIT_NOT_EXIST(702,"Hit 不存在或不可访问"),
    HIT_CONTENT_EMPTY_ERROR(703,"Hit 内容不能为空"),
    HIT_CONTENT_TOO_LONG_ERROR(704,"Hit 内容最多 140 字"),
    HIT_TAG_TOO_LONG_ERROR(705,"单个标签最多 30 字"),
    HIT_TAG_COUNT_ERROR(706,"一条 Hit 最多添加 10 个标签"),
    HIT_TAG_FORMAT_ERROR(707,"Hit 标签格式不正确"),
    HIT_COMMENT_TOO_LONG_ERROR(708,"Hit 评论最多 300 字"),
    COMMENT_NOT_EXIST(709,"评论不存在"),

    MEMBERSHIP_REQUIRED(801, "需要有效的 Standard 或 Premium 会员"),
    PREMIUM_MEMBERSHIP_REQUIRED(802, "AI 评分和 AI 追问仅对 Premium 会员开放"),
    MEMBERSHIP_PLAN_NOT_FOUND(803, "会员套餐不存在或已下架"),
    MEMBERSHIP_INVALID_CHANGE(804, "不支持该会员变更"),
    MEMBERSHIP_CHANGE_IN_PROGRESS(805, "已有会员变更或待支付订单正在处理"),
    MEMBERSHIP_ORDER_NOT_FOUND(806, "会员订单不存在"),
    MEMBERSHIP_ORDER_STATE_ERROR(807, "会员订单状态不允许当前操作"),
    MEMBERSHIP_PAYMENT_MISMATCH(808, "支付金额或订单信息不匹配"),
    MEMBERSHIP_BILLING_TYPE_MISMATCH(809, "升级会员时必须保持相同的计费周期"),
    MEMBERSHIP_PAYMENT_CHANNEL_UNAVAILABLE(810, "所选支付渠道暂不可用"),
    MEMBERSHIP_PAYMENT_GATEWAY_ERROR(811, "支付平台暂时不可用，请稍后重试");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
