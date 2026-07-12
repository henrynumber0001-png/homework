package com.homework.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.homework.common.entity.BaseEntity;
import com.homework.model.enums.HitActionType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户对 Hit 动态的互动记录。
 *
 * <p>数据库用 (post_id, user_id, action_type) 唯一键保证同一种互动最多一条，
 * 取消互动时使用逻辑删除，以便再次点击时恢复同一条记录。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hit_action")
public class HitAction extends BaseEntity {

    private Long postId;

    private Long userId;

    /** 1.like; 2.favorite; 3.repost。 */
    private HitActionType actionType;
}
