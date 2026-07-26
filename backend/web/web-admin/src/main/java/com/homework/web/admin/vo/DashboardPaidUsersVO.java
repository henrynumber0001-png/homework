package com.homework.web.admin.vo;

import lombok.Data;

/** 看板 Premium 与 Premium Plus 付费人数。 */
@Data
public class DashboardPaidUsersVO {

    /** 指定日期 Premium 付费人数。 */
    private Long premiumDaily;

    /** Premium 历史累计付费人数。 */
    private Long premiumTotal;

    /** 指定日期 Premium Plus 付费人数。 */
    private Long premiumPlusDaily;

    /** Premium Plus 历史累计付费人数。 */
    private Long premiumPlusTotal;
}
