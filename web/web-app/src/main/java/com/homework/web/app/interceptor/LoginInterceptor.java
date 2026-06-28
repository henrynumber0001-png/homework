package com.homework.web.app.interceptor;

import com.homework.common.exception.HomeworkException;
import com.homework.common.result.ResultCodeEnum;
import com.homework.common.utils.JwtUtil;
import com.homework.common.utils.LoginUserHolder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public LoginInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        String authorization = request.getHeader("Authorization");

        if(!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")){
            throw new HomeworkException(ResultCodeEnum.APP_LOGIN_NOT_AUTH);
        }

        String token = authorization.substring(7);

        try {
            Long userId = jwtUtil.getUserId(token);
            LoginUserHolder.setUserId(userId);
        }catch (ExpiredJwtException e){//如果token过期，抛异常
            throw new HomeworkException(ResultCodeEnum.TOKEN_EXPIRED);
        } catch(JwtException | IllegalArgumentException e){ //其他token异常（被篡改、格式错误、签名验证失败、token未生效，token是空字符串 等）
            throw new HomeworkException(ResultCodeEnum.TOKEN_INVALID);
        }
        return true;
    }

    @Override
    //整个请求处理完成后执行。
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        LoginUserHolder.removeUserId();
        /*
        LoginUserHolder 使用了 ThreadLocal。
        而 Web 服务器的线程会复用。
        不清理可能导致下一个请求读取到旧用户 ID。
         */
    }
}
