package com.homework.web.app.vo;

import lombok.Data;

@Data
public class CategoryModuleVO  {

    private Long id;

    /*
    把 private String groupId 删掉了
    目的是为了不让前端混淆
    因为当前业务模式下，groupId就是从用户侧获取
    然后 clickedGroupId = currentGroupId
     */

    private String moduleName;

    private Integer sortOrder;

}
