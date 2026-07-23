package com.zxl.chatbase.wx.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
public class WxCryptoUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    public static byte[] decryptMedia(byte[] encryptedData, byte[] key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            log.error("AES-128-ECB 媒体文件解密失败", e);
            throw new RuntimeException("媒体文件解密失败", e);
        }
    }

    public static byte[] decryptMedia(byte[] encryptedData, String base64Key) {
        byte[] key = Base64.getDecoder().decode(base64Key);
        return decryptMedia(encryptedData, key);
    }
}
