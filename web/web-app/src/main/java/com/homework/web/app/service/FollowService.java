package com.homework.web.app.service;

public interface FollowService {
    boolean follow(Long currentUserId, Long targetUserId, Boolean active);
}
