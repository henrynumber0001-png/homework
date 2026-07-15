package com.homework.model.entity;

import lombok.Data;

@Data
public class UserFollowing {

    private Long userId;

    private Long followingId;
}
