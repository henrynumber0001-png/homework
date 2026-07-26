package com.homework.web.admin.vo;

import lombok.Data;

import java.util.List;

/** 后台会员详情。 */
@Data
public class MembershipDetailVO extends MembershipRowVO {

    /** 最近二十条管理员会员变更。 */
    private List<MembershipChangeVO> recentChanges;
}
