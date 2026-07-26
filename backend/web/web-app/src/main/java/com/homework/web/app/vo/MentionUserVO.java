package com.homework.web.app.vo;

import lombok.Data;

/** 返回 Post 或 Comment 中 @ 用户选择器的一项搜索结果。 */
@Data
public class MentionUserVO {
    /** 用户 ID，提交 @ 时使用。 */
    private Long userId;
    /** 唯一账号编号。 */
    private String accountNo;
    /** 用户展示名。 */
    private String displayName;
    /** 用户头像。 */
    private String avatar;
}
