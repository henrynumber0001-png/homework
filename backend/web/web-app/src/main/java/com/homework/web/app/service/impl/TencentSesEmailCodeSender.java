package com.homework.web.app.service.impl;




import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.web.app.config.TencentSesProperties;
import com.homework.web.app.service.EmailCodeSender;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ses.v20201002.SesClient;
import com.tencentcloudapi.ses.v20201002.models.SendEmailRequest;
import com.tencentcloudapi.ses.v20201002.models.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@RequiredArgsConstructor
public class TencentSesEmailCodeSender implements EmailCodeSender {
    private final SesClient sesClient;

    private final TencentSesProperties tencentSesProperties;

    private final ObjectMapper objectMapper;

    @Override
    public void sendCode(String email, String code) {
        try {
            Template template = new Template();
            template.setTemplateID(tencentSesProperties.getTemplateId());
            template.setTemplateData(buildTemplateData(code));

            SendEmailRequest request = new SendEmailRequest();
            request.setFromEmailAddress(tencentSesProperties.getFromEmail());
            request.setDestination(new String[]{email});
            request.setSubject("HomeWork 邮箱验证码"); //用户收到的邮件标题
            request.setTemplate(template);

            // 1 表示验证码、通知等触发类邮件
            request.setTriggerType(1L);

            sesClient.SendEmail(request);
        } catch (TencentCloudSDKException | JsonProcessingException exception) {
            throw new HomeworkException(ResultCodeEnum.EMAIL_CODE_SEND_FAILED, exception);
        }

    }
    private String buildTemplateData(String code) throws JsonProcessingException {
        return objectMapper.writeValueAsString(Map.of("code", code, "minutes", "1"));
    }
}
