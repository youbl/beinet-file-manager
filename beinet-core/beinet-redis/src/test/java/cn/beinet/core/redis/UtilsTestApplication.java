package cn.beinet.core.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用于单元测试的主类
 * @author youbl
 * @since 2024/11/20 20:01
 */
@SpringBootApplication(scanBasePackages = "cn.beinet")
//@EnableFeignClients(basePackages = {"cn.beinet"})
//@MapperScan({"cn.beinet.**.dal"})
public class UtilsTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(UtilsTestApplication.class, args);
    }
}