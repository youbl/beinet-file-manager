package cn.beinet.business.login.exception;

/**
 * 认证异常类
 * 
 * @author youbl
 * @since 2024-12-20
 */
public class AuthenticationException extends RuntimeException {
    
    private final int code;
    
    public AuthenticationException(String message) {
        super(message);
        this.code = 401;
    }
    
    public AuthenticationException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.code = 401;
    }
    
    public AuthenticationException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
}
