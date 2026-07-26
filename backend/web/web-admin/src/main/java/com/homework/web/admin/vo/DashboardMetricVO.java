package com.homework.web.admin.vo;

import lombok.Data;

/** 看板单项的当日和累计指标。 */
@Data
public class DashboardMetricVO {

    /** 指定日期值。 */
    private Long daily;

    /** 历史累计值。 */
    private Long total;
}
