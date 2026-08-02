package com.homework.web.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.model.entity.AdminAccount;
import com.homework.model.entity.AdminOperationLog;
import com.homework.web.admin.config.AdminRequestIdFilter;
import com.homework.web.admin.context.AdminContext;
import com.homework.web.admin.mapper.AdminOperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 统一记录管理后台的操作审计日志。
 *
 * <p>普通运行日志主要用于排查程序问题；审计日志则用于回答：
 * 谁在什么时间、通过哪个请求、对哪个业务对象做了什么操作，
 * 操作前后数据是什么，以及操作最终是否成功。</p>
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    /** 将审计日志写入 admin_operation_log 表。 */
    private final AdminOperationLogMapper logMapper;

    /** 把操作前后的 Java 对象序列化成 JSON 快照。 */
    private final ObjectMapper objectMapper;

    /**
     * 记录一次成功的后台业务写操作。
     *
     * @param module 业务模块，例如 BANK、QUESTION、USER
     * @param action 操作动作，例如 CREATE、UPDATE、PUBLISH
     * @param targetType 被操作的资源类型，例如 QUESTION_BANK
     * @param targetId 被操作资源的主键或业务编号
     * @param reason 管理员填写的操作原因
     * @param before 修改前的数据；创建操作通常为 null
     * @param after 修改后的数据；删除操作也可以保存删除后的状态
     */
    public void record(
            String module,
            String action,
            String targetType,
            Object targetId,
            String reason,
            Object before,
            Object after
    ) {
        // 从当前线程的管理员上下文中取得本次操作人；系统内部任务执行时可能为空。
        AdminAccount admin = AdminContext.get();

        // 取得当前 HTTP 请求，以便记录 requestId、IP 和 User-Agent。
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 非 HTTP 场景下 attributes 可能为空，因此这里需要允许 request 为 null。
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        // 创建一条成功操作的审计日志实体。
        AdminOperationLog log = new AdminOperationLog();

        // HTTP 请求使用过滤器生成的 requestId；内部调用统一标记为 internal。
        log.setRequestId(request == null ? "internal" : String.valueOf(request.getAttribute(AdminRequestIdFilter.ATTRIBUTE)));

        // 记录管理员 ID；没有管理员上下文时使用 0 表示系统操作。
        log.setOperatorAdminId(admin == null ? 0L : admin.getId());

        // 保存管理员名称快照，避免管理员以后改名导致历史记录无法辨认。
        log.setOperatorName(admin == null ? "system" : admin.getDisplayName());

        // 保存业务模块、操作动作和被操作资源信息。
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);

        // 不同业务的主键类型可能不同，因此统一转换成字符串保存。
        log.setTargetId(targetId == null ? null : String.valueOf(targetId));

        // 保存管理员执行该操作时填写的原因。
        log.setReason(reason);

        // 把修改前和修改后的对象序列化成 JSON，便于后续对比变更内容。
        log.setBeforeSnapshot(toJson(before));
        log.setAfterSnapshot(toJson(after));

        // record() 只用于业务成功后的记录，因此 success 固定为 true。
        log.setSuccess(true);

        // HTTP 请求存在时记录来源 IP 和浏览器/客户端信息。
        log.setIp(request == null ? null : request.getRemoteAddr());
        log.setUserAgent(request == null ? null : request.getHeader("User-Agent"));

        // 将完整审计记录持久化到数据库。
        logMapper.insert(log);
    }

    /**
     * 记录一次失败的后台写请求。
     *
     * <p>该方法由全局异常处理器调用。失败可能发生在进入具体业务方法之前，
     * 因而这里只记录请求级信息，而不强行推断具体业务对象。</p>
     *
     * @param failureMessage 异常或业务失败信息
     */
    public void recordFailure(String failureMessage) {
        // 尝试取得当前 HTTP 请求上下文。
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 请求上下文不存在时无法记录请求信息。
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();

        // GET 是只读请求，不属于后台写操作；内部调用没有 request，也直接跳过。
        if (request == null || "GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        // 参数校验失败时管理员上下文可能尚未建立，因此 admin 允许为空。
        AdminAccount admin = AdminContext.get();

        // 创建一条请求失败的审计日志。
        AdminOperationLog log = new AdminOperationLog();

        // 使用请求过滤器生成的 requestId，把接口响应、运行日志和审计日志关联起来。
        log.setRequestId(String.valueOf(request.getAttribute(AdminRequestIdFilter.ATTRIBUTE)));

        // 已登录时记录管理员；无法识别操作人时使用 anonymous。
        log.setOperatorAdminId(admin == null ? 0L : admin.getId());
        log.setOperatorName(admin == null ? "anonymous" : admin.getDisplayName());

        // 失败可能发生在 Controller 参数校验阶段，所以统一按 HTTP 请求记录。
        log.setModule("REQUEST");
        log.setAction(request.getMethod());
        log.setTargetType("HTTP_ENDPOINT");
        log.setTargetId(request.getRequestURI());

        // 标记本次操作失败。
        log.setSuccess(false);

        // 数据库字段最多保存 500 个字符；空消息使用 unknown，过长消息安全截断。
        log.setFailureMessage(failureMessage == null ? "unknown" : failureMessage.substring(0, Math.min(failureMessage.length(), 500)));

        // 保存失败请求的来源信息。
        log.setIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));

        // 将失败审计记录持久化到数据库。
        logMapper.insert(log);
    }

    /** 将任意对象转换成审计快照 JSON，并避免序列化失败影响原业务。 */
    private String toJson(Object value) {
        // null 表示该操作没有修改前或修改后的对象，例如创建操作的 before。
        if (value == null) {
            return null;
        }
        try {
            // 使用项目统一配置的 ObjectMapper 生成 JSON。
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            // 快照无法序列化时保存占位摘要，不再向外抛出序列化异常。
            return "{\"summary\":\"unavailable\"}";
        }
    }
}
