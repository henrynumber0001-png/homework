package com.homework.web.app.controller.login;


import com.homework.common.result.Result;
import com.homework.web.app.dto.*;
import com.homework.web.app.service.UserAuthIdentityService;
import com.homework.web.app.service.impl.EmailCodeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/app/auth")
public class AuthController {

    @Autowired
    private UserAuthIdentityService userAuthIdentityService;
    @Autowired
    private EmailCodeService emailCodeService;


    @PostMapping("/register/email/code/send")
    public Result<Void> sendEmailCode(@Valid @RequestBody EmailSendDTO emailSendDTO,HttpServletRequest request) {
        emailCodeService.sendEmailCode(emailSendDTO,request);
        return Result.success();
    }

    @PostMapping("/register/email/code/send/verify")
    public Result<String> verifyEmailCode(@Valid @RequestBody EmailVerifyDTO emailVerifyDTO) {
        String secureTicket = emailCodeService.verifyEmailCode(emailVerifyDTO);
        return Result.success(secureTicket);
    }

    @Operation(summary = "Email Register")
    @PostMapping("/register/email")
    public Result<String> registerByEmail(@Valid @RequestBody EmailRegisterDTO emailRegisterDTO, HttpServletRequest request) {
        String token = userAuthIdentityService.registerByEmail(emailRegisterDTO,request);
        return Result.success(token);
    }

    @Operation(summary = "Third Party Register")
    @PostMapping("/register/oauth")
    public Result<String> registerByOAuth(@RequestBody ThirdPartyRegisterDTO thirdPartyRegisterDTO,HttpServletRequest request) {
        String token = userAuthIdentityService.registerByOAuth(thirdPartyRegisterDTO,request);
        return Result.success(token);
    }

    @Operation(summary = "Email Login")
    @PostMapping("/login/email")
    public Result<String> loginByEmail(@RequestBody EmailLoginDTO emailLoginDTO, HttpServletRequest request) {
        String token = userAuthIdentityService.loginByEmail(emailLoginDTO,request);
        return Result.success(token);
    }

    @Operation(summary = "Third Party Login")
    @PostMapping("/login/oauth")
    public Result<String> loginByOAuth(@RequestBody ThirdPartyLoginDTO thirdPartyLoginDTO,HttpServletRequest request) {
        String token = userAuthIdentityService.loginByOAuth(thirdPartyLoginDTO,request);
        return Result.success(token);
    }


}
