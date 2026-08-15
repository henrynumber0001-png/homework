package com.homework.web.app.vo;

import lombok.Data;

@Data
public class FolloweeVO {

    private long followeeUserId;
    private String followeeDisplayName;
    private String followeeAvatarUrl;

    private boolean mutualFollow;
    private boolean blocked;
}
