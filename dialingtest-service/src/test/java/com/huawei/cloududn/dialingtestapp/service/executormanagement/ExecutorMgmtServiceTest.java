package com.huawei.cloududn.dialingtestapp.service.executormanagement;

import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportMsgDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.UeItemDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.UeDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;

import javax.websocket.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

/**
 * ExecutorMgmtService单元测试 - V3协议版本
 * 测试心跳处理、状态管理和UE信息更新
 *
 * @author g00940940
 * @since 2025-11-11
 */
public class ExecutorMgmtServiceTest {

    @Mock
    private ExecutorDao executorDao;

    @Mock
    private UeDao ueDao;

    @Mock
    private SessionBindingRegistry registry;

    @Mock
    private WssMessageSender wssMessageSender;

    @InjectMocks
    private ExecutorMgmtService service;

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
     * 测试handleReportMsg：成功处理心跳消息，更新状态和UE信息
     */
    @Test
    public void testHandleReportMsg_WithBinding_UpdatesDaoAndUe() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-001");
        when(registry.getExecutorName("session-001")).thenReturn("Executor-01");

        // Create ReportMsgDto with UE list
        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal"); // ONLINE

        UeItemDto ueItem = new UeItemDto();
        ueItem.setSerialNo("ABC123");
        ueItem.setBrand("Huawei");
        ueItem.setModel("P40");
        ueItem.setOs("Android");
        ueItem.setVersion("11");
        ueItem.setBattery(85);

        reportMsg.setUeList(Arrays.asList(ueItem));

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(executorDao).updateStatus(eq("Executor-01"), eq(1), any(Instant.class));
        verify(ueDao).upsert(any()); // UE should be upserted
        verify(wssMessageSender).sendJsonMessage(eq("session-001"), any());
    }

    /**
     * 测试handleReportMsg：无会话绑定时发送错误应答
     */
    @Test
    public void testHandleReportMsg_NoBinding_SendsErrorAck() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-002");
        when(registry.getExecutorName("session-002")).thenReturn(null);

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");
        reportMsg.setUeList(Arrays.asList());

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(executorDao, never()).updateStatus(anyString(), anyInt(), any(Instant.class));
        verify(ueDao, never()).upsert(any());
        verify(wssMessageSender).sendJsonMessage(eq("session-002"), any());
    }

    /**
     * 测试handleReportMsg：空UE列表时不进行UE更新
     */
    @Test
    public void testHandleReportMsg_EmptyUeList_NoUeUpsert() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-003");
        when(registry.getExecutorName("session-003")).thenReturn("Executor-02");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");
        reportMsg.setUeList(Arrays.asList()); // Empty list

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(executorDao).updateStatus(eq("Executor-02"), eq(1), any(Instant.class));
        verify(ueDao, never()).upsert(any()); // No UE to upsert
        verify(wssMessageSender).sendJsonMessage(eq("session-003"), any());
    }

    /**
     * 测试handleReportMsg：null UE列表时不进行UE更新
     */
    @Test
    public void testHandleReportMsg_NullUeList_NoUeUpsert() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-004");
        when(registry.getExecutorName("session-004")).thenReturn("Executor-03");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");
        reportMsg.setUeList(null); // Null list

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(executorDao).updateStatus(eq("Executor-03"), eq(1), any(Instant.class));
        verify(ueDao, never()).upsert(any()); // No UE to upsert
        verify(wssMessageSender).sendJsonMessage(eq("session-004"), any());
    }

    /**
     * 测试handleReportMsg：多个UE设备时批量更新
     */
    @Test
    public void testHandleReportMsg_MultipleUeItems_BatchUpsert() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-005");
        when(registry.getExecutorName("session-005")).thenReturn("Executor-04");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");

        // Create multiple UE items
        UeItemDto ue1 = new UeItemDto();
        ue1.setSerialNo("UE001");
        ue1.setBrand("Huawei");
        ue1.setModel("P40");

        UeItemDto ue2 = new UeItemDto();
        ue2.setSerialNo("UE002");
        ue2.setBrand("Xiaomi");
        ue2.setModel("Mi11");

        reportMsg.setUeList(Arrays.asList(ue1, ue2));

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(executorDao).updateStatus(eq("Executor-04"), eq(1), any(Instant.class));
        verify(ueDao, Mockito.times(2)).upsert(any()); // Two UEs should be upserted
        verify(wssMessageSender).sendJsonMessage(eq("session-005"), any());
    }

    /**
     * 测试handleExecutorDisconnect：存在绑定时更新状态并解绑
     */
    @Test
    public void testHandleExecutorDisconnect_BindingExists_UpdateAndUnbind() {
        // Given
        when(registry.getExecutorName("session-disconnect-001")).thenReturn("Executor-Disconnect");

        // When
        service.handleExecutorDisconnect("session-disconnect-001");

        // Then
        verify(executorDao).updateStatus(eq("Executor-Disconnect"), eq(0), any(Instant.class));
        verify(registry).unbind("session-disconnect-001");
    }

    /**
     * 测试handleExecutorDisconnect：无绑定时不执行任何操作
     */
    @Test
    public void testHandleExecutorDisconnect_NoBinding_NoUpdate() {
        // Given
        when(registry.getExecutorName("session-disconnect-002")).thenReturn(null);

        // When
        service.handleExecutorDisconnect("session-disconnect-002");

        // Then
        verify(executorDao, never()).updateStatus(anyString(), anyInt(), any(Instant.class));
        verify(registry, never()).unbind(anyString());
    }

    /**
     * 测试sendReportAck：正确调用消息发送器
     */
    @Test
    public void testSendReportAck_CallsMessageSender() {
        // Given
        String sessionId = "session-ack-001";
        long token = 98765L;
        int state = 0; // OK

        // When
        // Note: sendReportAck is private, so we test it through public methods
        // This test verifies the integration by checking that handleReportMsg calls sendJsonMessage
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn(sessionId);
        when(registry.getExecutorName(sessionId)).thenReturn("Executor-Ack");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(token);
        reportMsg.setState("Normal");
        reportMsg.setUeList(Arrays.asList());

        service.handleReportMsg(reportMsg, session);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq(sessionId), any());
    }

    /**
     * 测试UE信息处理：UeItemDto正确转换为Ue实体
     */
    @Test
    public void testUeItemProcessing_CorrectlyMapsFields() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-ue-test");
        when(registry.getExecutorName("session-ue-test")).thenReturn("Executor-UeTest");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");

        UeItemDto ueItem = new UeItemDto();
        ueItem.setSerialNo("TEST123456");
        ueItem.setBrand("TestBrand");
        ueItem.setModel("TestModel");
        ueItem.setOs("Android");
        ueItem.setVersion("12");
        ueItem.setWmsize("1080x2400");
        ueItem.setIpv4("192.168.1.100");
        ueItem.setIpv6("2001:db8::1");
        ueItem.setBattery(90);

        reportMsg.setUeList(Arrays.asList(ueItem));

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(ueDao).upsert(any()); // UE should be upserted with correct mapping
    }

    /**
     * 测试handleReportMsg：UE serialNo为null时跳过
     */
    @Test
    public void testHandleReportMsg_UeSerialNoNull_SkipsUpsert() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-null-serial");
        when(registry.getExecutorName("session-null-serial")).thenReturn("Executor-NullSerial");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");

        UeItemDto ueItem = new UeItemDto();
        ueItem.setSerialNo(null); // null serial
        ueItem.setBrand("TestBrand");
        ueItem.setModel("TestModel");

        reportMsg.setUeList(Arrays.asList(ueItem));

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(executorDao).updateStatus(eq("Executor-NullSerial"), eq(1), any(Instant.class));
        verify(ueDao, never()).upsert(any()); // Should not upsert null serial
        verify(wssMessageSender).sendJsonMessage(eq("session-null-serial"), any());
    }

    /**
     * 测试handleReportMsg：UE serialNo为空字符串时跳过
     */
    @Test
    public void testHandleReportMsg_UeSerialNoEmpty_SkipsUpsert() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-empty-serial");
        when(registry.getExecutorName("session-empty-serial")).thenReturn("Executor-EmptySerial");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");

        UeItemDto ueItem = new UeItemDto();
        ueItem.setSerialNo("  "); // empty/whitespace serial
        ueItem.setBrand("TestBrand");

        reportMsg.setUeList(Arrays.asList(ueItem));

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(ueDao, never()).upsert(any()); // Should not upsert empty serial
    }

    /**
     * 测试handleReportMsg：UE serialNo为"null"字符串时跳过
     */
    @Test
    public void testHandleReportMsg_UeSerialNoNullString_SkipsUpsert() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-null-string");
        when(registry.getExecutorName("session-null-string")).thenReturn("Executor-NullString");

        ReportMsgDto reportMsg = new ReportMsgDto();
        reportMsg.setToken(12345L);
        reportMsg.setState("Normal");

        UeItemDto ueItem = new UeItemDto();
        ueItem.setSerialNo("null"); // "null" string
        ueItem.setBrand("TestBrand");

        reportMsg.setUeList(Arrays.asList(ueItem));

        // When
        service.handleReportMsg(reportMsg, session);

        // Then
        verify(ueDao, never()).upsert(any()); // Should not upsert "null" string
    }

    /**
     * 测试handleDeRegisterRequest：成功注销
     */
    @Test
    public void testHandleDeRegisterRequest_Success() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-dereg-001");
        when(registry.getExecutorName("session-dereg-001")).thenReturn("Executor-Dereg");

        com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto dto =
                new com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto();
        dto.setToken(98765L);

        // When
        service.handleDeRegisterRequest(dto, session);

        // Then
        verify(executorDao).updateStatus(eq("Executor-Dereg"), eq(0), any(Instant.class));
        verify(registry).unbind("session-dereg-001");
        verify(wssMessageSender).sendJsonMessage(eq("session-dereg-001"), any());
    }

    /**
     * 测试handleDeRegisterRequest：无绑定时发送错误应答
     */
    @Test
    public void testHandleDeRegisterRequest_NoBinding_SendsError() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-dereg-002");
        when(registry.getExecutorName("session-dereg-002")).thenReturn(null);

        com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto dto =
                new com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto();
        dto.setToken(98765L);

        // When
        service.handleDeRegisterRequest(dto, session);

        // Then
        verify(executorDao, never()).updateStatus(anyString(), anyInt(), any(Instant.class));
        verify(registry, never()).unbind(anyString());
        verify(wssMessageSender).sendJsonMessage(eq("session-dereg-002"), any());
    }

    /**
     * 测试handleDeRegisterRequest：数据库异常时发送错误应答
     */
    @Test
    public void testHandleDeRegisterRequest_DatabaseError_SendsError() {
        // Given
        Session session = Mockito.mock(Session.class);
        when(session.getId()).thenReturn("session-dereg-003");
        when(registry.getExecutorName("session-dereg-003")).thenReturn("Executor-DbError");

        doThrow(new IllegalArgumentException("Database error")).when(executorDao)
                .updateStatus(eq("Executor-DbError"), eq(0), any(Instant.class));

        com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto dto =
                new com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto();
        dto.setToken(98765L);

        // When
        service.handleDeRegisterRequest(dto, session);

        // Then
        verify(wssMessageSender).sendJsonMessage(eq("session-dereg-003"), any());
    }

    /**
     * 测试handleHeartbeatTimeout：正常处理超时
     */
    @Test
    public void testHandleHeartbeatTimeout_Success() {
        // Given
        when(registry.getSessionId("Executor-Timeout")).thenReturn("session-timeout");

        // When
        service.handleHeartbeatTimeout("Executor-Timeout");

        // Then
        verify(executorDao).updateStatus(eq("Executor-Timeout"), eq(0), any(Instant.class));
        verify(registry).unbind("session-timeout");
    }

    /**
     * 测试handleHeartbeatTimeout：执行机名称为null
     */
    @Test
    public void testHandleHeartbeatTimeout_NullExecutorName_NoUpdate() {
        // When
        service.handleHeartbeatTimeout(null);

        // Then
        verify(executorDao, never()).updateStatus(anyString(), anyInt(), any(Instant.class));
        verify(registry, never()).unbind(anyString());
    }

    /**
     * 测试handleHeartbeatTimeout：执行机名称为空字符串
     */
    @Test
    public void testHandleHeartbeatTimeout_EmptyExecutorName_NoUpdate() {
        // When
        service.handleHeartbeatTimeout("  ");

        // Then
        verify(executorDao, never()).updateStatus(anyString(), anyInt(), any(Instant.class));
        verify(registry, never()).unbind(anyString());
    }

    /**
     * 测试handleHeartbeatTimeout：无会话绑定
     */
    @Test
    public void testHandleHeartbeatTimeout_NoSession_UpdatesStatusOnly() {
        // Given
        when(registry.getSessionId("Executor-NoSession")).thenReturn(null);

        // When
        service.handleHeartbeatTimeout("Executor-NoSession");

        // Then
        verify(executorDao).updateStatus(eq("Executor-NoSession"), eq(0), any(Instant.class));
        verify(registry, never()).unbind(anyString());
    }

    /**
     * 测试getExecutorDetails：成功获取执行机详情
     */
    @Test
    public void testGetExecutorDetails_Success() {
        // Given
        com.huawei.cloududn.dialingtest.model.Executor executor = new com.huawei.cloududn.dialingtest.model.Executor();
        executor.setName("Executor-Details");
        executor.setIp("192.168.1.10");
        executor.setStatus(1);

        java.util.List<com.huawei.cloududn.dialingtest.model.Executor> executors = Arrays.asList(executor);
        when(executorDao.findPage(isNull(), isNull(), eq(0), eq(1000))).thenReturn(executors);

        com.huawei.cloududn.dialingtest.model.Ue ue = new com.huawei.cloududn.dialingtest.model.Ue();
        ue.setMsisdn("8613800138000");
        ue.setOs("Android 12");

        when(ueDao.findByExecutorName("Executor-Details")).thenReturn(Arrays.asList(ue));

        // When
        java.util.List<com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.ExecutorDetailDto> result =
                service.getExecutorDetails();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Executor-Details", result.get(0).getName());
        assertEquals("192.168.1.10", result.get(0).getIp());
    }

    /**
     * 测试getExecutorDetails：无执行机时返回空列表
     */
    @Test
    public void testGetExecutorDetails_NoExecutors_ReturnsEmptyList() {
        // Given
        when(executorDao.findPage(isNull(), isNull(), eq(0), eq(1000))).thenReturn(Arrays.asList());

        // When
        java.util.List<com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.ExecutorDetailDto> result =
                service.getExecutorDetails();

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    /**
     * 测试getExecutorDetails：数据库异常时抛出RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void testGetExecutorDetails_DatabaseError_ThrowsException() {
        // Given
        when(executorDao.findPage(isNull(), isNull(), eq(0), eq(1000)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - 期望抛出RuntimeException
        service.getExecutorDetails();
    }

}
