package cn.beinet.core.thirdparty.google;

import cn.beinet.core.thirdparty.google.dto.GoogleInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Google登录对接辅助类
 */
@Component
@RequiredArgsConstructor
public class GoogleLoginUtil {

    private final static String USER_URL = "https://www.googleapis.com/oauth2/v1/userinfo?access_token=";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 根据google返回的access_token，去google api获取完整用户信息
     * @param googleAccessToken google的access_token
     * @return 用户信息
     */
    public GoogleInfoResult getUserInfoByToken(String googleAccessToken) {
        Assert.isTrue(StringUtils.hasLength(googleAccessToken), "Google token must not be empty");

        var url = USER_URL + URLEncoder.encode(googleAccessToken, StandardCharsets.UTF_8);
        var result = restTemplate.getForObject(url, GoogleInfoResult.class);
        Assert.notNull(result, "Google token is not valid");
        return result;
    }
}