package cn.beinet.core.thirdparty.aws;

import cn.beinet.core.thirdparty.aws.dto.AwsEmailDto;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * aws邮件发送辅助类.
 * 官方文档： <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_sesv2_code_examples.html">...</a>
 * @author youbl
 * @since 2024/9/18 10:25
 */
@Component
public class AwsEmailUtil {

    // 发件人
    @Value("${aws.email.from:}")
    private String defaultMailFrom;
    @Value("${aws.email.access-key:}")
    private String accessKey;
    @Value("${aws.email.secret-key:}")
    private String secretKey;
    @Value("${aws.email.region:us-east-1}")
    private String region;

    private SesV2Client sesClient;

    /**
     * 发送邮件,
     * <a href="https://github.com/awsdocs/aws-doc-sdk-examples/blob/main/javav2/example_code/ses/src/main/java/com/example/sesv2/SendEmail.java">参考代码</a>
     *
     * @param dto 邮件信息
     */
    @SneakyThrows
    public void sendEmail(AwsEmailDto dto) {
        if (!StringUtils.hasLength(dto.getFrom())) {
            dto.setFrom(defaultMailFrom);
        }
        if (!StringUtils.hasLength(dto.getFrom())) {
            throw new IllegalArgumentException("mail-from cannot be null or empty");
        }
        if (!StringUtils.hasLength(dto.getTo())) {
            throw new IllegalArgumentException("mail-to cannot be null or empty");
        }
        if (isTest(dto.getTo())) {
            return;
        }
        Destination destination = Destination.builder()
                .toAddresses(dto.getTo())
                .build();
        Content sub = Content.builder()
                .data(dto.getSubject())
                .build();
        Content content = Content.builder()
                .data(dto.getContent())
                .build();
        Body body;
        if (dto.isHtml()) {
            body = Body.builder().html(content).build();
        } else {
            body = Body.builder().text(content).build();
        }
        Message msg = Message.builder()
                .subject(sub)
                .body(body)
                .build();
        EmailContent emailContent = EmailContent.builder()
                .simple(msg)
                .build();
        SendEmailRequest request = SendEmailRequest.builder()
                .destination(destination)
                .content(emailContent)
                // 必需，否则抛异常：
                // software.amazon.awssdk.services.sesv2.model.BadRequestException: Service returned error code BadRequestException (Service: SesV2, Status Code: 400, Request ID: d7271b63-b1f5-46f8-bea5-8ddb9ea71dc5)
                .fromEmailAddress(dto.getFrom())
                .build();


        getClient().sendEmail(request);
    }

    private SesV2Client getClient() {
        if (sesClient == null) {
            var credentials = AwsBasicCredentials.create(accessKey, secretKey);
            sesClient = SesV2Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .build();
        }
        return sesClient;
    }

    private boolean isTest(String email) {
        if (email.indexOf("@1.com") > 0) {
            // 测试邮箱
            return true;
        }
        return false;
    }
}
