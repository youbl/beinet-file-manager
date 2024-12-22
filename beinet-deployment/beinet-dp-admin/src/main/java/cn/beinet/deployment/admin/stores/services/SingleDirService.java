package cn.beinet.deployment.admin.stores.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 子目录提级用的服务类，
 * 遍历某个目录下的所有子目录，如果某个目录下有且只有一个子目录，且没有文件，则把子目录里的内容提取到上级目录，并删除该子目录
 *
 * @author youbl
 * @since 2024/12/18 20:21
 */
@Service
@Getter
@Slf4j
public class SingleDirService {
    private final AtomicInteger successCount = new AtomicInteger(0);
    private long startTime = 0;
    private long endTime = 0;
    private boolean completed = true;

    @Async
    public void scan(String dir) {
        init();
        try {
            findAndExtractFiles(new File(dir));
        } catch (Exception e) {
            log.error("扫描过程失败：", e);
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


    // 遍历目录及其子目录
    private void findAndExtractFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        if (files.length == 1 && files[0].isDirectory()) {
            // 只处理1个目录的场景
            moveSubFilesToTop(files[0], directory);
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                findAndExtractFiles(file);
            }
        }
    }

    // 把指定目录下的所有文件和目录，移动到另一个目录下
    private void moveSubFilesToTop(File sourceDir, File targetDir) {
        try {
            File[] files = sourceDir.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file : files) {
                Path targetPath = Paths.get(targetDir.getAbsolutePath() + "\\" + file.getName());
                Files.move(file.toPath(), targetPath);
            }
            // 移完了，重命名一下
            sourceDir.renameTo(new File(sourceDir.getAbsolutePath() + "-zeroFiles"));
            successCount.incrementAndGet();
        } catch (Exception e) {
            log.error("移动失败: {}", sourceDir, e);
        }
    }

    private void init() {
        this.completed = false;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        successCount.set(0);
    }
}
