package cn.beinet.sdk.event;

import cn.beinet.sdk.event.enums.EventSubType;
import cn.beinet.sdk.event.service.EventFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 事件上传辅助类
 * @author youbl
 * @since 2024/12/13 14:40
 */
@Component
@Slf4j
public class EventUtils {
    // 单例
    private static EventUtils instance;

    private final EventFactory factory;

    public EventUtils(EventFactory eventFactory) {
        instance = this;
        this.factory = eventFactory;
    }

    /**
     * 事件上报
     * @param subType 事件子类型
     * @param data 事件数据
     */
    public static void report(EventSubType subType, Object data) {
        if (instance != null) {
            instance.reportEvent(subType, data);
        }
    }

    /**
     * 事件上报
     * @param subType 事件子类型
     * @param data 事件数据
     */
    @Async
    public void reportEvent(EventSubType subType, Object data) {
        try {
            factory.reportEvent(subType, data);
        } catch (Exception e) {
            log.error("事件上传出错:{} {}", subType, data, e);
        }
    }
}
