package com.homework.web.app.service;

public interface MembershipAccessService {

    MembershipAccessSnapshot getAccess(Long userId);

    MembershipAccessSnapshot requireActiveMembership(Long userId);

    MembershipAccessSnapshot requirePremium(Long userId);
}
