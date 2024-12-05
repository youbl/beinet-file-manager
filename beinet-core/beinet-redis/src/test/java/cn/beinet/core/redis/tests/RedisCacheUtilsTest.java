package cn.beinet.core.redis.tests;

import cn.beinet.core.redis.RedisCacheUtils;
import cn.beinet.core.redis.UtilsTestApplication;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = UtilsTestApplication.class)
@RunWith(SpringRunner.class)
public class RedisCacheUtilsTest {
    @Test
    @SneakyThrows
    public void testDemo() {
        var cacheKey = "abcccd";

        // 确保Redis没值，有值会影响下面的测试结果
        var str = RedisCacheUtils.get(cacheKey);
        Assert.assertNull(str);

        // 首次请求，缓存应该不存在，并执行getThirdVal方法
        var map = RedisCacheUtils.get(cacheKey, Map.class, 5, this::getThirdVal);
        Assert.assertTrue(map != null && map.size() == 3);
        System.out.println(map.toString());

        // 缓存应该存在，并会不执行getOneVal方法，所以结果还是上一次的3个值
        map = RedisCacheUtils.get(cacheKey, Map.class, 5, this::getOneVal);
        Assert.assertTrue(map != null && map.size() == 3);
        System.out.println(map.toString());

        Thread.sleep(5000);
        // 等5秒，缓存应该过期了，会执行getOneVal方法，所以结果是一个值
        map = RedisCacheUtils.get(cacheKey, Map.class, 5, this::getOneVal);
        Assert.assertTrue(map != null && map.size() == 1);
        System.out.println(map.toString());
    }

    private Map<String, String> getThirdVal() {
        System.out.println("getThirdVal 执行了");
        var mapInner = new HashMap<String, String>();
        mapInner.put("key1", "value1");
        mapInner.put("key2", "value2");
        mapInner.put("key3", "value3");
        return mapInner;
    }

    private Map<String, String> getOneVal() {
        System.out.println("getOneVal 执行了");
        var mapInner = new HashMap<String, String>();
        mapInner.put("onekey1", "onevalue1");
        return mapInner;
    }
}