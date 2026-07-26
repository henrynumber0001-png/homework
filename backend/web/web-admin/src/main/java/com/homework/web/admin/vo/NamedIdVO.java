package com.homework.web.admin.vo;

import lombok.Data;

/** 通用 ID 与名称引用。 */
@Data
public class NamedIdVO {

    /** 资源 ID。 */
    private Long id;

    /** 资源名称。 */
    private String name;
}
