package com.huawei.cloududn.dialingtestapp.exceptions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultGlobalExceptionTest {

    private DefaultGlobalException exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @Before
    public void setUp() {
        exceptionHandler = new DefaultGlobalException();
        when(request.getRequestURI()).thenReturn("/test/api");
        when(request.getMethod()).thenReturn("GET");
    }

    @Test
    public void testHandleNoHandlerFoundException_Standard_ReturnsNotFound() {
        // Arrange
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/test/url", new org.springframework.http.HttpHeaders());
        
        // Act
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleNoHandlerFoundException(ex, request);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("NOT_FOUND", body.get("errorCode"));
        assertEquals(404, body.get("statusCode"));
        assertFalse((Boolean) body.get("success"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(((String) body.get("message")).contains("请求的资源不存在"));
    }

    @Test
    public void testHandleResponseStatusException_WithReason_ReturnsSpecifiedStatus() {
        // Arrange
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Parameter");

        // Act
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResponseStatusException(ex, request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("BAD_REQUEST", body.get("errorCode"));
        assertEquals("Invalid Parameter", body.get("message"));
        assertEquals(400, body.get("statusCode"));
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    public void testHandleResponseStatusException_WithoutReason_ReturnsDefaultMessage() {
        // Arrange
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN);

        // Act
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleResponseStatusException(ex, request);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("FORBIDDEN", body.get("errorCode"));
        assertEquals("Forbidden", body.get("message")); // Default reason phrase
        assertEquals(403, body.get("statusCode"));
    }

    @Test
    public void testHandleNullPointerException_Standard_ReturnsInternalServerError() {
        // Arrange
        NullPointerException ex = new NullPointerException("Null value encountered");

        // Act
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleNullPointerException(ex, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("NULL_POINTER", body.get("errorCode"));
        assertEquals("系统内部错误", body.get("message"));
        assertEquals(500, body.get("statusCode"));
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    public void testHandleRuntimeException_Standard_ReturnsInternalServerError() {
        // Arrange
        RuntimeException ex = new RuntimeException("Runtime error occurred");

        // Act
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRuntimeException(ex, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("RUNTIME_ERROR", body.get("errorCode"));
        assertEquals("系统运行时错误", body.get("message"));
        assertEquals(500, body.get("statusCode"));
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    public void testHandleGenericException_Standard_ReturnsInternalServerError() {
        // Arrange
        Exception ex = new Exception("Unexpected error");

        // Act
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleGenericException(ex, request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.get("errorCode"));
        assertEquals("系统内部错误", body.get("message"));
        assertEquals(500, body.get("statusCode"));
        assertFalse((Boolean) body.get("success"));
    }
}

