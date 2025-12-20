package cn.beinet.business.login.service;

import cn.beinet.core.utils.TokenHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 
 * @author youbl
 * @since 2024-12-20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${login.secret:beinet.cn.file}")
    private String secret;

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    /**
     * 将Token添加到黑名单
     * 
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        try {
            // 获取Token的剩余有效期
            long remainingTtl = TokenHelper.getRemainingTtl(token, secret);
            
            if (remainingTtl > 0) {
                String tokenHash = hashToken(token);
                String key = BLACKLIST_PREFIX + tokenHash;
                
                // 将Token哈希值存入Redis，TTL设置为Token的剩余有效期
                redisTemplate.opsForValue().set(key, "1", remainingTtl, TimeUnit.SECONDS);
                
                log.debug("Token已添加到黑名单: hash={}, ttl={}秒", tokenHash, remainingTtl);
            }
        } catch (Exception e) {
            log.error("添加Token到黑名单失败: token={}", token, e);
        }
    }

    /**
     * 检查Token是否在黑名单中
     * 
     * @param token JWT Token
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }

        try {
            String tokenHash = hashToken(token);
            String key = BLACKLIST_PREFIX + tokenHash;
            
            Boolean exists = redisTemplate.hasKey(key);
            boolean isBlacklisted = Boolean.TRUE.equals(exists);
            
            if (isBlacklisted) {
                log.debug("Token在黑名单中: hash={}", tokenHash);
            }
            
            return isBlacklisted;
        } catch (Exception e) {
            log.error("检查Token黑名单状态失败: token={}", token, e);
            // 出错时为了安全起见，认为Token有效
            return false;
        }
    }

    /**
     * 从黑名单中移除Token（一般不需要，Token过期会自动清理）
     * 
     * @param token JWT Token
     */
    public void removeFromBlacklist(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }

        try {
            String tokenHash = hashToken(token);
            String key = BLACKLIST_PREFIX + tokenHash;
            
            redisTemplate.delete(key);
            log.debug("Token已从黑名单移除: hash={}", tokenHash);
        } catch (Exception e) {
            log.error("从黑名单移除Token失败: token={}", token, e);
        }
    }

    /**
     * 清理过期的黑名单条目（Redis会自动清理，这个方法主要用于统计）
     * 
     * @return 清理的条目数
     */
    public long cleanupExpiredTokens() {
        try {
            // 获取所有黑名单键
            var keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            long beforeCount = keys.size();
            
            // Redis会自动清理过期的键，这里只是统计
            keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            long afterCount = keys != null ? keys.size() : 0;
            
            long cleanedCount = beforeCount - afterCount;
            if (cleanedCount > 0) {
                log.info("清理了 {} 个过期的黑名单Token", cleanedCount);
            }
            
            return cleanedCount;
        } catch (Exception e) {
            log.error("清理过期黑名单Token失败", e);
            return 0;
        }
    }

    /**
     * 获取黑名单中的Token数量
     * 
     * @return Token数量
     */
    public long getBlacklistSize() {
        try {
            var keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("获取黑名单大小失败", e);
            return 0;
        }
    }

    /**
     * 清空所有黑名单Token（谨慎使用）
     */
    public void clearAllBlacklist() {
        try {
            var keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.warn("已清空所有黑名单Token，数量: {}", keys.size());
            }
        } catch (Exception e) {
            log.error("清空黑名单失败", e);
        }
    }

    /**
     * 对Token进行哈希处理
     * 
     * @param token 原始Token
     * @return 哈希值
     */
    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(token.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Token哈希计算失败", e);
            // 降级方案：使用Token的hashCode
            return String.valueOf(token.hashCode());
        }
    }
}