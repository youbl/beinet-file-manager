package cn.beinet.sdk.login;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.sdk.login.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "login-sdk", url = "")
public interface LoginSdk {
    @Operation(summary = "根据github授权码，去github获取用户信息，并完成登录")
    @GetMapping("login/github")
    ResponseData<UserDto> github(@RequestParam String code);

    /**
     * 根据google的token，去google获取用户信息，并完成登录
     *
     * @param accessToken google的token
     * @return 用户信息，以及cookie里有token
     */
    @Operation(summary = "根据google的token，去google获取用户信息，并完成登录")
    @GetMapping("google/token")
    ResponseData<UserDto> googleCallback(@RequestParam String accessToken);
}
