package com.homework.web.app.controller.login;


import com.homework.common.result.Result;
import com.homework.web.app.dto.*;
import com.homework.web.app.service.UserAuthIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
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


    @Operation(summary = "Email Register")
    @PostMapping("/register/email")
    public Result<String> registerByEmail(@RequestBody EmailRegisterDTO emailRegisterDTO, HttpServletRequest request) {
        String token = userAuthIdentityService.registerByEmail(emailRegisterDTO,request);
        return Result.success(token);
    }

//    @Operation(summary = "Phone Register")
//    @PostMapping("/register/phone")
//    public Result<String> registerByPhone(@RequestBody PhoneRegisterDTO phoneRegisterDTO) {
//        return Result.success();
//    }

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
        return Result.success();
    }

//    @Operation(summary = "Phone Login")
//    @PostMapping("/login/phone")
//    public Result<String> loginByPhone(@RequestBody PhoneRegisterDTO phoneRegisterDTO) {
//        return Result.fail();
//    }

    @Operation(summary = "Third Party Login")
    @PostMapping("/login/oauth")
    public Result<String> loginByOAuth(@RequestBody ThirdPartyLoginDTO thirdPartyLoginDTO,HttpServletRequest request) {
        String token = userAuthIdentityService.loginByOAuth(thirdPartyLoginDTO,request);
        return Result.success(token);
    }


}
