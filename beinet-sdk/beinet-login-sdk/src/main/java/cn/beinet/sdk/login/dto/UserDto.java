package cn.beinet.sdk.login.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String token;
    private String avatar;
    private Integer status;
    private LocalDateTime lastLoginDate;
    private Integer loginCount;
    private String loginType;
    
    // GitHub 相关信息 (可选返回)
    private Long githubId;
    private String githubLogin;
    private String githubAvatarUrl;
    private String githubNodeId;
    
    // 扩展信息
    private String lastLoginIp;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
