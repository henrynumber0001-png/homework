package com.homework.web.app.service.impl;

import com.homework.common.exception.HomeworkException;
import com.homework.common.redisConstant.RedisConstant;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.utils.JwtUtil;
import com.homework.common.utils.TurnstileService;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.model.entity.UserInfo;
import com.homework.web.app.dto.EmailRegisterDTO;
import com.homework.web.app.mapper.UserAuthIdentityMapper;
import com.homework.web.app.mapper.UserInfoMapper;
import com.homework.web.app.service.ThirdPartyAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthIdentityServiceImplTest {

    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private TurnstileService turnstileService;
    @Mock private UserInfoMapper userInfoMapper;
    @Mock private UserAuthIdentityMapper userAuthIdentityMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private ThirdPartyAuthService thirdPartyAuthService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private UserAuthIdentityServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserAuthIdentityServiceImpl();
        ReflectionTestUtils.setField(service, "bCryptPasswordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "turnstileService", turnstileService);
        ReflectionTestUtils.setField(service, "userInfoMapper", userInfoMapper);
        ReflectionTestUtils.setField(service, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(service, "thirdPartyAuthService", thirdPartyAuthService);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", userAuthIdentityMapper);
    }

    @Test
    void secureTicketCanOnlyBeConsumedOnce() {
        EmailRegisterDTO dto = validRegistration();
        String ticketKey = RedisConstant.EMAIL_SECURE_TICKET + dto.getSecureTicket();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(ticketKey))
                .thenReturn("user@example.com")
                .thenReturn(null);
        when(userAuthIdentityMapper.selectOne(any(), eq(true))).thenReturn(null);
        when(userInfoMapper.insert(any(UserInfo.class))).thenAnswer(invocation -> {
            UserInfo userInfo = invocation.getArgument(0);
            userInfo.setId(7L);
            return 1;
        });
        when(userAuthIdentityMapper.insert(any(UserAuthIdentity.class))).thenReturn(1);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(jwtUtil.createToken(any(), any())).thenReturn("jwt-token");

        assertEquals("jwt-token", service.registerByEmail(dto, null));

        HomeworkException secondAttempt = assertThrows(
                HomeworkException.class,
                () -> service.registerByEmail(dto, null)
        );

        assertEquals(ResultCodeEnum.EMAIL_SECURE_TICKET_ERROR, secondAttempt.getResultCodeEnum());
        verify(valueOperations, times(2)).getAndDelete(ticketKey);
        verify(userAuthIdentityMapper, times(1)).insert(any(UserAuthIdentity.class));
    }

    @Test
    void ticketForDifferentEmailIsRejected() {
        EmailRegisterDTO dto = validRegistration();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(any())).thenReturn("someone-else@example.com");

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.registerByEmail(dto, null)
        );

        assertEquals(ResultCodeEnum.EMAIL_SECURE_TICKET_ERROR, exception.getResultCodeEnum());
    }

    @Test
    void passwordShorterThanEightCharactersIsRejected() {
        EmailRegisterDTO dto = validRegistration();
        dto.setPassword("1234567");
        dto.setPasswordConfirm("1234567");
        stubValidTicket(dto);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.registerByEmail(dto, null)
        );

        assertEquals(ResultCodeEnum.APP_LOGIN_PASSWORD_LENGTH_ERROR, exception.getResultCodeEnum());
    }

    @Test
    void mismatchedPasswordsAreRejected() {
        EmailRegisterDTO dto = validRegistration();
        dto.setPasswordConfirm("different123");
        stubValidTicket(dto);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.registerByEmail(dto, null)
        );

        assertEquals(ResultCodeEnum.APP_LOGIN_PASSWORD_CONFIRM_ERROR, exception.getResultCodeEnum());
    }

    private void stubValidTicket(EmailRegisterDTO dto) {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(
                RedisConstant.EMAIL_SECURE_TICKET + dto.getSecureTicket()
        )).thenReturn("user@example.com");
    }

    private static EmailRegisterDTO validRegistration() {
        EmailRegisterDTO dto = new EmailRegisterDTO();
        dto.setEmail("USER@example.com");
        dto.setPassword("password123");
        dto.setPasswordConfirm("password123");
        dto.setSecureTicket("secure-ticket");
        dto.setDisplayName("Henry");
        return dto;
    }
}
