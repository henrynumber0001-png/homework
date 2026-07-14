package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.HitPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HitPostMapper extends BaseMapper<HitPost> {

    /**
     * 原子修改互动计数，避免并发请求同时读取旧值后互相覆盖。
     * GREATEST 确保异常重试或历史脏数据也不会产生负数。
     */
    @Update("""
            UPDATE hit_post
            SET
              like_count = GREATEST(COALESCE(like_count, 0) + #{likeDelta}, 0),
              favorite_count = GREATEST(COALESCE(favorite_count, 0) + #{favoriteDelta}, 0),
              repost_count = GREATEST(COALESCE(repost_count, 0) + #{repostDelta}, 0),
              updated_time = CURRENT_TIMESTAMP
            WHERE id = #{postId} AND is_deleted = 0 AND post_status = 1
            """)
    int changeActionCounters(@Param("postId") Long postId,
                             @Param("likeDelta") int likeDelta,
                             @Param("favoriteDelta") int favoriteDelta,
                             @Param("repostDelta") int repostDelta);

    /** 评论计数同样使用原子 SQL 更新。 */
    @Update("""
            UPDATE hit_post
            SET comment_count = GREATEST(COALESCE(comment_count, 0) + #{delta}, 0),
                updated_time = CURRENT_TIMESTAMP
            WHERE id = #{postId} AND is_deleted = 0 AND post_status = 1
            """)
    int changeCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    @Select("""
            SELECT
                id
            FROM hit_post
            WHERE id = #{postId} AND is_deleted = 0 AND post_status = 1
            FOR UPDATE
            """)
    Long lockPublishedPost(@Param("postId") Long postId);
    /*
    假设 postId = 100，这条 SQL 做两件事：
    检查 Hit 100 是否存在、已发布且未删除。
    给 hit_post 中 ID 为 100 的记录加排他锁。
    这个锁会一直持有到当前事务 COMMIT 或 ROLLBACK。

    没有 FOR UPDATE 会怎样
    假设用户同时发出两次点赞请求。
    事务 A：
    查询 hit_action：没有点赞记录
    准备插入点赞
    事务 B 几乎同时执行：
    查询 hit_action：也没有点赞记录
    准备插入点赞
    两边都认为自己是第一次点赞：
    事务 A：INSERT hit_action
    事务 B：INSERT hit_action
    最终可能出现：
    唯一键冲突；
    数据库死锁；
    一个请求成功，另一个请求异常；
    如果表没有唯一键，甚至可能插入两条并重复增加计数。
    问题的根源是：对于“尚不存在的点赞记录”，没有现成的行可以稳定地锁住。

    加上 FOR UPDATE 后
    两个请求都先锁定一定存在的 hit_post 记录。
     */

}
