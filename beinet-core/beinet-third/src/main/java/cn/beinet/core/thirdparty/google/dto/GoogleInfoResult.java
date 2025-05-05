package cn.beinet.core.thirdparty.google.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 下面接口返回的json数据格式
 * <a href="https://www.googleapis.com/oauth2/v1/userinfo">...</a>
 * @author youbl
 * @since 2024/9/11 10:53
 */
@Data
@Accessors(chain = true)
public class GoogleInfoResult {
    private String id;
    private String email;
    private Boolean verified_email;
    private String name;
    private String given_name;
    private String family_name;
    private String picture;
}
