package cn.beinet.deployment.admin.tools.service;

import cn.beinet.core.utils.StrHelper;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * yml加解密的类
 * @author youbl
 * @since 2024/12/2 20:43
 */
@Service
public class YmlEncryptorService {
    private final PooledPBEStringEncryptor encryptor;

    public YmlEncryptorService(Environment environment) {
        encryptor = createEncryptor(environment.getProperty("jasypt.encryptor.password"));
    }

    /**
     * yml加解密方法
     * @param str 要加密或解密的字符串
     * @param isDecrypt true为解密，false为加密
     * @return 加解密结果
     */
    public String ymlEnc(String str, boolean isDecrypt) {
        try {
            if (!isDecrypt) {
                // 加密
                return encryptor.encrypt(str);
            }

            // 解密
            str = str.trim();
            str = StrHelper.trim(str, "\"");
            str = StrHelper.trim(str, "&quot;");
            str = StrHelper.trim(str, "ENC(");
            str = StrHelper.trim(str, ")");
            var ret = encryptor.decrypt(str);
            return ret;
        } catch (Exception exp) {
            return "错误：" + exp.getMessage();
        }
    }

    // 创建yml配置加解密使用的jasypt类实例
    private PooledPBEStringEncryptor createEncryptor(String password) {
        // 参考 https://github.com/ulisesbocchio/jasypt-spring-boot
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        // 默认值
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}
