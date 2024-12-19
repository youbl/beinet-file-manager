package cn.beinet.deployment.admin.stores.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 解压用的服务类
 * @author youbl
 * @since 2024/12/18 20:21
 */
@Service
@Getter
@Slf4j
public class UnRarService {
    private final static String rarExe = "\"c:\\Program Files\\WinRAR\\Rar.exe\"";
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private long startTime = 0;
    private long endTime = 0;
    private boolean completed = true;

    @Async
    public void unrar(String dir, String pwd) {
        init();
        try {
            findAndExtractRarFiles(new File(dir), pwd);
        } catch (Exception e) {
            log.error("解压过程失败：", e);
        } finally {
            completed = true;
            this.endTime = System.currentTimeMillis();
        }
    }

    public long costTime() {
        if (this.startTime == 0) {
            return 0;
        }
        if (this.endTime > 0) {
            return this.endTime - this.startTime;
        }
        return System.currentTimeMillis() - this.startTime;
    }


    // 遍历目录及其子目录，找到 RAR 文件并解压
    private void findAndExtractRarFiles(File directory, String pwd) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findAndExtractRarFiles(file, pwd); // 递归查找子目录
                } else if (file.getName().endsWith(".rar")) {
                    try {
                        if (extractRarFile(file, pwd)) {
                            successCount.incrementAndGet();
                            // 成功时，删除文件
                            file.delete();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet(); // 失败计数
                        log.error("解压文件失败: {}", file.getAbsolutePath(), e);
                    }
                }
            }
        }
    }

    // 解压单个 RAR 文件
    private boolean extractRarFile(File rarFile, String pwd) {
        String rarFileName = rarFile.getName();
        String subDirName = rarFileName.substring(0, rarFileName.lastIndexOf('.'));
        File outputDir = new File(rarFile.getParent(), subDirName);
        outputDir.mkdir(); // 创建以 RAR 文件名命名的子目录

        // 解压 RAR 文件到指定目录, x指解压 -o+指覆盖 -p解压密码
        String command = getRarExe() + " x -o+ -p" + pwd +
                " \"" + rarFile.getAbsolutePath() + "\" \"" + outputDir.getAbsolutePath() + "\\\"";
        StringBuilder output = new StringBuilder("Executing command: ");
        output.append(command).append(System.lineSeparator());
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor(); // 等待解压完成

            // 读取命令行输出，指定字符编码为 UTF-8
            Charset gbk = Charset.forName("GBK");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), gbk))) {
                String line;
                output.append("*****Command output*****").append(System.lineSeparator());
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            // 读取错误流
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), gbk))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append(System.lineSeparator());
                }
            }
            output.append(System.lineSeparator())
                    .append("exit code: ")
                    .append(process.exitValue())
                    .append(System.lineSeparator());
            if (!errorOutput.isEmpty()) {
                output.append("*****Command error*****")
                        .append(System.lineSeparator())
                        .append(errorOutput);
            }

            log.info(output.toString().replaceAll("[\\r\\n]+", System.lineSeparator())); // 一次性记录所有命令行输出

            return (process.exitValue() == 0);
        } catch (IOException | InterruptedException e) {
            log.error("解压文件失败: {}", output, e);
            return false;
        }
    }

    private void init() {
        this.completed = false;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        successCount.set(0);
        failureCount.set(0);
    }

    private String getRarExe() {
        return rarExe;
    }
}
