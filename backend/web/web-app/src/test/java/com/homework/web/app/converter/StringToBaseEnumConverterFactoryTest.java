package com.homework.web.app.converter;

import com.homework.model.enums.UserImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringToBaseEnumConverterFactoryTest {

    @Test
    void convertsNumericPathValueToUserImageType() {
        StringToBaseEnumConverterFactory factory = new StringToBaseEnumConverterFactory();

        UserImageType result = factory.getConverter(UserImageType.class).convert("1");

        assertEquals(UserImageType.AVATAR, result);
    }
}
