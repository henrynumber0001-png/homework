package com.homework.web.app.service;

import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.web.app.dto.ThirdPartyUser;

public interface ThirdPartyAuthService {
    ThirdPartyUser verifyAndGetUser(UserAuthIdentityProvider provider, String authCode);
}
