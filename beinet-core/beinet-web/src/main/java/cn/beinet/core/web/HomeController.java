package cn.beinet.core.web;

import cn.beinet.core.utils.IpHelper;
import cn.beinet.core.web.context.ContextUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 测试接口类
 *
 * @author youbl
 * @since 2024/7/10 9:54
 */
@RestController
@Tag(name = "test", description = "测试接口")
@Hidden
@Slf4j
public class HomeController {
    
    @GetMapping(value = "test", produces = {"text/plain"})
    @Operation(summary = "默认接口")
    public String test(HttpServletRequest request) {
        ContextUtils.setTraceId("abc");
        var ret = IpHelper.getRequestHeader(request);
        log.info("请求test接口");
        return getServerInfo(ret);
    }

    @PostMapping(value = "test", produces = {"text/plain"})
    @Operation(summary = "默认接口")
    @SneakyThrows
    public String testPost(HttpServletRequest request) {
        var ret = IpHelper.getRequestHeader(request);

        // 获取请求体并转换为字符串
        var body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        ret += "\r\n\r\n  body: " + body;

        return getServerInfo(ret);
    }

    @GetMapping("test/err")
    @Operation(summary = "测试异常响应")
    public String err() {
        throw new IllegalArgumentException("test 一下下 err");
    }

    @NotNull
    private String getServerInfo(String ret) {
        var ts1 = System.currentTimeMillis();
        var now = LocalDateTime.now();
        var ts3 = Timestamp.valueOf(now).getTime();

        var zone = TimeZone.getDefault();
        ret += "\r\n\r\n server ip: " + IpHelper.getServerIp() +
                "\r\n time: " + now +
                "\r\n timestamp: " + ts3 + " " + ts1 +
                "\n zone:" + zone.getID() + " " + zone.getRawOffset();
        return (ret);
    }
}
