package com.homework.web.app.vo;

import lombok.Data;

@Data
public class FollowerVO {

    private long followerUserId;
    private String followerDisplayName;
    private String followerAvatarUrl;

    private boolean mutualFollow;
    private boolean blocked;
}
