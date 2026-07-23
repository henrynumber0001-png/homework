package com.homework.web.app.service;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.model.entity.BaseVipRecord;
import com.homework.model.entity.SvipRecord;
import com.homework.model.enums.MembershipStatus;
import com.homework.model.enums.MembershipType;
import com.homework.web.app.mapper.BaseVipRecordMapper;
import com.homework.web.app.mapper.SvipRecordMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipAccessServiceTest {

    @Mock
    private BaseVipRecordMapper baseVipMapper;
    @Mock
    private SvipRecordMapper svipMapper;

    @Test
    void svipWinsAndBaseVipIsReportedAsFrozen() {
        LocalDateTime now = LocalDateTime.now();
        SvipRecord svip = new SvipRecord();
        svip.setExpireTime(now.plusDays(20));
        BaseVipRecord baseVip = new BaseVipRecord();
        baseVip.setExpireTime(now.plusDays(80));
        when(svipMapper.selectOne(any())).thenReturn(svip);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);

        MembershipAccessSnapshot access =
                new MembershipAccessService(baseVipMapper, svipMapper).getAccess(7L);

        assertEquals(MembershipStatus.PREMIUM_PLUS, access.status());
        assertEquals(MembershipType.PREMIUM_PLUS, access.membershipType());
        assertEquals(svip.getExpireTime(), access.currentExpireTime());
        assertEquals(baseVip.getExpireTime(), access.baseFreezeExpireTime());
        assertTrue(access.status() == MembershipStatus.PREMIUM_PLUS);
    }

    @Test
    void expiredSvipFallsBackToBaseVip() {
        LocalDateTime now = LocalDateTime.now();
        SvipRecord svip = new SvipRecord();
        svip.setExpireTime(now.minusMinutes(1));
        BaseVipRecord baseVip = new BaseVipRecord();
        baseVip.setExpireTime(now.plusDays(30));
        when(svipMapper.selectOne(any())).thenReturn(svip);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);

        MembershipAccessSnapshot access =
                new MembershipAccessService(baseVipMapper, svipMapper).getAccess(7L);

        assertEquals(MembershipStatus.PREMIUM, access.status());
        assertEquals(baseVip.getExpireTime(), access.currentExpireTime());
    }

    @Test
    void premiumCannotUsePremiumPlusOnlyFeature() {
        BaseVipRecord baseVip = new BaseVipRecord();
        baseVip.setExpireTime(LocalDateTime.now().plusDays(30));
        when(svipMapper.selectOne(any())).thenReturn(null);
        when(baseVipMapper.selectOne(any())).thenReturn(baseVip);

        HomeworkException error = assertThrows(
                HomeworkException.class,
                () -> new MembershipAccessService(
                        baseVipMapper,
                        svipMapper
                ).requirePremiumPlus(7L)
        );

        assertEquals(
                ResultCodeEnum.PREMIUM_PLUS_MEMBERSHIP_REQUIRED,
                error.getResultCodeEnum()
        );
    }
}
