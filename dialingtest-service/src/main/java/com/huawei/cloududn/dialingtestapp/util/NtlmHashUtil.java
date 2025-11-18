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
 * NTLM Hash工具类
 * 用于将明文密码转换为NTLM Hash格式，支持执行机CHAP认证
 *
 * <p><b>安全说明：</b></p>
 * <p>本工具类使用MD4算法生成NTLM Hash，这是为了符合微软Windows NTLM认证协议的标准规范。
 * NTLM协议明确规定必须使用MD4算法，这是协议兼容性的硬性要求，而非安全设计缺陷。
 * 虽然MD4算法已被证明存在安全漏洞，但为了与Windows系统互操作，必须遵循协议规范。</p>
 *
 * <p><b>使用场景：</b></p>
 * <ul>
 *   <li>与Windows系统或NTLM兼容系统进行身份认证</li>
 *   <li>执行机CHAP认证（Challenge-Handshake Authentication Protocol）</li>
 * </ul>
 *
 * <p><b>安全建议：</b></p>
 * <ul>
 *   <li>NTLM协议本身存在安全限制，建议仅在必要时使用</li>
 *   <li>在条件允许时，应迁移到更安全的认证方式（如Kerberos、OAuth2、SAML等）</li>
 *   <li>确保NTLM认证仅在受信任的网络环境中使用</li>
 *   <li>建议使用NTLMv2而非NTLMv1（本工具类生成的是NTLMv1 Hash）</li>
 * </ul>
 *
 * @author g00940940
 * @since 2025-11-12
 */
public class NtlmHashUtil {
    private static final Logger logger = LoggerFactory.getLogger(NtlmHashUtil.class);
    
    /**
     * 将明文密码转换为NTLM Hash
     * 算法流程：
     * 1. 将明文密码转换为UTF-16LE编码的字节数组
     * 2. 使用MD4算法计算哈希值
     * 3. 将哈希值转换为32位16进制字符串（大写）
     *
     * @param plainPassword 明文密码，如 "QAZ!q123"
     * @return NTLM Hash，如 "08A1D3438D7DFE8A2DDC9BBBCB05A0D0"
     * @throws IllegalArgumentException 如果密码为空
     * @throws IllegalStateException 如果MD4算法不可用
     */
    public static String toNtlmHash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            logger.error("Plain password cannot be null or empty");
            throw new IllegalArgumentException("Plain password cannot be null or empty");
        }
        
        try {
            // 1. Unicode编码 (UTF-16LE)
            byte[] unicodeBytes = plainPassword.getBytes(StandardCharsets.UTF_16LE);
            logger.debug("Converting password to NTLM Hash, unicodeBytes.length={}", unicodeBytes.length);
            
            // 2. MD4加密
            MessageDigest md4 = getMd4MessageDigest();
            byte[] hashBytes = md4.digest(unicodeBytes);
            
            // 3. 转16进制字符串（大写）
            String ntlmHash = bytesToHexString(hashBytes);
            logger.debug("NTLM Hash generated successfully, length={}", ntlmHash.length());
            
            return ntlmHash;
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD4 algorithm not available", e);
            throw new IllegalStateException("NTLM Hash generation failed: MD4 algorithm not available", e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid password format", e);
            throw new IllegalStateException("NTLM Hash generation failed: Invalid password format", e);
        }
    }
    
    /**
     * 获取MD4 MessageDigest实例
     * 优先尝试使用标准名称"MD4"，如果不支持则尝试使用BouncyCastle
     *
     * <p><b>安全说明：</b></p>
     * <p>MD4算法已被证明存在安全漏洞，不应在新的安全关键场景中使用。
     * 但本工具类必须使用MD4是因为NTLM协议的标准规范强制要求使用MD4算法。
     * NTLM是微软Windows定义的认证协议（RFC规范），为了与Windows系统互操作，
     * 必须严格遵循协议规范使用MD4算法。这是协议兼容性要求，非安全设计缺陷。</p>
     *
     * <p><b>建议：</b>在条件允许时，应迁移到更安全的认证方式（如Kerberos、OAuth2等）</p>
     *
     * @return MD4 MessageDigest实例
     * @throws NoSuchAlgorithmException 如果MD4算法不可用
     */
    @SuppressWarnings("java:S4790")  // 抑制弱加密算法告警：NTLM协议强制要求使用MD4
    private static MessageDigest getMd4MessageDigest() throws NoSuchAlgorithmException {
        try {
            // 尝试使用标准MD4算法
            // 注意：此处必须使用MD4以符合NTLM协议规范
            return MessageDigest.getInstance("MD4");
        } catch (NoSuchAlgorithmException e) {
            logger.warn("MD4 algorithm not available via standard API, attempting BouncyCastle");
            
            // 尝试使用BouncyCastle提供商
            try {
                // 动态加载BouncyCastle提供商（如果存在）
                Class<?> providerClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
                java.security.Provider provider =
                        (java.security.Provider) providerClass.getDeclaredConstructor().newInstance();
                return MessageDigest.getInstance("MD4", provider);
            } catch (ClassNotFoundException ex) {
                logger.error("BouncyCastle provider not found in classpath");
                throw new NoSuchAlgorithmException(
                        "MD4 algorithm not available. Please add BouncyCastle dependency.", e);
            } catch (NoSuchAlgorithmException ex) {
                throw e;
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Failed to initialize BouncyCastle provider", ex);
            } catch (IllegalAccessException ex) {
                throw new IllegalArgumentException("Failed to access BouncyCastle provider", ex);
            } catch (SecurityException ex) {
                throw new IllegalArgumentException("Security restriction accessing BouncyCastle provider", ex);
            } catch (InstantiationException ex) {
                throw new IllegalArgumentException("Failed to instantiate BouncyCastle provider", ex);
            } catch (NoSuchMethodException ex) {
                throw new IllegalArgumentException("BouncyCastle provider constructor not found", ex);
            } catch (java.lang.reflect.InvocationTargetException ex) {
                throw new IllegalArgumentException("Failed to invoke BouncyCastle provider constructor", ex);
            }
        }
    }
    
    /**
     * 将字节数组转换为16进制字符串（大写）
     *
     * @param bytes 字节数组
     * @return 16进制字符串（大写）
     */
    private static String bytesToHexString(byte[] bytes) {
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
     * 验证字符串是否为有效的NTLM Hash格式
     * 有效格式：32位16进制字符串
     *
     * @param ntlmHash 待验证的字符串
     * @return true表示格式有效，false表示格式无效
     */
    public static boolean isValidNtlmHash(String ntlmHash) {
        if (ntlmHash == null) {
            return false;
        }
        
        // NTLM Hash必须是32位16进制字符串
        if (ntlmHash.length() != 32) {
            return false;
        }
        
        // 检查是否全部为16进制字符
        return ntlmHash.matches("^[0-9A-Fa-f]{32}$");
    }
}

