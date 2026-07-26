package com.homework.web.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.SvipRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 生成会员双台账审计快照 JSON。 */
@Service
@RequiredArgsConstructor
public class MembershipSnapshotService {

    private final ObjectMapper objectMapper;

    public String create(BaseVipRecord baseVip, SvipRecord svip, boolean suspended) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("premiumExpireTime", baseVip == null ? null : baseVip.getExpireTime());
        snapshot.put("premiumVersion", baseVip == null ? null : baseVip.getVersion());
        snapshot.put("premiumPlusExpireTime", svip == null ? null : svip.getExpireTime());
        snapshot.put("premiumPlusVersion", svip == null ? null : svip.getVersion());
        snapshot.put("suspended", suspended);
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
    }
}
