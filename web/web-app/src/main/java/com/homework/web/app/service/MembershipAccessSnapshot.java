package com.homework.web.app.service;

import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import java.time.LocalDateTime;

/** 应用层最终判定的唯一生效会员身份。 */
/*
快照的作用，不是为了缓存，也不是因为两张表变化少、查询一次可以长期使用
而是作为多项业务接口的唯一判断标准，使程序不用频繁的调用 BaseVip 和 SVip 的数据，然后自己再判断一遍会员是否过期
这样不利于数据维护、准确性和性能

快照就相当于是唯一出口
它是为了统一解释两张会员台账，把某一时刻的最终会员状态作为一个整体返回
 */
public record MembershipAccessSnapshot(
        MembershipStatus status,
        MembershipType membershipType,
        LocalDateTime currentExpireTime,
        LocalDateTime baseFreezeExpireTime
) {
}

//record 是 Java 专门用来表示“只保存一组数据的对象”的类型。
//record 创建后不能修改，创建后，其中保存的四个值不能重新赋值。
//如果会员状态变化了，不是修改旧的值，而是要创建一个新的record。
//所以才会被称为快照。

/*
相当于：
public final class MembershipAccessSnapshot {

    private final MembershipStatus status;
    private final MembershipType membershipType;
    private final LocalDateTime currentExpireTime;
    private final LocalDateTime baseFreezeExpireTime;

    public MembershipAccessSnapshot(
            MembershipStatus status,
            MembershipType membershipType,
            LocalDateTime currentExpireTime,
            LocalDateTime baseFreezeExpireTime
    ) {
        this.status = status;
        this.membershipType = membershipType;
        this.currentExpireTime = currentExpireTime;
        this.baseFreezeExpireTime = baseFreezeExpireTime;
    }

    public MembershipStatus status() {
        return status;
    }

    public MembershipType membershipType() {
        return membershipType;
    }

    public LocalDateTime currentExpireTime() {
        return currentExpireTime;
    }

    public LocalDateTime baseFreezeExpireTime() {
        return baseFreezeExpireTime;
    }
}
 */