package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.HitComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HitCommentMapper extends BaseMapper<HitComment> {
    @Select("""
            SELECT * 
            FROM hit_comment WHERE id = #{id} LIMIT 1
            """)
    HitComment selectIncludingDeleted(@Param("id") Long id);

    @Select("""
            <script>
            SELECT *
            FROM hit_comment
            WHERE id IN
            <foreach collection="ids"
                     item="id"
                     open="("
                     separator=","
                     close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<HitComment> selectIncludingDeletedByIds(@Param("ids") Collection<Long> ids);

    @Select("SELECT id FROM hit_comment WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Long lockActive(@Param("id") Long id);

    @Update("""
            UPDATE hit_comment
            SET like_count = GREATEST(COALESCE(like_count, 0) + #{delta}, 0),
                updated_time = CURRENT_TIMESTAMP
            WHERE id = #{id} AND is_deleted = 0
            """)
    int changeLikeCount(@Param("id") Long id, @Param("delta") int delta);
}
