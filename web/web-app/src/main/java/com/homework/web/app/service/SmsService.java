package com.homework.web.app.service;

public interface SmsService {

    void sendVerifyCode(String phone, String code);
}
