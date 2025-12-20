package cn.beinet.business.login.dal.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Data
@Accessors(chain = true)
@TableName("audit_logs")
public class AuditLogs implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联users表
     */
    @TableField(value = "`user_id`")
    private Long userId;

    /**
     * 事件类型: LOGIN, LOGOUT, AUTH_FAILED, TOKEN_EXPIRED
     */
    @Size(max = 50)
    @TableField(value = "`event_type`")
    private String eventType;

    /**
     * 事件描述
     */
    @Size(max = 500)
    @TableField(value = "`event_description`")
    private String eventDescription;

    /**
     * 客户端IP地址
     */
    @Size(max = 45)
    @TableField(value = "`ip_address`")
    private String ipAddress;

    /**
     * 用户代理字符串
     */
    @Size(max = 1000)
    @TableField(value = "`user_agent`")
    private String userAgent;

    /**
     * 请求URI
     */
    @Size(max = 500)
    @TableField(value = "`request_uri`")
    private String requestUri;

    /**
     * 会话ID
     */
    @Size(max = 100)
    @TableField(value = "`session_id`")
    private String sessionId;

    /**
     * 结果: 0=失败, 1=成功
     */
    @TableField(value = "`result`")
    private Integer result;

    /**
     * 错误信息（如果有）
     */
    @Size(max = 500)
    @TableField(value = "`error_message`")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "`create_time`", insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;
}