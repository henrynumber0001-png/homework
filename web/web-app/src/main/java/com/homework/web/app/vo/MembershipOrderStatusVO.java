package com.homework.web.app.vo;

import com.homework.model.enums.MembershipOrderStatus;
import lombok.Data;

@Data
public class MembershipOrderStatusVO {

    private String orderNo;

    private MembershipOrderStatus orderStatus;
}
