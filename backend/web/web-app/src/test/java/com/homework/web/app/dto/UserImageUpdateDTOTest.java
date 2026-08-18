package com.homework.web.app.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.homework.model.enums.UserImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserImageUpdateDTOTest {

    @Test
    void convertsNumericJsonValueToUserImageType() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule());

        UserImageUpdateDTO dto = objectMapper.readValue(
                "{\"imageObjectKey\":\"temp/user/image/banner/example.png\",\"userImageType\":2}",
                UserImageUpdateDTO.class
        );

        assertEquals(UserImageType.BANNER, dto.getUserImageType());
    }
}
