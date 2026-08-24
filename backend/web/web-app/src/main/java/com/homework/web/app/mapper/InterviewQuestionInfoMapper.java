package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.InterviewQuestionInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterviewQuestionInfoMapper extends BaseMapper<InterviewQuestionInfo> {

    @Update("""
                    UPDATE question_bank
                    SET view_count = view_count + 1
                    WHERE id = #{bankId} AND is_deleted = 0
            """)
    int incrementViewCount(Long bankId);


    @Update("""
        UPDATE question_bank
        SET complete_count = COALESCE(complete_count,0) + 1
        WHERE id = #{bankId}
          AND is_deleted = 0
        """)
    int bankCompletionCount(Long bankId);

}
/*
所谓数据库的 原子，实际上就是指 一个完整的 UPDATE/DELETE/INSERT 操作
是指这个操作从查询条件到数据变更，全部都在SQL中执行
所以SQL会自动添加一个 排他锁，这个 排他锁 是隐藏式的

 但 SELECT 操作不是原子的，不会自动加 排他锁，也就是谁都可以查，且同时查询
 所以 SELECT 操作才会有 FOR UPDATE，用于：“我要读取这条数据，而且接下来我要基于这个读取结果做一些操作。在我的事务结束之前，不允许其他事务抢先修改它。”

 接下来看两个例子：
1. SQL的原子性：
 UPDATE question_bank
        SET complete_count = COALESCE(complete_count,0) + 1
        WHERE id = #{bankId}
          AND is_deleted = 0

2. 只有在Java中分开多个方法操作时，才会产生非原子性：
QuestionBank bank = questionBankMapper.selectById(bankId); //双方可以同时查询（没有 For Update）
bank.setCompleteCount(bank.getCompleteCount() + 1); //同时读取 complete_count = 100, 都可以操作 100+1，这两个操作是并发的
questionBankMapper.updateById(bank); //那么最后更新的都是 100+1，因为是并发，那就必然有一个更新是无效的，最终结果只能是 101，而非102 （所哟这种情况才需要 乐观锁）
 */