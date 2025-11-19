/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA256 Hash工具类
 * 用于替代旧的NTLM Hash，提供更安全的密码存储和认证计算
 *
 * @author g00940940
 * @since 2025-11-19
 */
public class Sha256HashUtil {
    private static final Logger logger = LoggerFactory.getLogger(Sha256HashUtil.class);

    /**
     * 将明文密码转换为 SHA256 Hash
     *
     * @param plainPassword 明文密码
     * @return SHA256 Hash (64位 16进制大写字符串)
     * @throws IllegalArgumentException 如果密码为空
     * @throws IllegalStateException 如果SHA-256算法不可用
     */
    public static String toSha256Hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            logger.error("Plain password cannot be null or empty");
            throw new IllegalArgumentException("Plain password cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            String hash = bytesToHexString(hashBytes);
            logger.debug("SHA256 Hash generated successfully, length={}", hash.length());
            return hash;
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 将字节数组转换为16进制字符串（大写）
     *
     * @param bytes 字节数组
     * @return 16进制字符串（大写）
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    /**
     * 验证字符串是否为有效的 SHA256 Hash格式
     * 有效格式：64位16进制字符串
     *
     * @param hash 待验证的字符串
     * @return true表示格式有效，false表示格式无效
     */
    public static boolean isValidSha256Hash(String hash) {
        if (hash == null) {
            return false;
        }

        if (hash.length() != 64) {
            return false;
        }

        return hash.matches("^[0-9A-Fa-f]{64}$");
    }
}

