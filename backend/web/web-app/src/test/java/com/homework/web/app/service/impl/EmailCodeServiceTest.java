package com.homework.web.app.service.impl;

import com.homework.common.encoder.VerifyCodeGenerator;
import com.homework.common.exception.HomeworkException;
import com.homework.common.redisConstant.RedisConstant;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.utils.TurnstileService;
import com.homework.model.entity.UserAuthIdentity;
import com.homework.web.app.dto.EmailSendDTO;
import com.homework.web.app.dto.EmailVerifyDTO;
import com.homework.web.app.mapper.UserAuthIdentityMapper;
import com.homework.web.app.service.EmailCodeSender;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailCodeServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String REMOTE_IP = "203.0.113.10";

    @Mock private UserAuthIdentityMapper userAuthIdentityMapper;
    @Mock private TurnstileService turnstileService;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private VerifyCodeGenerator verifyCodeGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailCodeSender emailCodeSender;
    @Mock private EmailCodeRateLimiter emailCodeRateLimiter;
    @Mock private HttpServletRequest request;

    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        service = new EmailCodeService(
                userAuthIdentityMapper,
                turnstileService,
                stringRedisTemplate,
                verifyCodeGenerator,
                passwordEncoder,
                emailCodeSender,
                emailCodeRateLimiter
        );
    }

    @Test
    void turnstileFailureStopsBeforeDatabaseAndRedis() {
        EmailSendDTO dto = sendDto();
        when(request.getRemoteAddr()).thenReturn(REMOTE_IP);
        when(turnstileService.verify("turnstile-token", REMOTE_IP)).thenReturn(false);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.sendEmailCode(dto, request)
        );

        assertEquals(ResultCodeEnum.APP_LOGIN_TURNSTILE_VERIFY_ERROR, exception.getResultCodeEnum());
        verifyNoInteractions(userAuthIdentityMapper, stringRedisTemplate, emailCodeSender, emailCodeRateLimiter);
    }

    @Test
    void registeredEmailDoesNotSendCode() {
        EmailSendDTO dto = sendDto();
        when(request.getRemoteAddr()).thenReturn(REMOTE_IP);
        when(turnstileService.verify("turnstile-token", REMOTE_IP)).thenReturn(true);
        when(userAuthIdentityMapper.selectOne(any())).thenReturn(new UserAuthIdentity());

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.sendEmailCode(dto, request)
        );

        assertEquals(ResultCodeEnum.APP_LOGIN_EMAIL_EXIST, exception.getResultCodeEnum());
        verifyNoInteractions(stringRedisTemplate, emailCodeSender, emailCodeRateLimiter);
    }

    @Test
    void successfulSendStoresHashedCodeAndStartsCooldown() {
        stubSuccessfulSendPreparation();

        service.sendEmailCode(sendDto(), request);

        String emailHash = sha256(EMAIL);
        String resendKey = RedisConstant.EMAIL_VERIFY_CODE_RESEND + emailHash;
        String codeKey = RedisConstant.EMAIL_VERIFY_CODE + emailHash;

        verify(emailCodeRateLimiter).check(emailHash, REMOTE_IP);
        verify(valueOperations).setIfAbsent(resendKey, "1", 60L, TimeUnit.SECONDS);
        verify(hashOperations).put(codeKey, "codeHash", "encoded-code");
        verify(hashOperations).put(codeKey, "attempt", "0");
        verify(stringRedisTemplate).expire(codeKey, 60L, TimeUnit.SECONDS);
        verify(emailCodeSender).sendCode(EMAIL, "123456");
    }

    @Test
    void activeCooldownRejectsAnotherSend() {
        stubSendUntilCooldown();
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.sendEmailCode(sendDto(), request)
        );

        assertEquals(ResultCodeEnum.APP_LOGIN_EMAIL_RESEND_LOCK, exception.getResultCodeEnum());
        verifyNoInteractions(verifyCodeGenerator, passwordEncoder, emailCodeSender);
    }

    @Test
    void sesFailureDeletesCodeAndCooldownKeys() {
        stubSuccessfulSendPreparation();
        RuntimeException sendFailure = new RuntimeException("SES unavailable");
        doThrow(sendFailure).when(emailCodeSender).sendCode(EMAIL, "123456");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> service.sendEmailCode(sendDto(), request)
        );

        assertSame(sendFailure, thrown);
        String emailHash = sha256(EMAIL);
        verify(stringRedisTemplate).delete(RedisConstant.EMAIL_VERIFY_CODE_RESEND + emailHash);
        verify(stringRedisTemplate).delete(RedisConstant.EMAIL_VERIFY_CODE + emailHash);
    }

    @Test
    void expiredCodeIsRejected() {
        stubVerifyCodeLookup();
        when(hashOperations.get(anyString(), eq("codeHash"))).thenReturn(null);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.verifyEmailCode(verifyDto("123456"))
        );

        assertEquals(ResultCodeEnum.EMAIL_CODE_EXPIRED, exception.getResultCodeEnum());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void fourthWrongAttemptKeepsCode() {
        stubWrongCodeAttempt(4L);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.verifyEmailCode(verifyDto("111111"))
        );

        assertEquals(ResultCodeEnum.EMAIL_CODE_ERROR, exception.getResultCodeEnum());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

    @Test
    void fifthWrongAttemptDeletesCode() {
        stubWrongCodeAttempt(5L);

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.verifyEmailCode(verifyDto("111111"))
        );

        assertEquals(ResultCodeEnum.EMAIL_CODE_ERROR, exception.getResultCodeEnum());
        verify(stringRedisTemplate).delete(RedisConstant.EMAIL_VERIFY_CODE + sha256(EMAIL));
    }

    @Test
    void correctCodeDeletesCodeAndCreatesSecureTicket() {
        stubVerifyCodeLookup();
        when(hashOperations.get(anyString(), eq("codeHash"))).thenReturn("encoded-code");
        when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
        when(verifyCodeGenerator.generateSecureTicket()).thenReturn("secure-ticket");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String ticket = service.verifyEmailCode(verifyDto("123456"));

        assertEquals("secure-ticket", ticket);
        verify(stringRedisTemplate).delete(RedisConstant.EMAIL_VERIFY_CODE + sha256(EMAIL));
        verify(valueOperations).set(
                RedisConstant.EMAIL_SECURE_TICKET + "secure-ticket",
                EMAIL,
                15L * 60,
                TimeUnit.SECONDS
        );
    }

    @Test
    void registeredEmailCannotVerifyCode() {
        EmailVerifyDTO dto = verifyDto("123456");
        when(userAuthIdentityMapper.selectOne(any())).thenReturn(new UserAuthIdentity());

        HomeworkException exception = assertThrows(
                HomeworkException.class,
                () -> service.verifyEmailCode(dto)
        );

        assertEquals(ResultCodeEnum.APP_LOGIN_EMAIL_EXIST, exception.getResultCodeEnum());
        verifyNoInteractions(stringRedisTemplate, passwordEncoder, verifyCodeGenerator);
    }

    private void stubSendUntilCooldown() {
        when(request.getRemoteAddr()).thenReturn(REMOTE_IP);
        when(turnstileService.verify("turnstile-token", REMOTE_IP)).thenReturn(true);
        when(userAuthIdentityMapper.selectOne(any())).thenReturn(null);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private void stubSuccessfulSendPreparation() {
        stubSendUntilCooldown();
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);
        when(verifyCodeGenerator.generateVerifyCode()).thenReturn("123456");
        when(passwordEncoder.encode("123456")).thenReturn("encoded-code");
        when(stringRedisTemplate.<Object, Object>opsForHash()).thenReturn(hashOperations);
    }

    private void stubVerifyCodeLookup() {
        when(userAuthIdentityMapper.selectOne(any())).thenReturn(null);
        when(stringRedisTemplate.<Object, Object>opsForHash()).thenReturn(hashOperations);
    }

    private void stubWrongCodeAttempt(long attempt) {
        stubVerifyCodeLookup();
        when(hashOperations.get(anyString(), eq("codeHash"))).thenReturn("encoded-code");
        when(passwordEncoder.matches("111111", "encoded-code")).thenReturn(false);
        when(hashOperations.increment(anyString(), eq("attempt"), eq(1L))).thenReturn(attempt);
    }

    private static EmailSendDTO sendDto() {
        EmailSendDTO dto = new EmailSendDTO();
        dto.setEmail(" User@Example.com ");
        dto.setTurnstileToken("turnstile-token");
        return dto;
    }

    private static EmailVerifyDTO verifyDto(String code) {
        EmailVerifyDTO dto = new EmailVerifyDTO();
        dto.setEmail(EMAIL);
        dto.setCode(code);
        return dto;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
