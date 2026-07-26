package com.homework.web.app.service;

import com.homework.model.enums.UserAuthIdentityProvider;
import com.homework.web.app.dto.ThirdPartyUser;

public interface ThirdPartyAuthHandler {

    UserAuthIdentityProvider provider();

    ThirdPartyUser verifyAndGetUser(String authCode);
}
