package com.homework.web.app.controller;

import com.homework.common.result.Result;
import com.homework.web.app.dto.PrivateMessageCreateDTO;
import com.homework.web.app.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/app/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/notifications")
    public Result<List<Map<String, Object>>> notifications(@RequestParam Long userId,
                                                           @RequestParam(required = false) Integer type,
                                                           @RequestParam(required = false) String tab) {
        return Result.success(messageService.listNotifications(userId, type, tab));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestParam Long userId) {
        return Result.success(messageService.countUnread(userId));
    }

    @GetMapping("/private")
    public Result<List<Map<String, Object>>> privateMessages(@RequestParam Long userId) {
        return Result.success(messageService.listPrivateMessages(userId));
    }

    @PostMapping("/private")
    public Result<Long> sendPrivateMessage(@RequestBody PrivateMessageCreateDTO dto) {
        return Result.success(messageService.sendPrivateMessage(dto));
    }
}
