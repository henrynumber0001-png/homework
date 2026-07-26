package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.common.result.PageResult;
import com.homework.model.entity.AdminOperationLog;
import com.homework.model.enums.AdminRole;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.AdminOperationLogMapper;
import com.homework.web.admin.vo.AuditLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** 分页查询当前管理员可见的后台操作日志。 */
@Service
@RequiredArgsConstructor
public class AdminAuditQueryService {

    private final AdminOperationLogMapper logMapper;

    public PageResult<AuditLogVO> list(
            Long operatorAdminId,
            String module,
            String action,
            String targetId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer pageNum,
            Integer pageSize
    ) {
        int normalizedPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int normalizedSize = pageSize == null ? 20 : Math.min(Math.max(pageSize, 1), 100);
        LambdaQueryWrapper<AdminOperationLog> query = new LambdaQueryWrapper<>();
        if (AdminContext.get().getRole() == AdminRole.SUPER_ADMIN) {
            query.eq(operatorAdminId != null, AdminOperationLog::getOperatorAdminId, operatorAdminId);
        } else {
            query.eq(AdminOperationLog::getOperatorAdminId, AdminContext.getAdminId());
        }
        query.eq(module != null && !module.isBlank(), AdminOperationLog::getModule, module)
                .eq(action != null && !action.isBlank(), AdminOperationLog::getAction, action)
                .eq(targetId != null && !targetId.isBlank(), AdminOperationLog::getTargetId, targetId)
                .ge(startTime != null, AdminOperationLog::getCreatedTime, startTime)
                .le(endTime != null, AdminOperationLog::getCreatedTime, endTime)
                .orderByDesc(AdminOperationLog::getCreatedTime)
                .orderByDesc(AdminOperationLog::getId);
        Page<AdminOperationLog> page = logMapper.selectPage(
                new Page<>(normalizedPage, normalizedSize),
                query
        );
        PageResult<AuditLogVO> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(log -> {
            AuditLogVO vo = new AuditLogVO();
            vo.setRequestId(log.getRequestId());
            vo.setOperatorAdminId(log.getOperatorAdminId());
            vo.setOperatorName(log.getOperatorName());
            vo.setModule(log.getModule());
            vo.setAction(log.getAction());
            vo.setTargetType(log.getTargetType());
            vo.setTargetId(log.getTargetId());
            vo.setReason(log.getReason());
            vo.setBeforeSnapshot(log.getBeforeSnapshot());
            vo.setAfterSnapshot(log.getAfterSnapshot());
            vo.setSuccess(log.getSuccess());
            vo.setFailureMessage(log.getFailureMessage());
            vo.setIp(log.getIp());
            vo.setUserAgent(log.getUserAgent());
            vo.setCreatedTime(log.getCreatedTime());
            return vo;
        }).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }
}
