package cn.beinet.deployment.admin.auth;

/** 可公开的错误分类与仅供服务端查看的诊断上下文，原始异常只作为 cause 保存。 */
public class LoginFailure extends RuntimeException {
    private final String reason;
    private final String context;
    public LoginFailure(String reason) { this(reason, "", null); }
    public LoginFailure(String reason, String context, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
        this.context = context;
    }
    public String reason() { return reason; }
    public String context() { return context; }
}
