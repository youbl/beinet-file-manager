package cn.beinet.deployment.admin.stores.jobs;

import cn.beinet.core.utils.FileHelper;
import cn.beinet.deployment.admin.stores.configs.RealConfigVar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 新类
 * @author youbl
 * @since 2024/12/17 19:27
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreJobs {

    // 每600秒（10分钟）执行一次磁盘分区文件读写，避免磁盘睡着
    // 设置Windows电源里的睡眠时间为0好像没效果
    @Scheduled(fixedRate = 600000)
    public void activeDisks() {
        List<String> partitions = RealConfigVar.activeDisks();
        for (String partition : partitions) {
            activeDisksDo(partition);
        }
    }

    private void activeDisksDo(String partition) {
        try {
            String ts = new SimpleDateFormat("yyyyMMdd HHmmss.SSS").format(new Date()) + System.lineSeparator();
            String file = partition + "beinet.txt";
            FileHelper.saveFile(file, ts, false);
        } catch (Exception e) {
            log.error("写入失败:{}", partition, e);
        }
    }
}
