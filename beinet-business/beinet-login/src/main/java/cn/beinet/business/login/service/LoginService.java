package cn.beinet.business.login.service;

import cn.beinet.core.thirdparty.github.GithubUtil;
import cn.beinet.core.thirdparty.github.feigns.dto.GithubUserDto;
import cn.beinet.core.thirdparty.google.GoogleLoginUtil;
import cn.beinet.core.thirdparty.google.dto.GoogleInfoResult;
import cn.beinet.sdk.login.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class LoginService {
    private final GithubUtil githubUtil;
    private final GoogleLoginUtil googleLoginUtil;

    /**
     * 根据github授权码，去github获取用户信息，并完成登录
     *
     * @param code github的授权码
     * @return 用户信息，以及token
     */
    public UserDto loginByGithub(String code) {
        var user = githubUtil.getUser(code);
        var ret = fromGitUser(user);
        generateAndSetToken(ret);
        return ret;
    }

    /**
     * 根据google的token，去google获取用户信息，并完成登录
     *
     * @param accessToken google的token
     * @return 用户信息，以及token
     */
    public UserDto loginByGoogle(String accessToken) {
        var user = googleLoginUtil.getUserInfoByToken(accessToken);
        var ret = fromGoogleUser(user);
        generateAndSetToken(ret);
        return ret;
    }

    private void generateAndSetToken(UserDto user) {
        Assert.hasLength(user.getEmail(), "user email is empty.");
        if (!StringUtils.hasLength(user.getUsername())) {
            user.setUsername(user.getEmail());
        }

        // 完成登录，并生成token
        user.setToken();
    }

    private UserDto fromGitUser(GithubUserDto githubUser) {
        return new UserDto()
                .setUsername(githubUser.getName())
                .setEmail(githubUser.getEmail());
    }

    private UserDto fromGoogleUser(GoogleInfoResult googleUser) {
        return new UserDto()
                .setUsername(googleUser.getName())
                .setEmail(googleUser.getEmail());
    }
}
