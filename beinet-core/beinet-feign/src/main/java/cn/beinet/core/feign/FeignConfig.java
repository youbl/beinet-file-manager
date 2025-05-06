package cn.beinet.core.feign;

import cn.beinet.core.feign.log.MyFeignLogger;
import feign.Logger;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"cn.beinet"})
public class FeignConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 使用自定义的日志记录器
     */
    @Bean
    MyFeignLogger createMyLogger() {
        return new MyFeignLogger();
    }
}
