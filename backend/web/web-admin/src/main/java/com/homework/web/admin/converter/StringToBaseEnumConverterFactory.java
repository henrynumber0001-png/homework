package com.homework.web.admin.converter;

import com.homework.model.enums.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

/** 将查询参数和路径参数中的数字转换为统一的业务枚举。 */
@Component
//把前端传来的字符串数字，例如 "1"，自动转换成某个实现了 BaseEnum 的枚举值。
//ConverterFactory 可以根据目标类型，动态生产不同 Converter，只要这些枚举都实现了 BaseEnum
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @Override
    // T 必须是 BaseEnum 的子类型，Java 泛型里的 extends，表示”上界（upper bound）”，不是字面意义上的”继承类”。继承和实现，都会形成 子类型关系
    // 本方法的返回值类型是一个转换器 Converter<String, T>
    // Class<T> targetType 是 目标枚举类型的 .class 文件，比如：UserImageType.class
    // Class<T> targetType 是 Spring MVC 在通过 Handler Mapping 匹配到正确的 Controller 方法之后，根据该方法中需要 类型转换 的参数提取出来的。
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) { //Spring MVC 在找到 handler方法之后，根据方法的参数，自己推断出来的

        // Lambda表达式，完整类型实际上是：Converter<String, T>
        /*
        new Converter<String, T>() {
        @Override
        public T convert(String source) { //source 就是前端传来的字符串。
        ...
            }};
         */
        return source -> { //source 就是前端传来的字符串。
            Integer code = Integer.valueOf(source); //解析为 Integer
            for (T enumConstant : targetType.getEnumConstants()) { //getEnumConstants() 获取这个枚举类型定义的所有枚举常量。
                //因为 T extends BaseEnum，所以代码知道：枚举常量 enumConstant 一定拥有 getCode() 方法。
                //如果匹配到了，返回这个枚举常量。比如 source = "1"，那么就返回 UserImageType.AVATAR
                if (enumConstant.getCode().equals(code)) {
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown " + targetType.getSimpleName() + " code: " + source
            );
        };
    }
}
