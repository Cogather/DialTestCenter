/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterChallengeDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterResultDto;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtest.model.DialUser;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.DialUserService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.DualLinkRouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.websocket.Session;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CHAP authentication and session binding service.
 *
 * <p>Handles register_request and register_auth messages.</p>
 *
 * @author g00940940
 * @since 2025-11-04
 */
@Service
public class AuthSessionService {

    private static final Logger logger = LoggerFactory.getLogger(AuthSessionService.class);

    private static final long CHALLENGE_TTL_MILLIS = 120_000L;

    private final Map<String, PendingAuthContext> pendingMap = new ConcurrentHashMap<>();

    @Autowired
    private WssMessageSender wssMessageSender;

    @Autowired
    private DialUserService dialUserService;

    @Autowired
    private ExecutorDao executorDao;

    @Autowired
    private SessionBindingRegistry registry;

    @Autowired
    private DualLinkRouter dualLinkRouter;
    
    private final AtomicInteger challengeIdCounter = new AtomicInteger(1);
    
    /**
     * Handle RegisterRequest (0x01): generate and send challenge (V4 JSON version).
     * 阶段1→2：接收注册请求，生成并发送挑战
     *
     * @param dto     RegisterRequest DTO
     * @param session WebSocket session
     */
    public void handleRegisterRequest(RegisterRequestDto dto, Session session) {
        String hostname = dto.getHostname();
        logger.info("Received RegisterRequest from sessionId={}, hostname={}", 
            session.getId(), hostname);
        
        // Generate challenge
        int challengeId = challengeIdCounter.getAndIncrement();
        byte[] challengeBytes = generateChallenge();
        
        // Store pending context with empty username (will be provided in response)
        pendingMap.put(session.getId(),
            new PendingAuthContext("", hostname, Base64.getEncoder().encodeToString(challengeBytes),
                    Instant.now(), challengeId));
        
        // Send RegisterChallenge (V4 JSON format)
        RegisterChallengeDto challengeDto = new RegisterChallengeDto(challengeId, 
                Base64.getEncoder().encodeToString(challengeBytes));
        wssMessageSender.sendJsonMessage(session.getId(), challengeDto);

        // DEBUG: Log challenge details
        logger.info("Sent RegisterChallenge to sessionId={}, challengeId={}, hostname={}, challengeBytes.length={}",
            session.getId(), challengeId, hostname, challengeBytes.length);
        logger.debug("Challenge bytes (hex): {}", bytesToHexString(challengeBytes));
        logger.debug("Challenge Base64: {}", Base64.getEncoder().encodeToString(challengeBytes));
    }
    
    /**
     * Handle Register-Response (V4 JSON format).
     * V4版本：处理JSON格式的注册响应
     *
     * @param dto     Register Response DTO
     * @param session WebSocket session
     */
    public void handleRegisterResponse(RegisterResponseDto dto, Session session) {
        int challengeId = dto.getChallengeId();
        String username = dto.getUsername();
        String responseHex = dto.getResponse();
        
        logger.info("Received Register-Response from sessionId={}, challengeId={}, username={}, response={}",
                session.getId(), challengeId, username, responseHex);
        
        // Verify pending context
        PendingAuthContext ctx = pendingMap.get(session.getId());
        if (ctx == null || isExpired(ctx)) {
            sendRegisterResultV4(session.getId(), 1, "Challenge expired or not found", null);
            logger.warn("Auth failed: challenge missing or expired, sessionId={}", session.getId());
            return;
        }
        
        if (ctx.challengeId != challengeId) {
            sendRegisterResultV4(session.getId(), 2, "Challenge ID mismatch", null);
            logger.warn("Auth failed: challenge ID mismatch, sessionId={}", session.getId());
            return;
        }
        
        // Query user from dial_users table
        DialUser user = dialUserService.findByUsername(username);
        if (user == null) {
            sendRegisterResultV4(session.getId(), 3, "User not found", null);
            logger.warn("Auth failed: user not found, username={}", username);
            return;
        }
        
        // Verify CHAP response using SHA256
        byte[] expectedResponse = computeChapResponseSha256(user.getPassword(), ctx.challenge);
        String expectedHex = bytesToHexString(expectedResponse);
        
        logger.debug("Auth verification: username={}, expectedHex={}, actualHex={}",
                username, expectedHex, responseHex);
        
        if (!expectedHex.equalsIgnoreCase(responseHex)) {
            sendRegisterResultV4(session.getId(), 4, "Authentication failed", null);
            logger.warn("Auth failed: incorrect response, username={}", username);
            return;
        }
        
        // Generate token
        long token = generateTokenV3();
        
        // Update executor database
        try {
            executorDao.saveOrUpdateExecutor(ctx.executorName, token, "ONLINE");
            logger.info("Executor registered successfully: hostname={}, token={}", ctx.executorName, token);
        } catch (IllegalArgumentException e) {
            sendRegisterResultV4(session.getId(), 5, "Database error: " + e.getMessage(), null);
            logger.error("Failed to update executor in database, hostname={}", ctx.executorName, e);
            return;
        }
        
        // Send Register-Result - Success (BEFORE binding token, so it sends directly)
        sendRegisterResultV4(session.getId(), 0, "Authentication successful", token);
        logger.debug("Register-Result sent to sessionId={}, token={}", session.getId(), token);
        
        // Bind session to executor
        registry.bind(session.getId(), ctx.executorName, token);
        
        // V5: Bind control link to DualLinkRouter with token (AFTER sending Register-Result)
        String tokenStr = String.valueOf(token);
        dualLinkRouter.bindControlLinkWithToken(tokenStr, session.getId(), session);
        logger.info("Control link bound with token in DualLinkRouter: sessionId={}, token={}", 
                session.getId(), tokenStr);
        
        // Cleanup
        pendingMap.remove(session.getId());
        
        logger.info("Authentication successful: sessionId={}, hostname={}, username={}, token={}", 
                session.getId(), ctx.executorName, username, token);
    }

    private static String getText(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText("") : "";
    }

    private static byte[] generateChallenge() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private boolean isExpired(PendingAuthContext ctx) {
        return ctx.createdAt.plusMillis(CHALLENGE_TTL_MILLIS).isBefore(Instant.now());
    }
    
    /**
     * Convert hex string to byte array.
     *
     * @param hexString Hex string (e.g., "a1b2c3")
     * @return Byte array
     */
    private static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string: " + hexString);
        }
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < hexString.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }
    
    /**
     * Convert byte array to hex string.
     *
     * @param bytes Byte array
     * @return Hex string (lowercase)
     */
    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            } else {
                // no-op
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Send Register-Result (0x04).
     *
     * @param sessionId   session ID
     * @param resultCode  result code (0=success, non-zero=failure)
     * @param description description
     * @param token       token (null if failed)
     */
    /**
     * Send Register-Result (V4 JSON format).
     * V4版本：发送JSON格式的注册结果
     */
    private void sendRegisterResultV4(String sessionId, int resultCode, String description, Long token) {
        RegisterResultDto resultDto = new RegisterResultDto(resultCode, description, token);
        wssMessageSender.sendJsonMessage(sessionId, resultDto);
    }
    
    /**
     * Compute CHAP response: SHA256(StoredHashBytes + ChallengeBytes)
     *
     * @param storedHashHex SHA256 hash stored in database (hex string, 64 characters)
     * @param challengeBase64 challenge bytes (Base64 encoded)
     * @return SHA256 response (32 bytes)
     */
    private byte[] computeChapResponseSha256(String storedHashHex, String challengeBase64) {
        try {
            byte[] hashBytes = hexStringToBytes(storedHashHex);
            byte[] challengeBytes = Base64.getDecoder().decode(challengeBase64);

            logger.debug("CHAP calculation (SHA256): hashBytes.length={}, challengeBytes.length={}", 
                    hashBytes.length, challengeBytes.length);
            logger.debug("Hash bytes (hex): {}", bytesToHexString(hashBytes));
            logger.debug("Challenge bytes (hex): {}", bytesToHexString(challengeBytes));

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(hashBytes);
            digest.update(challengeBytes);
            byte[] result = digest.digest();

            logger.debug("CHAP result (hex): {}", bytesToHexString(result));
            return result;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to compute CHAP response", e);
            return new byte[32];
        }
    }
    
    /**
     * Generate 8-byte token.
     *
     * @return token as long
     */
    private long generateTokenV3() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        long token = 0L;
        for (int i = 0; i < 8; i++) {
            token = (token << 8) | (bytes[i] & 0xFF);
        }
        return token;
    }
    
    private static class PendingAuthContext {
        private final String username;
        private final String executorName;
        private final String challenge;
        private final Instant createdAt;
        private final int challengeId;

        
        private PendingAuthContext(String username, String executorName, String challenge, Instant createdAt,
                int challengeId) {
            this.username = username;
            this.executorName = executorName;
            this.challenge = challenge;
            this.createdAt = createdAt;
            this.challengeId = challengeId;
        }
    }
}



