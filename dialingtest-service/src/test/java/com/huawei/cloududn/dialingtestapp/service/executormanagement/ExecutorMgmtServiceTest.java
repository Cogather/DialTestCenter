package com.huawei.cloududn.dialingtestapp.service.executormanagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.huawei.cloududn.dialingtest.model.Executor;
import com.huawei.cloududn.dialingtest.model.Ue;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterAckDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.DeRegisterRequestDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportAckDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.ReportMsgDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.dto.UeItemDto;
import com.huawei.cloududn.dialingtestapp.controller.executormanagement.websocket.flow.WssMessageSender;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.ExecutorDao;
import com.huawei.cloududn.dialingtestapp.dao.executormanagement.UeDao;
import com.huawei.cloududn.dialingtestapp.service.executormanagement.dto.ExecutorDetailDto;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.websocket.Session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExecutorMgmtServiceTest {

    @Mock private ExecutorDao executorDao;
    @Mock private UeDao ueDao;
    @Mock private SessionBindingRegistry registry;
    @Mock private WssMessageSender wssMessageSender;
    @InjectMocks private ExecutorMgmtService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testHandleReportMsg_WithUeItems_PersistsAndAckOk() {
        Session session = session("session-1", "executor-1");
        ReportMsgDto dto = reportMsg(123L, ueItem("SN1", "Huawei", "HarmonyOS", "4.0", 80), ueItem("SN2", null, null, "13", null));
        service.handleReportMsg(dto, session);
        ArgumentCaptor<Ue> ueCaptor = ArgumentCaptor.forClass(Ue.class);
        verify(ueDao, times(2)).upsert(ueCaptor.capture());
        List<Ue> saved = ueCaptor.getAllValues();
        assertEquals("HarmonyOS 4.0", saved.get(0).getOs());
        assertTrue(saved.get(0).getInfo().contains("\"serial\":\"SN1\""));
        assertEquals("13", saved.get(1).getOs());
        ArgumentCaptor<Object> ackCaptor = ArgumentCaptor.forClass(Object.class);
        verify(wssMessageSender).sendJsonMessage(eq("session-1"), ackCaptor.capture());
        assertReportAck(ackCaptor.getValue(), 123L, 0); clearInvocations(executorDao, ueDao, wssMessageSender);
        service.handleReportMsg(reportMsg(321L), session);
        verify(executorDao).updateStatus(eq("executor-1"), eq(1), any(Instant.class));
        verifyNoInteractions(ueDao);
        ArgumentCaptor<Object> ackCaptor2 = ArgumentCaptor.forClass(Object.class);
        verify(wssMessageSender).sendJsonMessage(eq("session-1"), ackCaptor2.capture());
        assertReportAck(ackCaptor2.getValue(), 321L, 0);
    }
    @Test
    public void testHandleReportMsg_NoBinding_SendsErrorAck() {
        Session session = session("session-2", null);
        service.handleReportMsg(reportMsg(55L), session);
        verifyNoInteractions(executorDao, ueDao);
        ArgumentCaptor<Object> ackCaptor = ArgumentCaptor.forClass(Object.class);
        verify(wssMessageSender).sendJsonMessage(eq("session-2"), ackCaptor.capture());
        assertReportAck(ackCaptor.getValue(), 55L, 1);
    }
    @Test
    public void testHandleDeRegisterRequest_SuccessAndDatabaseError() {
        Session session = session("session-dereg", "executor-dereg");
        DeRegisterRequestDto dto = new DeRegisterRequestDto(999L, "host");
        service.handleDeRegisterRequest(dto, session);
        doThrow(new IllegalArgumentException("db")).when(executorDao)
            .updateStatus(eq("executor-dereg"), eq(0), any(Instant.class));
        service.handleDeRegisterRequest(dto, session);
        verify(executorDao, times(2)).updateStatus(eq("executor-dereg"), eq(0), any(Instant.class));
        verify(registry, times(1)).unbind("session-dereg");
        ArgumentCaptor<Object> ackCaptor = ArgumentCaptor.forClass(Object.class);
        verify(wssMessageSender, times(2)).sendJsonMessage(eq("session-dereg"), ackCaptor.capture());
        List<Object> acks = ackCaptor.getAllValues();
        assertDeregisterAck(acks.get(0), 0);
        assertDeregisterAck(acks.get(1), 2);
    }
    @Test
    public void testHeartbeatStatusAndTimeoutFlow() {
        Session session = session("hb-session", "exec-hb");
        ObjectNode payload = mapper.createObjectNode();
        payload.putArray("ue_list").add(mapper.createObjectNode().put("msisdn", "1001")).add(mapper.createObjectNode().put("msisdn", "1002"));
        service.handleHeartbeatStatus(payload, session);
        verify(executorDao).updateStatus(eq("exec-hb"), eq(1), any(Instant.class));
        verify(ueDao, times(2)).upsert(any(Ue.class));
        clearInvocations(executorDao, ueDao);
        Session missing = session("hb-missing", null);
        service.handleHeartbeatStatus(null, missing);
        verifyNoInteractions(executorDao, ueDao);
        clearInvocations(executorDao, registry);
        service.handleHeartbeatTimeout(" ");
        verifyNoInteractions(executorDao);
        when(registry.getSessionId("exec-timeout")).thenReturn("timeout-session");
        service.handleHeartbeatTimeout("exec-timeout");
        verify(executorDao).updateStatus(eq("exec-timeout"), eq(0), any(Instant.class));
        verify(registry).unbind("timeout-session");
        clearInvocations(executorDao, registry);
        when(registry.getExecutorName("disc-session")).thenReturn("exec-disc");
        service.handleExecutorDisconnect("disc-session");
        verify(executorDao).updateStatus(eq("exec-disc"), eq(0), any(Instant.class));
        verify(registry).unbind("disc-session");
        clearInvocations(executorDao, registry);
        when(registry.getExecutorName("disc-none")).thenReturn(null);
        service.handleExecutorDisconnect("disc-none");
        verifyNoInteractions(executorDao); verify(registry, times(0)).unbind(anyString());
    }
    @Test
    public void testHandleDeregisterAndExecutorInfoResponse() {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("session-old");
        when(registry.getExecutorName("session-old")).thenReturn("exec-old", "mismatch");
        ObjectNode dereg = mapper.createObjectNode().put("name", "exec-old");
        service.handleDeregister(dereg, session); service.handleDeregister(dereg, session);
        verify(executorDao, times(1)).updateStatus(eq("exec-old"), eq(0), any(Instant.class));
        verify(registry, times(1)).unbind("session-old");
        service.handleDeregister(mapper.createObjectNode(), session);
        clearInvocations(ueDao);
        ObjectNode execInfo = mapper.createObjectNode().put("executor_name", "exec-info"); execInfo.putArray("ue_details")
            .add(mapper.createObjectNode().put("msisdn", "msisdn-1").put("vendor", "v"))
            .add(mapper.createObjectNode());
        service.handleExecutorInfoResponse(execInfo, session);
        ObjectNode noList = mapper.createObjectNode().put("executor_name", "exec-info"); service.handleExecutorInfoResponse(noList, session);
        verify(ueDao, times(1)).upsert(any(Ue.class));
    }
    @Test
    public void testGetExecutorDetailsAndPublicAck() {
        Executor executor = new Executor().name("exec-detail").ip("10.0.0.1").status(1).lastOnlineTime("2025-11-18T00:00:00Z");
        when(executorDao.findPage(any(), any(), anyInt(), anyInt())).thenReturn(Collections.singletonList(executor));
        when(ueDao.findByExecutorName("exec-detail")).thenReturn(Collections.singletonList(new Ue().msisdn("msisdn-1").os("Android")));
        List<ExecutorDetailDto> details = service.getExecutorDetails();
        assertEquals(1, details.size());
        ExecutorDetailDto detail = details.get(0);
        assertEquals("exec-detail", detail.getName());
        assertEquals(1, detail.getUeList().size());
        assertEquals("msisdn-1", detail.getUeList().get(0).getSerialNo());
        assertEquals("Android", detail.getUeList().get(0).getOs());

        service.sendReportAck("session-ack", 888L);
        ArgumentCaptor<Object> ackCaptor = ArgumentCaptor.forClass(Object.class);
        verify(wssMessageSender).sendJsonMessage(eq("session-ack"), ackCaptor.capture());
        assertReportAck(ackCaptor.getValue(), 888L, 0);
    }

    private ReportMsgDto reportMsg(long token, UeItemDto... items) {
        ReportMsgDto dto = new ReportMsgDto();
        dto.setToken(token);
        dto.setState("ONLINE");
        dto.setUeList(items == null ? Collections.emptyList() : Arrays.asList(items));
        return dto;
    }

    private UeItemDto ueItem(String serial, String brand, String os, String version, Integer battery) {
        UeItemDto dto = new UeItemDto();
        dto.setSerialNo(serial);
        dto.setBrand(brand);
        dto.setOs(os);
        dto.setVersion(version);
        dto.setBattery(battery);
        return dto;
    }

    private Session session(String id, String executorName) {
        Session s = mock(Session.class);
        when(s.getId()).thenReturn(id);
        when(registry.getExecutorName(id)).thenReturn(executorName);
        return s;
    }

    private void assertReportAck(Object payload, long token, int state) {
        ReportAckDto ack = (ReportAckDto) payload;
        assertEquals(state, ack.getState());
        assertEquals(token, ack.getToken());
    }
    private void assertDeregisterAck(Object payload, int code) {
        assertEquals(code, ((DeRegisterAckDto) payload).getResultCode());
    }
}
