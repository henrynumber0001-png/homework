package com.homework.web.admin.converter;

import com.homework.model.enums.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.stereotype.Component;

/** 将查询参数和路径参数中的数字转换为统一的业务枚举。 */
@Component
public class StringToBaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return source -> {
            Integer value = Integer.valueOf(source);
            for (T enumConstant : targetType.getEnumConstants()) {
                if (enumConstant.getValue().equals(value)) {
                    return enumConstant;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown " + targetType.getSimpleName() + " value: " + source
            );
        };
    }
}
