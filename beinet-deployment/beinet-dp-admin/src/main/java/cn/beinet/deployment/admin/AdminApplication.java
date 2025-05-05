package cn.beinet.deployment.admin;

import cn.beinet.core.base.configs.SystemConst;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 运维后台主运行类
 *
 * @author youbl
 * @since 2024/11/19 10:45
 */
@SpringBootApplication(scanBasePackages = "cn.beinet")
@EnableAsync
@MapperScan({"cn.beinet.**.dal"})
@EnableScheduling
public class AdminApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // 输出 D:\mine\beinet-file-manager, 如果使用java -jar方法，则会得到启动目录，比如用服务启动就是java.exe目录，如 C:\jdk-21.0.3\bin
        // 如果是在cmd里启动，那就是cmd窗口显示的那个目录
        System.out.println("1.--- " + System.getProperty("user.dir"));

        // 输出 C:\Users\youbl，如果以服务启动，会输出：C:\Windows\system32\config\systemprofile
        System.out.println("2.--- " + System.getProperty("user.home"));

        // 获取 jar 文件所在目录
        String path = this.getClass().getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        // 解码 URL 编码的路径
        path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        File jarFile = new File(path);
        System.out.println("3.--- " + jarFile.getAbsolutePath());

        System.out.println("4.--- " + SystemConst.getBaseDir());
    }
}
