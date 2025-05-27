package cn.beinet.business.login;

import cn.beinet.business.login.service.LoginService;
import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.sdk.login.LoginSdk;
import cn.beinet.sdk.login.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class LoginController implements LoginSdk {
    private final LoginService loginService;

    /**
     * 根据github授权码，去github获取用户信息，并完成登录
     *
     * @param code github的授权码
     * @return 用户信息，以及cookie里有token
     */
    @Override
    public ResponseData<UserDto> github(@RequestParam String code) {
        var ret = loginService.loginByGithub(code);
        return ResponseData.ok(ret);
    }

    /**
     * 根据google的token，去google获取用户信息，并完成登录
     *
     * @param accessToken google的token
     * @return 用户信息，以及cookie里有token
     */
    @Override
    public ResponseData<UserDto> google(@RequestParam String accessToken) {
        var ret = loginService.loginByGoogle(accessToken);
        return ResponseData.ok(ret);
    }
}
