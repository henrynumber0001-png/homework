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
    APP_LOGIN_PASSWORD_LENGTH_ERROR(519,"密码长度必须在8到16个字符之间"),
    APP_LOGIN_TURNSTILE_VERIFY_ERROR(506, "人机验证失败，请重试"),
    APP_LOGIN_DISPLAY_NAME_EMPTY(507,"昵称为空"),
    APP_LOGIN_USER_NOT_EXIST(508, "用户不存在"),
    APP_ACCOUNT_DISABLED_ERROR(509, "该用户已被禁用"),
    APP_ACCOUNT_STATUS_ERROR(510, "账户状态异常"),
    APP_LOGIN_EMAIL_RESEND_LOCK(512, "邮箱验证码发送过于频繁，请稍后再试"),
    APP_AI_EVALUATION_EMPTY_SCORE(513, "该用户并未作答"),
    APP_VERSION_CONFLICT(520,"数据已变化，请刷新后重试"),
    EMAIL_CODE_SEND_FAILED(514, "邮箱验证码发送失败"),
    EMAIL_CODE_EXPIRED(515, "邮箱验证码已过期"),
    EMAIL_CODE_ERROR(516,"邮箱验证码错误"),
    EMAIL_SECURE_TICKET_ERROR(517, "SecureTicket错误"),
    EMAIL_CODE_RATE_LIMITED(518, "邮箱验证码发送过于频繁，请稍后再试"),

    EXAM_EXPIRED(914, "考试已到期，系统已自动交卷"),

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
    COMMUNITY_POST_RESTRICTED(710, "当前账号暂时不能发布动态"),
    COMMUNITY_COMMENT_RESTRICTED(711, "当前账号暂时不能发表评论"),

    MEMBERSHIP_REQUIRED(801, "需要有效的 Premium 或 Premium Plus 会员"),
    PREMIUM_PLUS_MEMBERSHIP_REQUIRED(802, "该功能仅对 Premium Plus 会员开放"),
    MEMBERSHIP_PLAN_NOT_FOUND(803, "会员套餐不存在或已下架"),
    MEMBERSHIP_INVALID_CHANGE(804, "不支持该会员变更"),
    MEMBERSHIP_CHANGE_IN_PROGRESS(805, "已有会员变更或待支付订单正在处理"),
    MEMBERSHIP_ORDER_NOT_FOUND(806, "会员订单不存在"),
    MEMBERSHIP_ORDER_STATE_ERROR(807, "会员订单状态不允许当前操作"),
    MEMBERSHIP_PAYMENT_MISMATCH(808, "支付金额或订单信息不匹配"),
    MEMBERSHIP_DIFF_UPGRADE_UNAVAILABLE(809, "当前 Premium 剩余时长不支持该补差档位"),
    MEMBERSHIP_PAYMENT_CHANNEL_UNAVAILABLE(810, "所选支付渠道暂不可用"),
    MEMBERSHIP_PAYMENT_GATEWAY_ERROR(811, "支付平台暂时不可用，请稍后重试"),
    MEMBERSHIP_SUSPENDED(812, "会员访问已被暂停"),

    ADMIN_NOT_AUTHENTICATED(1001, "管理员未登录"),
    ADMIN_CREDENTIALS_INVALID(1002, "邮箱或密码错误"),
    ADMIN_ACCOUNT_UNAVAILABLE(1003, "管理员账号不可用"),
    ADMIN_PERMISSION_DENIED(1004, "无权执行该管理操作"),
    ADMIN_BANK_SCOPE_DENIED(1005, "无权访问该题库"),
    ADMIN_SESSION_REVOKED(1006, "管理员会话已失效"),
    ADMIN_INVITATION_INVALID(1007, "管理员邀请无效或已过期"),
    ADMIN_REAUTH_INVALID(1008, "二次认证无效或已过期"),
    ADMIN_JWT_SECRET_KEY_TOO_SHORT(1009, "管理员 JWT 秘钥长度不足"),
    ADMIN_ACCOUNT_NOT_FOUND(1101, "管理员不存在"),
    ADMIN_ACCOUNT_CONFLICT(1102, "管理员邮箱或状态冲突"),
    ADMIN_BANK_NOT_FOUND(1201, "题库不存在"),
    ADMIN_BANK_NAME_CONFLICT(1202, "题库名称已存在"),
    ADMIN_BANK_STATE_INVALID(1203, "题库状态不允许当前操作"),
    ADMIN_BANK_CATEGORY_INVALID(1204, "题库分类不合法"),
    ADMIN_RESOURCE_VERSION_CONFLICT(1205, "数据已变化，请刷新后重试"),
    ADMIN_BANK_NO_RELEASED_QUESTION(1206, "题库没有可发布题目"),
    ADMIN_QUESTION_NOT_FOUND(1301, "题目不存在"),
    ADMIN_QUESTION_TYPE_INVALID(1302, "题型与题库不匹配"),
    ADMIN_QUESTION_OPTION_INVALID(1303, "选项或正确答案不合法"),
    ADMIN_QUESTION_STATE_INVALID(1304, "题目状态不允许当前操作"),
    // 变更：一题一库后不再存在“共享题目无权修改”的业务错误，1305 预留不复用。
    ADMIN_QUESTION_ORDER_INVALID(1306, "题目序号不合法"),
    ADMIN_QUESTION_TITLE_CONFLICT(1307, "同一题库中已存在相同题目"),
    ADMIN_IMPORT_FILE_INVALID(1310, "导入文件不合法"),
    ADMIN_IMPORT_ROW_INVALID(1311, "导入文件存在错误行"),
    ADMIN_IMPORT_TASK_INVALID(1312, "导入任务不存在、过期或状态错误"),
    ADMIN_USER_STATE_INVALID(1401, "用户不存在或状态不允许当前操作"),
    ADMIN_CONTENT_STATE_INVALID(1411, "社区内容不存在或状态不允许当前操作"),
    ADMIN_MEMBERSHIP_STATE_INVALID(1501, "会员状态不允许当前操作"),
    ADMIN_MEMBERSHIP_LEDGER_CONFLICT(1502, "会员台账已变化");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
