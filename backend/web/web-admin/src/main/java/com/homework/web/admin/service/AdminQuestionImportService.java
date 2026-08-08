package com.homework.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.QuestionBank;
import com.homework.model.entity.QuestionImportError;
import com.homework.model.entity.QuestionImportTask;
import com.homework.model.enums.AdminRole;
import com.homework.model.enums.GroupType;
import com.homework.model.enums.QuestionImportStatus;
import com.homework.web.admin.auth.AdminAccessService;
import com.homework.web.admin.config.AdminFeatureProperties;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.dto.QuestionImportCommitDTO;
import com.homework.web.admin.mapper.QuestionBankMapper;
import com.homework.web.admin.mapper.QuestionImportErrorMapper;
import com.homework.web.admin.mapper.QuestionImportTaskMapper;
import com.homework.web.admin.vo.QuestionImportErrorVO;
import com.homework.web.admin.vo.QuestionImportTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 后台题目 Excel 模板、预检任务和确认导入。 */
@Service
@RequiredArgsConstructor
public class AdminQuestionImportService {

    private static final long MAX_IMPORT_SIZE = 10L * 1024 * 1024;

    private final QuestionBankMapper bankMapper;
    private final QuestionImportTaskMapper taskMapper;
    private final QuestionImportErrorMapper errorMapper;
    private final AdminAccessService accessService;
    private final AdminFeatureProperties featureProperties;
    private final QuestionImportWorkbookService workbookService;
    private final QuestionImportCommitService commitService;
    private final AdminAuditService auditService;

    public byte[] createTemplate(Long bankId) {
        accessService.requireBank(bankId);

        GroupType groupType = bankMapper.selectGroupType(bankId);
        if (groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        return workbookService.createTemplate(groupType);
    }

    public QuestionImportTaskVO createTask(Long bankId, MultipartFile file) {
        accessService.requireBank(bankId);
        QuestionBank bank = bankMapper.selectById(bankId);
        GroupType groupType = bank == null ? null : bankMapper.selectGroupType(bankId);
        String fileName = file == null ? null : file.getOriginalFilename();
        if (bank == null || groupType == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_BANK_NOT_FOUND);
        }
        if (file == null || file.isEmpty() || file.getSize() > MAX_IMPORT_SIZE
                || fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID);
        }

        try {
            byte[] bytes = file.getBytes();
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            Long duplicateCount = taskMapper.selectCount(new LambdaQueryWrapper<QuestionImportTask>()
                    .eq(QuestionImportTask::getAdminId, AdminContext.getAdminId())
                    .eq(QuestionImportTask::getBankId, bankId)
                    .eq(QuestionImportTask::getFileSha256, sha256)
                    .in(QuestionImportTask::getStatus, QuestionImportStatus.READY, QuestionImportStatus.SUCCEEDED));
            if (duplicateCount > 0) {
                throw new HomeworkException(ResultCodeEnum.REPEAT_SUBMIT);
            }
            Path directory = Path.of(featureProperties.getImportConfig().getTempDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String taskNo = "QIMPORT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            Path filePath = directory.resolve(taskNo + ".xlsx");
            Files.write(filePath, bytes);

            QuestionImportTask task = new QuestionImportTask();
            task.setTaskNo(taskNo);
            task.setBankId(bankId);
            task.setAdminId(AdminContext.getAdminId());
            task.setFileName(fileName);
            task.setFileSha256(sha256);
            task.setFilePath(filePath.toString());
            task.setStatus(QuestionImportStatus.VALIDATING);
            task.setExpiresTime(LocalDateTime.now().plusHours(24));
            taskMapper.insert(task);

            //调用 QuestionImportWorkbookService 的 parse 方法的返回值
            QuestionImportWorkbookResult workbookResult;
            try (InputStream input = Files.newInputStream(filePath)) {
                workbookResult = workbookService.parse(input, groupType);
            }
            for (QuestionImportErrorVO errorVO : workbookResult.getErrors()) {
                QuestionImportError error = new QuestionImportError();
                error.setTaskId(task.getId());
                error.setRowNumber(errorVO.getRowNumber());
                error.setFieldName(errorVO.getFieldName());
                error.setErrorMessage(errorVO.getErrorMessage());
                errorMapper.insert(error);
            }
            task.setTotalRows(workbookResult.getTotalRows());
            task.setValidRows(workbookResult.getQuestions().size());
            task.setErrorRows(workbookResult.getErrors().size());
            task.setStatus(workbookResult.getErrors().isEmpty()
                    ? QuestionImportStatus.READY
                    : QuestionImportStatus.INVALID);
            taskMapper.updateById(task);
            auditService.record("QUESTION", "IMPORT_VALIDATE", "QUESTION_IMPORT", taskNo, fileName, null, task);
            return toVO(task);
        } catch (HomeworkException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_FILE_INVALID, exception);
        }
    }

    public QuestionImportTaskVO getTask(String taskNo) {
        QuestionImportTask task = taskMapper.selectOne(new LambdaQueryWrapper<QuestionImportTask>()
                .eq(QuestionImportTask::getTaskNo, taskNo));
        requireTaskAccess(task);
        if (task.getExpiresTime().isBefore(LocalDateTime.now())
                && task.getStatus() != QuestionImportStatus.SUCCEEDED) {
            task.setStatus(QuestionImportStatus.EXPIRED);
            taskMapper.updateById(task);
        }
        return toVO(task);
    }

    public List<QuestionImportErrorVO> listErrors(String taskNo) {
        QuestionImportTask task = taskMapper.selectOne(new LambdaQueryWrapper<QuestionImportTask>()
                .eq(QuestionImportTask::getTaskNo, taskNo));
        requireTaskAccess(task);
        return errorMapper.selectList(new LambdaQueryWrapper<QuestionImportError>()
                        .eq(QuestionImportError::getTaskId, task.getId())
                        .orderByAsc(QuestionImportError::getRowNumber)
                        .orderByAsc(QuestionImportError::getId))
                .stream()
                .map(error -> {
                    QuestionImportErrorVO vo = new QuestionImportErrorVO();
                    vo.setRowNumber(error.getRowNumber());
                    vo.setFieldName(error.getFieldName());
                    vo.setErrorMessage(error.getErrorMessage());
                    return vo;
                })
                .toList();
    }

    public QuestionImportTaskVO commit(String taskNo, QuestionImportCommitDTO dto) {
        QuestionImportTask task = taskMapper.selectOne(new LambdaQueryWrapper<QuestionImportTask>()
                .eq(QuestionImportTask::getTaskNo, taskNo));
        requireTaskAccess(task);
        accessService.requireBank(task.getBankId());
        if (task.getStatus() != QuestionImportStatus.READY
                || task.getExpiresTime().isBefore(LocalDateTime.now())
                || !task.getTotalRows().equals(dto.getConfirmTotalRows())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_TASK_INVALID);
        }
        GroupType groupType = bankMapper.selectGroupType(task.getBankId());
        QuestionImportWorkbookResult workbookResult;
        try (InputStream input = Files.newInputStream(Path.of(task.getFilePath()))) {
            workbookResult = workbookService.parse(input, groupType);
        } catch (Exception exception) {
            task.setStatus(QuestionImportStatus.FAILED);
            task.setFailureReason("无法重新读取预检文件");
            task.setFinishedTime(LocalDateTime.now());
            taskMapper.updateById(task);
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_TASK_INVALID, exception);
        }
        if (!workbookResult.getErrors().isEmpty()
                || workbookResult.getTotalRows() != task.getTotalRows()) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_ROW_INVALID);
        }

        task.setStatus(QuestionImportStatus.IMPORTING);
        taskMapper.updateById(task);
        try {
            int imported = commitService.importAll(task.getBankId(), workbookResult.getQuestions());
            task.setStatus(QuestionImportStatus.SUCCEEDED);
            task.setImportedRows(imported);
            task.setFinishedTime(LocalDateTime.now());
            taskMapper.updateById(task);
            auditService.record("QUESTION", "IMPORT_COMMIT", "QUESTION_IMPORT", taskNo, "确认导入", null, task);
            return toVO(task);
        } catch (RuntimeException exception) {
            task.setStatus(QuestionImportStatus.FAILED);
            task.setFailureReason("导入事务执行失败");
            task.setFinishedTime(LocalDateTime.now());
            taskMapper.updateById(task);
            throw exception;
        }
    }

    public void requireTaskAccess(QuestionImportTask task) {
        if (task == null) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_IMPORT_TASK_INVALID);
        }
        if (AdminContext.get().getRole() != AdminRole.SUPER_ADMIN
                && !task.getAdminId().equals(AdminContext.getAdminId())) {
            throw new HomeworkException(ResultCodeEnum.ADMIN_PERMISSION_DENIED);
        }
    }

    public QuestionImportTaskVO toVO(QuestionImportTask task) {
        QuestionImportTaskVO vo = new QuestionImportTaskVO();
        vo.setTaskId(task.getTaskNo());
        vo.setBankId(task.getBankId());
        vo.setFileName(task.getFileName());
        vo.setStatus(task.getStatus());
        vo.setTotalRows(task.getTotalRows());
        vo.setValidRows(task.getValidRows());
        vo.setErrorRows(task.getErrorRows());
        vo.setImportedRows(task.getImportedRows());
        vo.setFailureReason(task.getFailureReason());
        vo.setExpiresTime(task.getExpiresTime());
        vo.setFinishedTime(task.getFinishedTime());
        return vo;
    }
}
