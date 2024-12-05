package cn.beinet.core.redis;

import cn.beinet.core.utils.SpringHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import lombok.SneakyThrows;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class RedisCacheUtils {

    private static final String KEY_PREFIX = ":";

    private static GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer;

    /**
     * 返回指定key的值
     * @param key key
     * @return 值或null
     */
    public static String get(String key) {
        return getRedisTemplate().opsForValue().get(key);
    }

    /**
     * 从Redis中读取指定key的值并返回，
     * key不存在时，执行方法并填充Redis，再返回
     *
     * @param key redis的key
     * @param timeoutSeconds noCacheMethod的值写入redis的缓存时长，小于等于0时表示永久缓存
     * @param noCacheMethod redis不存在时，要执行的获取值的方法
     * @return 值
     */
    @SneakyThrows
    public static <V> V get(String key, Class<V> clazz, long timeoutSeconds, Callable<V> noCacheMethod) {
        StringRedisTemplate redis = getRedisTemplate();
        ObjectMapper mapper = SpringHelper.getBean(ObjectMapper.class);

        String val = redis.opsForValue().get(key);
        if (val != null) {
            return mapper.readValue(val, clazz);
        }
        if (noCacheMethod != null) {
            V obj = noCacheMethod.call();
            if (obj == null) {
                return null;
            }
            val = mapper.writeValueAsString(obj);
            if (timeoutSeconds > 0) {
                redis.opsForValue().set(key, val, timeoutSeconds, TimeUnit.SECONDS);
            } else {
                redis.opsForValue().set(key, val);
            }
            return obj;
        }
        return null;
    }

    public static StringRedisTemplate getRedisTemplate() {
        return SpringHelper.getBean(StringRedisTemplate.class);
    }

    public static RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1))
//                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(genericJackson2JsonRedisSerializer()))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .computePrefixWith(cacheName -> cacheName + KEY_PREFIX);
    }

    public static GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer() {
        if (genericJackson2JsonRedisSerializer != null) {
            return genericJackson2JsonRedisSerializer;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        //日期序列化
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        //日期反序列化
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        genericJackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        return genericJackson2JsonRedisSerializer;
    }
}
