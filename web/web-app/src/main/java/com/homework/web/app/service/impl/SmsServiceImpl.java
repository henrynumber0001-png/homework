package com.homework.web.app.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.homework.common.sms.AliyunSMSProperties;
import com.homework.web.app.service.SmsService;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {

    private final Client client;
    private final AliyunSMSProperties properties;

    public SmsServiceImpl(Client client, AliyunSMSProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public void sendVerifyCode(String phone, String code) {
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumbers(phone);
        request.setTemplateCode(properties.getTemplateCode());
        request.setSignName(properties.getSignName());
        request.setTemplateParam("{\"code\":\"" + code + "\"}");

        try {
            client.sendSms(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
