package com.homework.web.app.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.homework.model.enums.HitActionType;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 将 JSON 请求体中的整数转换成 HitActionType。
 *
 * 例如：
 * 1 -> LIKE
 * 2 -> FAVORITE
 * 3 -> REPOST
 */
@Component
public class HitActionTypeJsonDeserializer extends JsonDeserializer<HitActionType> {

    @Override
    public HitActionType deserialize(JsonParser parser, DeserializationContext context) throws IOException {

        // JSON 中明确传 null 时返回 null，
        // 后续再由业务校验判断 actionType 是否必填。
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        // 读取 JSON 中的整数。
        int value = parser.getIntValue();

        for (HitActionType type : HitActionType.values()) {

            if (type.getValue().equals(value)) {
                return type;
            }
        }

        // 告诉 Jackson 当前数字无法转换成目标枚举。
        return (HitActionType) context.handleWeirdNumberValue(HitActionType.class, value, "未知的 Hit 互动类型，只允许 1、2、3");
    }
}
/*
JSON 数字 1
    ↓
HitActionTypeJsonDeserializer
    ↓
HitActionType.LIKE
    ↓
保存到 dto.actionType
 */
