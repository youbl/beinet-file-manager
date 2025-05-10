package cn.beinet.sdk.login.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserDto {
    private String username;
    private String email;
    private String token;
}
