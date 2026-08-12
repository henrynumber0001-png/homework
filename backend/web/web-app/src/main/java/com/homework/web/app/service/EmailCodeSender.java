package com.homework.web.app.service;

public interface EmailCodeSender {

    void sendCode(String email, String code);
}
