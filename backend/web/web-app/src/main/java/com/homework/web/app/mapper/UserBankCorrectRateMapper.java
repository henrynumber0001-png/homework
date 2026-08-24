package com.homework.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.model.entity.UserBankCorrectRate;
import com.homework.web.app.vo.BankCorrectRateVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserBankCorrectRateMapper extends BaseMapper<UserBankCorrectRate> {

    @Select("""
        <script>
        SELECT
            bank_id AS bankId,
            COALESCE(AVG(correct_rate), 0) AS avgCorrectRate
        FROM user_bank_correct_rate
        WHERE is_deleted = 0
          AND bank_id IN
          <foreach collection="bankIds"
                   item="bankId"
                   open="("
                   separator=","
                   close=")">
              #{bankId}
          </foreach>
        GROUP BY bank_id
        </script>
        """)
    List<BankCorrectRateVO> selectAverageByBankIds(List<Long> bankIds);
    //这是查询，不是Update，返回值怎么可能是 int ？？
}

// <foreach = list.foreach(bankId:bankIds){}
// collection = 要遍历的集合名字
// item = 遍历集合中每个元素的名字
// open="(" 和 close=")" :因为 SQL 的 IN 必须写成 bank_id IN (10, 20, 30) 这种，所以 open="(" 和 close=")" 就是告诉我 MyBatis，遍历开始前加 (，遍历结束后加 )
