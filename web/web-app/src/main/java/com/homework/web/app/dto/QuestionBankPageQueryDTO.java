package com.homework.web.app.dto;

import com.homework.model.enums.SortType;
import lombok.Data;

@Data
public class QuestionBankPageQueryDTO {
    /** 第一次进入面试题库/认证题库页面时传入。 */
    private Long groupId;

    /** 点击顶部 module 时传入；后端会自动切换该 module 下的 sub_module 和题库列表。 */
    private Long moduleId;

    /** 点击左侧 sub_module 时传入；后端只切换题库列表。 */
    private Long subModuleId;

    /** 页面右侧题库列表排序：hot 表示热度，new/latest 表示最新。 */
    private SortType sort;

    /** 题库列表当前页码。 */
    private Integer pageNum = 1;

    /** 题库列表每页数量。 */
    private Integer pageSize = 20;
}
