package com.homework.web.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.admin.config.TencentSesProperties;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.Template;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@AllArgsConstructor
public class TencentSesAdminInvitationEmailSender implements EmailInvitationSender{
    private final SesClient sesClient;
    private final TencentSesProperties properties;
    private final ObjectMapper objectMapper;
    @Override
    public void sendInvitation(String email, String displayName, String invitationUrl, LocalDateTime expiresTime) {
        try {
            Template template = new Template();
            template.setTemplateID(properties.getTemplateId());
            //从 JSON 语法看，值可以是 int、long、boolean 等类型；但对于腾讯云 SES，建议统一转换成字符串。
            template.setTemplateData(objectMapper.writeValueAsString(Map.of(
                    "displayName", displayName,
                    "invitationUrl", invitationUrl,
                    "expiresTime", expiresTime.toString()
            )));

            SendEmailRequest request = new SendEmailRequest();
            request.setFromEmailAddress(properties.getFromEmail());
            request.setDestination(new String[]{email});
            request.setSubject("Homework 管理员邀请");
            request.setTemplate(template);
            request.setTriggerType(1L);

            sesClient.SendEmail(request);
        } catch (Exception exception) {
            throw new HomeworkException(ResultCodeEnum.SERVICE_ERROR, exception);
        }
    }
}
