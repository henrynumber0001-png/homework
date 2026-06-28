package com.homework.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.web.app.dto.EmailRegisterDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserAuthIdentityService extends IService<UserAuthIdentity> {
    String register(EmailRegisterDTO emailRegisterDTO, HttpServletRequest request);
}
