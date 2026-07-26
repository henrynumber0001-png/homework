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


