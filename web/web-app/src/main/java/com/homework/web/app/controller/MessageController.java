package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.context.LoginUserHolder;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.vo.MessageUnreadSummaryVO;
import com.homework.web.app.vo.NotificationVO;
import com.homework.web.app.vo.PrivateMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 头像下拉菜单中“我的消息”的四个模块接口。 */
@RestController
@RequestMapping("/api/app/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/notifications")
    public Result<List<NotificationVO>> notifications(@RequestParam(required = false) Integer type,
                                                       @RequestParam(required = false) String tab,
                                                       @RequestParam(required = false) Integer pageNum,
                                                       @RequestParam(required = false) Integer pageSize) {
        return Result.success(messageService.listNotifications(
                LoginUserHolder.getUserId(), type, tab, pageNum, pageSize));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageService.countUnread(LoginUserHolder.getUserId()));
    }

    @GetMapping("/unread-summary")
    public Result<MessageUnreadSummaryVO> unreadSummary() {
        return Result.success(messageService.unreadSummary(LoginUserHolder.getUserId()));
    }

    @PutMapping("/notifications/{notificationId}/read")
    public Result<Void> markNotificationRead(@PathVariable Long notificationId) {
        messageService.markNotificationRead(LoginUserHolder.getUserId(), notificationId);
        return Result.success();
    }

    @PutMapping("/notifications/read-all")
    public Result<Void> markTabRead(@RequestParam String tab) {
        messageService.markTabRead(LoginUserHolder.getUserId(), tab);
        return Result.success();
    }

    @GetMapping("/private")
    public Result<List<PrivateMessageVO>> privateMessages(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(messageService.listPrivateMessages(
                LoginUserHolder.getUserId(), pageNum, pageSize));
    }

    @PostMapping("/private")
    public Result<Long> sendPrivateMessage(@RequestBody PrivateMessageCreateDTO dto) {
        return Result.success(messageService.sendPrivateMessage(LoginUserHolder.getUserId(), dto));
    }

    @PutMapping("/private/{messageId}/read")
    public Result<Void> markPrivateMessageRead(@PathVariable Long messageId) {
        messageService.markPrivateMessageRead(LoginUserHolder.getUserId(), messageId);
        return Result.success();
    }
}
