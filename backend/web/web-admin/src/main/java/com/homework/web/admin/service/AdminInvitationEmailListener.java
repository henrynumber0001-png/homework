package com.homework.web.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 在邀请事务成功提交后异步发送邮件。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInvitationEmailListener {
    //AdminInvitationEmailListener 自己并不知道事务是否成功。
    //真正判断事务成功或失败的是 Spring 的事务管理器。

    private final EmailInvitationSender emailInvitationSender;

    /*
        事务提交成功
            ↓
    Spring 触发 AFTER_COMMIT 监听器
            ↓
    @Async 拦截调用
            ↓
    把任务提交给 adminEmailExecutor
            ↓
    请求线程继续结束
            ↓
    邮件线程执行 sendInvitation()
     */
    @Async("adminEmailExecutor")
    //监听器只是通过这个注解告诉 Spring：请在当前事务成功提交以后，再调用这个监听方法。
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendInvitation(AdminInvitationCreatedEvent event) {
        try {
            emailInvitationSender.sendInvitation(
                    event.email(),
                    event.displayName(),
                    event.rawToken(),
                    event.expiresTime()
            );
        } catch (Exception exception) {
            // 异步线程不能再回滚已经提交的邀请；记录标识供告警和人工补发定位，禁止记录含 Token 的链接。
            log.error(
                    "管理员邀请邮件发送失败，invitationId={}, email={}",
                    event.invitationId(),
                    event.email(),
                    exception
            );
        }
    }
}

     /*  监听逻辑
            invite()
              ↓
            保存邀请
              ↓
            保存审计失败
              ↓
            数据库回滚
              ↓
            Spring 不调用监听器
              ↓
            try 和 catch 都不执行
         */
        /*
        数据库成功，邮件进入进入 try
               ↓
            执行邮件发送
               ↓
            是否抛出异常？
               ├─ 否 → 跳过 catch，方法结束
               └─ 是 → 停止 try 的剩余代码，进入 catch
         */
