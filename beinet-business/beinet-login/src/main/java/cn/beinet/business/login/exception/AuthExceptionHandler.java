package cn.beinet.business.login.exception;

import cn.beinet.core.base.commonDto.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 认证异常处理器
 * 
 * @author youbl
 * @since 2024-12-20
 */
@RestControllerAdvice
@Slf4j
public class AuthExceptionHandler {
    
    /**
     * 处理认证异常
     * 
     * @param e 认证异常
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseData<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: code={}, message={}", e.getCode(), e.getMessage());
        return ResponseData.fail(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理未授权异常
     * 
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseData<Void> handleAccessDeniedException(Exception e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        return ResponseData.fail(403, "访问被拒绝: " + e.getMessage());
    }
}
