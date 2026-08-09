package com.homework.schedule.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatCleanupTask {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 每 10 分钟执行一次。
     *
     * 作用：
     * 1. 把长时间没有活动的 ACTIVE 会话改成 CLOSED。
     * 2. 物理清理很久以前已经逻辑删除的 AI 消息。
     *
     * 注意：
     * 不删除 ai_chat_session，因为第一版方案里 session 是 userId + bankId 的长期容器。
     */
    @Scheduled(cron = "0 */10 * * * ?")
    @Transactional
    public void cleanupAiChat() {
        closeInactiveSessions();
        deleteOldLogicDeletedMessages();
    }

    /**
     * 用户突然关闭浏览器、断网、直接离开页面时，前端可能来不及调用 closeAiChat。
     * 这个任务兜底：超过 30 分钟没有 updated_time 变化的 ACTIVE session，自动改成 CLOSED。
     */
    private void closeInactiveSessions() {
        String sql = """
                UPDATE ai_chat_session acs
                SET acs.status = 2
                WHERE acs.status = 1
                  AND acs.is_deleted = 0
                  AND (
                    SELECT MAX(acm.updated_time)
                    FROM ai_chat_message acm
                    WHERE acm.session_id = acs.id
                    AND acm.is_deleted = 0
                    ) < NOW() - INTERVAL 30 MINUTE
                """;

        int count = jdbcTemplate.update(sql);
        log.info("Closed inactive AI chat sessions: {}", count);
    }

    /**
     * startAiChat 重新进入题库时，会逻辑删除旧 messages。
     * 这些 is_deleted = 1 的旧消息业务上已经不可见。
     * 这里物理删除 30 天以前的逻辑删除消息，避免表无限增长。
     */
    private void deleteOldLogicDeletedMessages() {
        String sql = """
                DELETE FROM ai_chat_message
                WHERE is_deleted = 1
                  AND updated_time < NOW() - INTERVAL 30 DAY
                """;

        int count = jdbcTemplate.update(sql);
        log.info("Physically deleted old logic-deleted AI chat messages: {}", count);
    }
}
