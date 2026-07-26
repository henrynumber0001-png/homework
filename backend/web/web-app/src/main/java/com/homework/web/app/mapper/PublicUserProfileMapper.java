package com.homework.web.app.mapper;

import com.homework.web.app.vo.PublicUserProfileActivityRowVO;
import com.homework.web.app.vo.PublicUserProfileCountsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PublicUserProfileMapper {
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM user_follow WHERE followee_user_id=#{userId} AND is_deleted=0) follower_count,
              (SELECT COUNT(*) FROM user_follow WHERE follower_user_id=#{userId} AND is_deleted=0) following_count,
              (SELECT COUNT(*) FROM hit_post WHERE post_user_id=#{userId} AND post_status=1 AND is_deleted=0)
                + (SELECT COUNT(*) FROM hit_action a JOIN hit_post p ON p.id=a.post_id
                   WHERE a.action_user_id=#{userId} AND a.action_type=3 AND a.is_deleted=0
                     AND p.post_status=1 AND p.is_deleted=0) post_count,
              (SELECT COUNT(*) FROM user_question_answer WHERE user_id=#{userId} AND is_deleted=0) answered_question_count,
              (SELECT COUNT(DISTINCT bank_id) FROM user_question_answer WHERE user_id=#{userId} AND is_deleted=0) learned_bank_count,
              (SELECT COALESCE(SUM(study_seconds),0) FROM user_learning_stat_daily WHERE user_id=#{userId} AND is_deleted=0) study_seconds,
              (SELECT COALESCE(SUM(like_count+favorite_count+repost_count),0) FROM hit_post
               WHERE post_user_id=#{userId} AND post_status=1 AND is_deleted=0)
                + (SELECT COALESCE(SUM(c.like_count),0) FROM hit_comment c JOIN hit_post p ON p.id=c.post_id
                   WHERE c.comment_user_id=#{userId} AND c.is_deleted=0
                     AND p.post_status=1 AND p.is_deleted=0) received_total_action_count
            """)
    PublicUserProfileCountsVO selectCounts(@Param("userId") Long userId);

    List<PublicUserProfileActivityRowVO> listActivities(@Param("userId") Long userId,
                                                       @Param("tab") String tab,
                                                       @Param("offset") long offset,
                                                       @Param("limit") long limit);
}
