package cn.beinet.sdk.login;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.sdk.login.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "login-sdk", url = "")
public interface LoginSdk {
    @Operation(summary = "github", description = "根据github授权码，去github获取用户信息，及登录后的token")
    @GetMapping("login/github")
    ResponseData<UserDto> github(@RequestParam String code);

    @Operation(summary = "google", description = "根据google的token，去google获取用户信息，及登录后的token")
    @GetMapping("login/google")
    ResponseData<UserDto> google(@RequestParam String accessToken);
}
