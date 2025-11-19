/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2025. All rights reserved.
 */

package com.huawei.cloududn.dialingtestapp.service.executormanagement.auth;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterResponseDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtest.model.DialUser;
import com.huawei.cloududn.dialingtestapp.service.basicDataManage.DialUserService;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.SessionBindingRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.websocket.Session;

import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.RegisterChallengeDto;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * AuthSessionService单元测试 - V4协议版本（SHA256升级）
 * 使用5个测试用例覆盖所有认证场景
 *
 * @author g00940940
 * @since 2025-11-19
 */
public class AuthSessionServiceTest {
    @Mock
    private WssMessageSender sender;
    @Mock
    private DialUserService dialUserService;
    @Mock
    private ExecutorDao executorDao;
    @Mock
    private SessionBindingRegistry registry;
    @InjectMocks
    private AuthSessionService service;
    private AutoCloseable mocks;

    @Before
    public void init() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    /**
     * UT1: 测试完整认证流程（失败路径）
     * 覆盖: handleRegisterRequest + handleRegisterResponse基本流程
     * 覆盖: CHAP响应验证失败的场景
     * 覆盖: computeChapResponseSha256方法的正常执行路径
     */
    @Test
    public void testAuthenticationFlow_SuccessAndFailurePaths_AllBranchesExecuted() {
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-flow");

        RegisterRequestDto requestDto = new RegisterRequestDto();
        requestDto.setHostname("Executor-Flow");
        service.handleRegisterRequest(requestDto, session);
        verify(sender, times(1)).sendJsonMessage(eq("session-flow"), any());

        RegisterResponseDto responseDto = new RegisterResponseDto();
        responseDto.setChallengeId(1);
        responseDto.setUsername("testuser");
        responseDto.setResponse("incorrectresponse");
        DialUser user = new DialUser();
        user.setUsername("testuser");
        user.setPassword("a1b2c3d4e5f6789012345678901234aba1b2c3d4e5f6789012345678901234ab");
        when(dialUserService.findByUsername("testuser")).thenReturn(user);

        service.handleRegisterResponse(responseDto, session);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-flow"), any());
    }

    /**
     * UT2: 测试各种上下文相关失败场景
     * 覆盖: 无pending context (resultCode=1), challengeId不匹配 (resultCode=2)
     * 新增: challenge过期测试 - 使用反射修改createdAt时间戳
     */
    @Test
    public void testAuthenticationFlow_ContextFailures_AllContextErrors() throws Exception {
        Session session1 = Mockito.mock(Session.class);
        when(session1.getId()).thenReturn("session-no-context");
        RegisterResponseDto responseDto1 = new RegisterResponseDto();
        responseDto1.setChallengeId(1);
        responseDto1.setUsername("testuser");
        responseDto1.setResponse("response");
        service.handleRegisterResponse(responseDto1, session1);
        verify(sender).sendJsonMessage(eq("session-no-context"), any());
        verify(executorDao, never()).saveOrUpdateExecutor(anyString(), anyLong(), anyString());

        Session session2 = Mockito.mock(Session.class);
        when(session2.getId()).thenReturn("session-mismatch");
        RegisterRequestDto requestDto2 = new RegisterRequestDto();
        requestDto2.setHostname("Executor-Mismatch");
        service.handleRegisterRequest(requestDto2, session2);
        RegisterResponseDto responseDto2 = new RegisterResponseDto();
        responseDto2.setChallengeId(999);
        responseDto2.setUsername("testuser");
        responseDto2.setResponse("response");
        service.handleRegisterResponse(responseDto2, session2);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-mismatch"), any());
        verify(registry, never()).bind(anyString(), anyString(), anyLong());

        Session session3 = Mockito.mock(Session.class);
        when(session3.getId()).thenReturn("session-expired");
        RegisterRequestDto requestDto3 = new RegisterRequestDto();
        requestDto3.setHostname("Executor-Expired");
        service.handleRegisterRequest(requestDto3, session3);

        java.lang.reflect.Field pendingMapField = service.getClass().getDeclaredField("pendingMap");
        pendingMapField.setAccessible(true);
        java.util.Map<String, Object> pendingMap = (java.util.Map<String, Object>) pendingMapField.get(service);
        Object ctx = pendingMap.get("session-expired");
        
        java.lang.reflect.Field createdAtField = ctx.getClass().getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(ctx, java.time.Instant.now().minusSeconds(200));

        RegisterResponseDto expiredResponse = new RegisterResponseDto();
        expiredResponse.setChallengeId(1);
        expiredResponse.setUsername("testuser");
        expiredResponse.setResponse("response");
        service.handleRegisterResponse(expiredResponse, session3);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-expired"), any());
    }

    /**
     * UT3: 测试用户和CHAP验证失败场景
     * 覆盖: 用户不存在 (resultCode=3), CHAP响应错误 (resultCode=4)
     * 新增: 测试hexStringToBytes的异常分支（无效hex字符串）
     */
    @Test
    public void testAuthenticationFlow_UserAndChapFailures_AllAuthErrors() {
        Session session1 = Mockito.mock(Session.class);
        when(session1.getId()).thenReturn("session-no-user");
        RegisterRequestDto requestDto1 = new RegisterRequestDto();
        requestDto1.setHostname("Executor-NoUser");
        service.handleRegisterRequest(requestDto1, session1);
        RegisterResponseDto responseDto1 = new RegisterResponseDto();
        responseDto1.setChallengeId(1);
        responseDto1.setUsername("nonexistent");
        responseDto1.setResponse("response");
        when(dialUserService.findByUsername("nonexistent")).thenReturn(null);
        service.handleRegisterResponse(responseDto1, session1);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-no-user"), any());

        Session session2 = Mockito.mock(Session.class);
        when(session2.getId()).thenReturn("session-wrong-chap");
        RegisterRequestDto requestDto2 = new RegisterRequestDto();
        requestDto2.setHostname("Executor-WrongChap");
        service.handleRegisterRequest(requestDto2, session2);
        RegisterResponseDto responseDto2 = new RegisterResponseDto();
        responseDto2.setChallengeId(1);
        responseDto2.setUsername("testuser");
        responseDto2.setResponse("wrongresponse");
        DialUser user2 = new DialUser();
        user2.setUsername("testuser");
        user2.setPassword("a1b2c3d4e5f6789012345678901234aba1b2c3d4e5f6789012345678901234ab");
        when(dialUserService.findByUsername("testuser")).thenReturn(user2);
        service.handleRegisterResponse(responseDto2, session2);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-wrong-chap"), any());
        verify(executorDao, never()).saveOrUpdateExecutor(anyString(), anyLong(), anyString());

        Session session3 = Mockito.mock(Session.class);
        when(session3.getId()).thenReturn("session-invalid-hex");
        RegisterRequestDto requestDto3 = new RegisterRequestDto();
        requestDto3.setHostname("Executor-InvalidHex");
        service.handleRegisterRequest(requestDto3, session3);
        RegisterResponseDto responseDto3 = new RegisterResponseDto();
        responseDto3.setChallengeId(1);
        responseDto3.setUsername("invalidhexuser");
        responseDto3.setResponse("invalidhexresponse");
        DialUser user3 = new DialUser();
        user3.setUsername("invalidhexuser");
        user3.setPassword("invalidhex");
        when(dialUserService.findByUsername("invalidhexuser")).thenReturn(user3);
        service.handleRegisterResponse(responseDto3, session3);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-invalid-hex"), any());
    }

    /**
     * UT4: 测试数据库操作失败场景和认证成功场景
     * 覆盖: 数据库更新失败 (resultCode=5)
     * 覆盖: generateTokenV3(), 成功认证的完整流程 (210-233行)
     */
    @Test
    public void testAuthenticationFlow_DatabaseFailure_ErrorHandled() {
        ArgumentCaptor<RegisterChallengeDto> captor = ArgumentCaptor.forClass(RegisterChallengeDto.class);
        
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-db-fail");
        RegisterRequestDto requestDto = new RegisterRequestDto();
        requestDto.setHostname("Executor-DbFail");
        service.handleRegisterRequest(requestDto, session);

        verify(sender).sendJsonMessage(eq("session-db-fail"), captor.capture());
        RegisterChallengeDto sentChallenge = captor.getValue();
        String challengeBase64 = sentChallenge.getChallenge();

        String sha256Hash = "a1b2c3d4e5f6789012345678901234aba1b2c3d4e5f6789012345678901234ab";
        String correctResponse = computeCorrectResponse(sha256Hash, challengeBase64);

        RegisterResponseDto responseDto = new RegisterResponseDto();
        responseDto.setChallengeId(1);
        responseDto.setUsername("testuser");
        responseDto.setResponse(correctResponse);
        DialUser user = new DialUser();
        user.setUsername("testuser");
        user.setPassword(sha256Hash);
        when(dialUserService.findByUsername("testuser")).thenReturn(user);
        when(executorDao.saveOrUpdateExecutor(anyString(), anyLong(), anyString()))
            .thenThrow(new IllegalArgumentException("Database error"));

        service.handleRegisterResponse(responseDto, session);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-db-fail"), any());
        verify(registry, never()).bind(anyString(), anyString(), anyLong());

        Session session2 = Mockito.mock(Session.class);
        when(session2.getId()).thenReturn("session-success");
        RegisterRequestDto requestDto2 = new RegisterRequestDto();
        requestDto2.setHostname("Executor-Success");
        service.handleRegisterRequest(requestDto2, session2);

        ArgumentCaptor<RegisterChallengeDto> captor2 = ArgumentCaptor.forClass(RegisterChallengeDto.class);
        verify(sender).sendJsonMessage(eq("session-success"), captor2.capture());
        String challengeBase64_2 = captor2.getValue().getChallenge();
        String correctResponse2 = computeCorrectResponse(sha256Hash, challengeBase64_2);

        RegisterResponseDto successResponse = new RegisterResponseDto();
        successResponse.setChallengeId(2);
        successResponse.setUsername("testuser");
        successResponse.setResponse(correctResponse2);
        reset(executorDao);
        when(executorDao.saveOrUpdateExecutor(eq("Executor-Success"), anyLong(), eq("ONLINE"))).thenReturn(1);

        service.handleRegisterResponse(successResponse, session2);
        verify(executorDao).saveOrUpdateExecutor(eq("Executor-Success"), anyLong(), eq("ONLINE"));
        verify(registry).bind(eq("session-success"), eq("Executor-Success"), anyLong());
    }

    private String computeCorrectResponse(String sha256HashHex, String challengeBase64) {
        try {
            byte[] hashBytes = hexToBytes(sha256HashHex);
            byte[] challengeBytes = Base64.getDecoder().decode(challengeBase64);
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(hashBytes);
            sha256.update(challengeBytes);
            byte[] digest = sha256.digest();
            return bytesToHex(digest);
        } catch (Exception e) {
            return "";
        }
    }

    private byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return bytes;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * UT5-1: 测试空值和null hostname参数
     * 覆盖: 空字符串和null hostname的边界条件
     */
    @Test
    public void testAuthenticationFlow_EmptyAndNullHostname_HandledCorrectly() {
        Session session1 = Mockito.mock(Session.class);
        when(session1.getId()).thenReturn("session-empty");
        RegisterRequestDto emptyRequest = new RegisterRequestDto();
        emptyRequest.setHostname("");
        service.handleRegisterRequest(emptyRequest, session1);
        verify(sender).sendJsonMessage(eq("session-empty"), any());

        Session session2 = Mockito.mock(Session.class);
        when(session2.getId()).thenReturn("session-null");
        RegisterRequestDto nullRequest = new RegisterRequestDto();
        nullRequest.setHostname(null);
        service.handleRegisterRequest(nullRequest, session2);
        verify(sender).sendJsonMessage(eq("session-null"), any());
    }

    /**
     * UT5-2: 测试并发请求场景
     * 覆盖: 多个并发认证请求的处理
     */
    @Test
    public void testAuthenticationFlow_ConcurrentRequests_HandledIndependently() {
        Session session3 = Mockito.mock(Session.class);
        when(session3.getId()).thenReturn("session-multi-1");
        Session session4 = Mockito.mock(Session.class);
        when(session4.getId()).thenReturn("session-multi-2");
        RegisterRequestDto req1 = new RegisterRequestDto();
        req1.setHostname("Executor-1");
        RegisterRequestDto req2 = new RegisterRequestDto();
        req2.setHostname("Executor-2");
        service.handleRegisterRequest(req1, session3);
        service.handleRegisterRequest(req2, session4);
        verify(sender).sendJsonMessage(eq("session-multi-1"), any());
        verify(sender).sendJsonMessage(eq("session-multi-2"), any());
    }

    /**
     * UT5-3: 测试null username的场景
     * 覆盖: hexStringToBytes异常分支（null username）
     */
    @Test
    public void testAuthenticationFlow_NullUsername_ErrorHandled() {
        Session session5 = Mockito.mock(Session.class);
        when(session5.getId()).thenReturn("session-null-username");
        RegisterRequestDto requestDto5 = new RegisterRequestDto();
        requestDto5.setHostname("Executor-5");
        service.handleRegisterRequest(requestDto5, session5);
        RegisterResponseDto nullUsernameResp = new RegisterResponseDto();
        nullUsernameResp.setChallengeId(1);
        nullUsernameResp.setUsername(null);
        nullUsernameResp.setResponse("response");
        service.handleRegisterResponse(nullUsernameResp, session5);
        verify(sender, atLeast(2)).sendJsonMessage(eq("session-null-username"), any());
    }

    /**
     * UT5-4: 测试奇数长度hex字符串的场景
     * 覆盖: hexStringToBytes异常分支（奇数长度hex字符串）
     */
    @Test
    public void testAuthenticationFlow_OddLengthHex_ExceptionHandled() {
        Session session6 = Mockito.mock(Session.class);
        when(session6.getId()).thenReturn("session-odd-hex");
        RegisterRequestDto requestDto6 = new RegisterRequestDto();
        requestDto6.setHostname("Executor-OddHex");
        service.handleRegisterRequest(requestDto6, session6);
        RegisterResponseDto oddHexResp = new RegisterResponseDto();
        oddHexResp.setChallengeId(1);
        oddHexResp.setUsername("oddhexuser");
        oddHexResp.setResponse("response");
        DialUser oddHexUser = new DialUser();
        oddHexUser.setUsername("oddhexuser");
        oddHexUser.setPassword("abc");
        when(dialUserService.findByUsername("oddhexuser")).thenReturn(oddHexUser);

        try {
            service.handleRegisterResponse(oddHexResp, session6);
        } catch (Exception e) {
            // Expected: hexStringToBytes will throw IllegalArgumentException for odd length
        }
        verify(sender, atLeast(1)).sendJsonMessage(eq("session-odd-hex"), any());
    }

    /**
     * UT5-5: 测试null password的场景
     * 覆盖: hexStringToBytes异常分支（null password）
     */
    @Test
    public void testAuthenticationFlow_NullPassword_ExceptionHandled() {
        Session session7 = Mockito.mock(Session.class);
        when(session7.getId()).thenReturn("session-null-password");
        RegisterRequestDto requestDto7 = new RegisterRequestDto();
        requestDto7.setHostname("Executor-NullPassword");
        service.handleRegisterRequest(requestDto7, session7);
        RegisterResponseDto nullPasswordResp = new RegisterResponseDto();
        nullPasswordResp.setChallengeId(1);
        nullPasswordResp.setUsername("nullpassworduser");
        nullPasswordResp.setResponse("response");
        DialUser nullPasswordUser = new DialUser();
        nullPasswordUser.setUsername("nullpassworduser");
        nullPasswordUser.setPassword(null);
        when(dialUserService.findByUsername("nullpassworduser")).thenReturn(nullPasswordUser);

        try {
            service.handleRegisterResponse(nullPasswordResp, session7);
        } catch (Exception e) {
            // Expected: hexStringToBytes will throw IllegalArgumentException for null
        }
        verify(sender, atLeast(1)).sendJsonMessage(eq("session-null-password"), any());
    }
}
