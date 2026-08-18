package com.homework.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/** 个人中心允许修改的图片种类。 */
@Getter
public enum UserImageType implements BaseEnum {

    AVATAR(1,"avatar",2L * 1024 * 1024),
    BANNER(2,"banner",5L * 1024 * 1024);


    private final Integer code;
    private final String name;
    private final Long maxSize;

    UserImageType(Integer code, String name,Long maxSize) {
        this.code = code;
        this.name = name;
        this.maxSize = maxSize;

    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserImageType of(Integer code){
        for(UserImageType type : values()){
            if(type.getCode().equals(code)){
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的图片类型");
    }
}

/*
这个values()不是你自己写的，这是 Java 编译器自动为每一个 enum 生成的静态方法。
public static UserImageType[] values() {
    return new UserImageType[]{
        AVATAR,
        BANNER
    };
}
 */

/*
前端 JSON
{
    "userImageType": 1
}
        ↓
HTTP Request Body
        ↓
Spring MVC找到updateImage()方法
        ↓
发现@RequestBody
        ↓
调用MappingJackson2HttpMessageConverter
        ↓
调用Jackson ObjectMapper -> objectMapper.readValue(json,UserImageUpdateDTO.class);
        ↓
发现目标对象：
UserImageUpdateDTO
        ↓
发现字段：
UserImageType userImageType
        ↓
需要把 JSON 1 转成 UserImageType
        ↓
检查 UserImageType 的反序列化规则
        ↓
发现 @JsonCreator
        ↓
调用：UserImageType.of(1)
        ↓
使用Java预编译的values()进行遍历
        ↓
[AVATAR(1), BANNER(2)]
        ↓
找到 AVATAR.getCode() == 1
        ↓
return UserImageType.AVATAR
        ↓
放入 dto.userImageType
        ↓
Controller 获得完整 DTO
 */
