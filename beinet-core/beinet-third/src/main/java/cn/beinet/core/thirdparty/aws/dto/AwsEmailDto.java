package cn.beinet.core.thirdparty.aws.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * aws 邮件发送对象
 * @author youbl
 * @since 2025/4/30 16:16
 */
@Data
@Accessors(chain = true)
public class AwsEmailDto {
    private String from;
    private String to;
    private String subject;
    private String content;
    private Boolean html;

    public boolean isHtml() {
        return html != null && html;
    }
}
