/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import static org.junit.Assert.*;

/**
 * SHA256 Hash工具类单元测试
 * 测试目标：验证SHA256哈希生成、格式验证等核心功能
 *
 * @author g00940940
 * @since 2025-11-19
 */
public class Sha256HashUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(Sha256HashUtilTest.class);

    /**
     * 测试SHA256 Hash生成 - 涵盖成功和异常场景
     * 验证：正常密码生成、空密码异常、null密码异常
     */
    @Test
    public void testToSha256Hash_AllScenarios_CorrectResults() {
        logger.debug("Testing SHA256 Hash generation - all scenarios");

        // 场景1：正常密码生成Hash
        String password = "Password123";
        String hash = Sha256HashUtil.toSha256Hash(password);
        assertNotNull("Hash should not be null", hash);
        assertEquals("Hash should be 64 characters", 64, hash.length());
        assertTrue("Hash should be uppercase hex", hash.matches("^[0-9A-F]{64}$"));

        // 场景2：相同密码生成相同Hash（确定性验证）
        String hash2 = Sha256HashUtil.toSha256Hash(password);
        assertEquals("Same password should generate same hash", hash, hash2);

        // 场景3：空密码抛出异常
        try {
            Sha256HashUtil.toSha256Hash("");
            fail("Should throw exception for empty password");
        } catch (IllegalArgumentException e) {
            assertEquals("Plain password cannot be null or empty", e.getMessage());
        }

        // 场景4：null密码抛出异常
        try {
            Sha256HashUtil.toSha256Hash(null);
            fail("Should throw exception for null password");
        } catch (IllegalArgumentException e) {
            assertEquals("Plain password cannot be null or empty", e.getMessage());
        }

        logger.debug("All SHA256 Hash generation scenarios passed");
    }

    /**
     * 测试字节转换和Hash验证 - 涵盖所有辅助方法
     * 验证：bytesToHexString、isValidSha256Hash
     */
    @Test
    public void testUtilityMethods_AllScenarios_CorrectResults() {
        logger.debug("Testing utility methods - all scenarios");

        // bytesToHexString：正常转换
        byte[] bytes = {0x01, 0x23, (byte) 0xAB, (byte) 0xCD};
        String hex = Sha256HashUtil.bytesToHexString(bytes);
        assertEquals("Hex conversion should match", "0123ABCD", hex);

        // bytesToHexString：空数组
        assertEquals("Empty bytes should return empty string", 
            "", Sha256HashUtil.bytesToHexString(new byte[0]));

        // bytesToHexString：null
        assertEquals("Null bytes should return empty string", 
            "", Sha256HashUtil.bytesToHexString(null));

        // isValidSha256Hash：有效Hash（64位16进制）
        String validHash = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
        assertTrue("Valid hash should pass", Sha256HashUtil.isValidSha256Hash(validHash));

        // isValidSha256Hash：小写也有效
        assertTrue("Lowercase hash should be valid",
            Sha256HashUtil.isValidSha256Hash(validHash.toLowerCase(Locale.ROOT)));

        // isValidSha256Hash：长度不足
        assertFalse("Short hash should be invalid", 
            Sha256HashUtil.isValidSha256Hash("E3B0C44298FC1C14"));

        // isValidSha256Hash：非16进制字符
        assertFalse("Non-hex hash should be invalid", 
            Sha256HashUtil.isValidSha256Hash("G3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855"));

        // isValidSha256Hash：null
        assertFalse("Null hash should be invalid", 
            Sha256HashUtil.isValidSha256Hash(null));

        // isValidSha256Hash：空字符串
        assertFalse("Empty hash should be invalid", 
            Sha256HashUtil.isValidSha256Hash(""));

        logger.debug("All utility methods scenarios passed");
    }
}

