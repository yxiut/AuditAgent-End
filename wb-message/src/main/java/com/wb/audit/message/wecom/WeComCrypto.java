package com.wb.audit.message.wecom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企微消息加解密（WXBizMsgCrypt 核心算法，纯 JDK 实现）。
 * 用途：接收消息服务器 URL 验证（echostr 解密回显）与消息/事件解密。
 * 算法：SHA1 签名 + AES-256-CBC + PKCS7(块32) + Base64。
 */
public final class WeComCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    private WeComCrypto() {
    }

    /**
     * 校验签名：SHA1(sort(token, timestamp, nonce, encrypt)) == msgSignature
     */
    public static boolean verifySignature(String token, String timestamp, String nonce,
                                          String encrypt, String msgSignature) throws Exception {
        String sorted = sort(token, timestamp, nonce, encrypt);
        return sha1(sorted).equals(msgSignature);
    }

    /**
     * 解密：返回明文消息与接收方ID（CorpID）。
     * 明文结构：16字节random + 4字节msg_len(大端) + msg + receiveId
     */
    public static DecryptResult decrypt(String encodingAesKey, String encrypted) throws Exception {
        byte[] aesKey = base64DecodeKey(encodingAesKey);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"),
                new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
        byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        // 去 PKCS7 padding
        int pad = plainBytes.length == 0 ? 0 : plainBytes[plainBytes.length - 1];
        if (pad < 1 || pad > 32 || pad > plainBytes.length) {
            pad = 0;
        }
        byte[] unpadded = Arrays.copyOfRange(plainBytes, 0, plainBytes.length - pad);
        if (unpadded.length < 20) {
            throw new IllegalArgumentException("解密结果长度非法");
        }
        int msgLen = ((unpadded[16] & 0xFF) << 24) | ((unpadded[17] & 0xFF) << 16)
                | ((unpadded[18] & 0xFF) << 8) | (unpadded[19] & 0xFF);
        if (msgLen < 0 || 20 + msgLen > unpadded.length) {
            throw new IllegalArgumentException("解密消息长度非法: " + msgLen);
        }
        String message = new String(unpadded, 20, msgLen, StandardCharsets.UTF_8);
        String receiveId = new String(unpadded, 20 + msgLen, unpadded.length - 20 - msgLen, StandardCharsets.UTF_8);
        return new DecryptResult(message, receiveId);
    }

    /**
     * 加密（生成 echostr / 被动回复用；也用于本地自测）
     */
    public static String encrypt(String encodingAesKey, String message, String receiveId) throws Exception {
        byte[] aesKey = base64DecodeKey(encodingAesKey);
        byte[] random = new byte[16];
        RANDOM.nextBytes(random);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(random);
        bos.write(intToBytes(message.getBytes(StandardCharsets.UTF_8).length));
        bos.write(message.getBytes(StandardCharsets.UTF_8));
        bos.write(receiveId.getBytes(StandardCharsets.UTF_8));
        byte[] data = bos.toByteArray();
        // PKCS7 补齐到 32 字节块
        int padLen = 32 - (data.length % 32);
        byte[] padded = Arrays.copyOf(data, data.length + padLen);
        Arrays.fill(padded, data.length, padded.length, (byte) padLen);
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"),
                new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
        return Base64.getEncoder().encodeToString(cipher.doFinal(padded));
    }

    private static byte[] base64DecodeKey(String encodingAesKey) {
        String key = encodingAesKey == null ? "" : encodingAesKey.trim();
        if (key.length() != 43) {
            throw new IllegalArgumentException("EncodingAESKey 长度必须为 43");
        }
        return Base64.getDecoder().decode(key + "=");
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{(byte) ((value >> 24) & 0xFF), (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF)};
    }

    private static String sort(String token, String timestamp, String nonce, String encrypt) {
        String[] arr = {token, timestamp, nonce, encrypt};
        Arrays.sort(arr);
        StringBuilder sb = new StringBuilder();
        for (String s : arr) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static String sha1(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** 解密结果 */
    public static final class DecryptResult {
        private final String message;
        private final String receiveId;

        public DecryptResult(String message, String receiveId) {
            this.message = message;
            this.receiveId = receiveId;
        }

        public String getMessage() {
            return message;
        }

        public String getReceiveId() {
            return receiveId;
        }
    }
}