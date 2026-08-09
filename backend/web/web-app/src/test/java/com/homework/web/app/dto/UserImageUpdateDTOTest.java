package com.homework.web.app.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.enums.UserImageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserImageUpdateDTOTest {

    @Test
    void convertsNumericJsonValueToUserImageType() throws Exception {
        UserImageUpdateDTO dto = new ObjectMapper().readValue(
                "{\"imageObjectKey\":\"temp/user/image/avatar/example.png\",\"userImageType\":1}",
                UserImageUpdateDTO.class
        );

        assertEquals(UserImageType.AVATAR, dto.getUserImageType());
    }
}
