package com.homework.web.app.controller;

import com.homework.common.result.PageResult;
import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.service.MessageService;
import com.homework.web.app.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/app/messages")
@RequiredArgsConstructor
public class MessageController {
    private final MessageService messageService;

    @GetMapping("/unread-summary")
    public Result<MessageUnreadSummaryVO> unreadSummary() {
        return Result.success(messageService.unreadSummary(LoginUserHolder.getUserId()));
    }

    /**
     * 打开评论、互动或系统通知 Tab：
     * 先把该 Tab 当前所有未读通知设为已读，再返回最近一次批量已读的通知。
     */
    @PutMapping("/notifications/open-tab")
    public Result<PageResult<NotificationVO>> openNotificationTab(
            @RequestParam String tab,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(messageService.loadNotificationTab(
                LoginUserHolder.getUserId(), tab, pageNum, pageSize, false));
    }

    /** 用户点击“查看历史信息”时调用，不改变任何通知的已读状态。 */
    @GetMapping("/notifications/history")
    public Result<PageResult<NotificationVO>> notificationHistory(
            @RequestParam String tab,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(messageService.loadNotificationTab(
                LoginUserHolder.getUserId(), tab, pageNum, pageSize, true));
    }

    @GetMapping("/chatboxes")
    public Result<PageResult<PrivateChatboxVO>> chatboxes(@RequestParam(required = false) Integer pageNum,
                                                         @RequestParam(required = false) Integer pageSize) {
        return Result.success(messageService.listChatboxes(LoginUserHolder.getUserId(), pageNum, pageSize));
    }

    @GetMapping("/chatboxes/with/{userId}")
    public Result<PrivateChatboxVO> chatboxWith(@PathVariable Long userId) {
        return Result.success(messageService.findChatboxWith(LoginUserHolder.getUserId(), userId));
    }

    @GetMapping("/chatboxes/{id}/messages")
    public Result<List<PrivateMessageVO>> messages(@PathVariable Long id,
                                                   @RequestParam(required = false) Long beforeId,
                                                   @RequestParam(required = false) Long afterId,
                                                   @RequestParam(required = false) Integer limit) {
        return Result.success(messageService.listMessages(LoginUserHolder.getUserId(), id, beforeId, afterId, limit));
    }

    @PostMapping("/private")
    public Result<PrivateMessageVO> send(@RequestBody PrivateMessageCreateDTO dto) {
        return Result.success(messageService.sendPrivateMessage(LoginUserHolder.getUserId(), dto));
    }

    /** 点击一条私信时，只把这一条私信标记为已读。 */
    @PutMapping("/private/{messageId}/read")
    public Result<Void> readPrivateMessage(@PathVariable Long messageId) {
        messageService.markPrivateMessageRead(LoginUserHolder.getUserId(), messageId);
        return Result.success();
    }
}
