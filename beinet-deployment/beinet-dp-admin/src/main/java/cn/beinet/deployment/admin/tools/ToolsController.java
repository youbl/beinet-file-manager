package cn.beinet.deployment.admin.tools;

import cn.beinet.core.base.commonDto.ResponseData;
import cn.beinet.deployment.admin.tools.service.YmlEncryptorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 工具类
 * @author youbl
 * @since 2024/12/2 20:38
 */
@Tag(name = "tools", description = "加解密工具类")
@RestController
@RequiredArgsConstructor
public class ToolsController {
    private final YmlEncryptorService ymlEncryptorService;

    @GetMapping("tools/ymlencrypt")
    public ResponseData<String> ymlEncrypt(@NonNull @RequestParam String str) {
        var ret = ymlEncryptorService.ymlEnc(str, false);
        return ResponseData.ok(ret);
    }

    @GetMapping("tools/ymldecrypt")
    public ResponseData<String> ymlDecrypt(@NonNull @RequestParam String str) {
        var ret = ymlEncryptorService.ymlEnc(str, true);
        return ResponseData.ok(ret);
    }


    @GetMapping("tools/md5")
    public ResponseData<String> md5(@RequestParam String str) {
        return ResponseData.ok(md5Hex(str));
    }

    @GetMapping("tools/sha256")
    public ResponseData<String> sha256(@RequestParam String str) {
        return ResponseData.ok(sha256Hex(str));
    }

    private String md5Hex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String sha256Hex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
