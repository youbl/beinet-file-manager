package cn.beinet.deployment.admin.thirdParty;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.core.thirdparty.aws.AwsEmailUtil;
import cn.beinet.core.thirdparty.aws.dto.AwsEmailDto;
import cn.beinet.core.thirdparty.github.GithubUtil;
import cn.beinet.core.thirdparty.github.feigns.dto.GithubUserDto;
import cn.beinet.core.thirdparty.google.GoogleLoginUtil;
import cn.beinet.core.thirdparty.google.dto.GoogleInfoResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 第三方接口
 * @author youbl
 * @since 2025/4/29 18:48
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "ThirdParty", description = "第三方接口")
public class ThirdPartyController {
    private final GithubUtil githubUtil;
    private final GoogleLoginUtil googleLoginUtil;
    private final AwsEmailUtil awsEmailUtil;

    /**
     * 根据github授权码，去github获取用户信息
     * @param code github的授权码
     * @return 用户信息
     */
    @Operation(summary = "根据github授权码，去github获取用户信息")
    @GetMapping("github/callback")
    public ResponseData<GithubUserDto> githubCallback(@RequestParam String code) {
        var ret = githubUtil.getUser(code);
        return ResponseData.ok(ret);
    }

    /**
     * 根据google的token，去google获取用户信息
     * @param accessToken google的token
     * @return 用户信息
     */
    @Operation(summary = "根据google的token，去google获取用户信息")
    @GetMapping("google/token")
    public ResponseData<GoogleInfoResult> googleCallback(@RequestParam String accessToken) {
        var ret = googleLoginUtil.getUserInfoByToken(accessToken);
        return ResponseData.ok(ret);
    }

    /**
     * 使用aws发邮件
     * @param dto 邮件信息
     * @return 无
     */
    @Operation(summary = "使用aws发邮件")
    @PostMapping("aws/email")
    public ResponseData<Void> sendEmail(@RequestBody AwsEmailDto dto) {
        awsEmailUtil.sendEmail(dto);
        return ResponseData.ok();
    }
}
