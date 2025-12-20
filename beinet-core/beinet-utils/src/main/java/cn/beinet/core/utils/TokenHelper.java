package cn.beinet.core.utils;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * jwt token辅助类
 * @author youbl
 * @since 2025/5/27 20:26
 */
public class TokenHelper {
    public static final String USER_ID = "user-id";
    public static final String USER_NAME = "user-name";
    public static final String USER_ACCOUNT = "user-account";
    public static final String USER_TYPE = "user-type";

    /**
     * 签发一个JWT
     * @param account 登录账号
     * @param secret 密钥
     * @param expireSecond 过期时间
     * @return jwt
     */
    public static String signJwt(String account,
                                 String secret,
                                 long expireSecond) {
        return signJwt(account, UserType.USER, secret, expireSecond);
    }

    /**
     * 签发一个JWT
     * @param account 登录账号
     * @param type 用户类型
     * @param secret 密钥
     * @param expireSecond 过期时间
     * @return jwt
     */
    public static String signJwt(String account,
                                 UserType type,
                                 String secret,
                                 long expireSecond) {
        long now = System.currentTimeMillis();
        long expired = now + expireSecond * 1000L;
        return JWT.create()
                //.setPayload(USER_ID, id)
                .setPayload(USER_ACCOUNT, account)
                //.setPayload(USER_NAME, name)
                .setPayload(USER_TYPE, type.name())
                .setIssuedAt(new Date(now))
                .setNotBefore(new Date(now))
                .setExpiresAt(new Date(expired))
                .setKey(secret.getBytes())
                .sign();
    }

    public static String getAccountFromJwt(String token, String secret) {
        if (!StringUtils.hasLength(token)) {
            return null;
        }
        // 验证授权有效性
        JWT jwt;
        try {
            jwt = JWTUtil.parseToken(token);
        } catch (Exception e) {
            return null;
        }

        // 验证token
        if (!JWTUtil.verify(token, secret.getBytes())) {
            return null;
        }

        // 验证授权时效性
        try {
            JWTValidator.of(jwt).validateDate();
        } catch (ValidateException err) {
            return null;
        }

        var account = jwt.getPayload(USER_ACCOUNT);
        if (account == null) {
            return null;
        }
        return account.toString();
    }

    /**
     * 获取Token的剩余有效期（秒）
     * 
     * @param token JWT Token
     * @param secret 密钥
     * @return 剩余有效期（秒），如果Token无效或已过期返回0
     */
    public static long getRemainingTtl(String token, String secret) {
        if (!StringUtils.hasLength(token)) {
            return 0;
        }
        
        try {
            JWT jwt = JWTUtil.parseToken(token);
            
            // 验证token
            if (!JWTUtil.verify(token, secret.getBytes())) {
                return 0;
            }
            
            // 获取过期时间
            Date expireDate = jwt.getPayload().getClaimsJson().getDate("exp");
            if (expireDate == null) {
                return 0;
            }
            
            long now = System.currentTimeMillis();
            long expireTime = expireDate.getTime();
            
            if (expireTime <= now) {
                return 0; // 已过期
            }
            
            return (expireTime - now) / 1000; // 转换为秒
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 用户类型枚举
     */
    public enum UserType {
        ADMIN,
        USER;
    }
}

