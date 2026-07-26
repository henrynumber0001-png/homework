package com.homework.web.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.web.app.dto.EmailLoginDTO;
import com.homework.web.app.dto.EmailRegisterDTO;
import com.homework.web.app.dto.ThirdPartyLoginDTO;
import com.homework.web.app.dto.ThirdPartyRegisterDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface UserAuthIdentityService extends IService<UserAuthIdentity> {
    String registerByEmail(EmailRegisterDTO emailRegisterDTO, HttpServletRequest request);

    String registerByOAuth(ThirdPartyRegisterDTO thirdPartyRegisterDTO,HttpServletRequest request);

    String loginByEmail(EmailLoginDTO emailLoginDTO,HttpServletRequest request);

    String loginByOAuth(ThirdPartyLoginDTO thirdPartyLoginDTO,HttpServletRequest request);
}
