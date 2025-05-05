package cn.beinet.core.thirdparty.github;

import cn.beinet.core.thirdparty.github.feigns.GithubApiFeign;
import cn.beinet.core.thirdparty.github.feigns.GithubTokenFeign;
import cn.beinet.core.thirdparty.github.feigns.dto.GithubTokenInputDto;
import cn.beinet.core.thirdparty.github.feigns.dto.GithubTokenOutputDto;
import cn.beinet.core.thirdparty.github.feigns.dto.GithubUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 获取Gitub用户信息的辅助类
 * @author youbl
 * @since 2025/4/29 18:30
 */
@Component
@RequiredArgsConstructor
public class GithubUtil {

    @Value("${github.client-id:}")
    private String githubClientId;
    @Value("${github.client-secret:}")
    private String githubClientSecret;

    private final GithubTokenFeign githubTokenFeign;
    private final GithubApiFeign githubApiFeign;

    /**
     * 根据github回调的code，获取github用户邮箱等信息
     * @param code 授权码
     * @return 用户信息
     */
    public GithubUserDto getUser(String code) {
        GithubTokenInputDto dto = new GithubTokenInputDto()
                .setClient_id(githubClientId)
                .setClient_secret(githubClientSecret)
                .setCode(code);
        // 根据授权码，获取access_token
        GithubTokenOutputDto ret = githubTokenFeign.getAccessToken(dto);
        if (!ret.success()) {
            throw new RuntimeException(ret.getError_description());
            // return "failed: " + ret.getError_description() + " " + ret.getError_uri();
        }

        String auth = ret.getToken_type() + " " + ret.getAccess_token();
        // 根据access_token, 获取用户信息
        return githubApiFeign.getUserInfo(auth);
    }
}
