package com.notification.controller;

import com.notification.dto.CreateNotificationRequest;
import com.notification.dto.CreateNotificationResponse;
import com.notification.dto.ErrorResponse;
import com.notification.dto.NotificationStatusResponse;
import com.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知 REST API 控制器
 * 
 * @author Notification System
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    /**
     * 创建通知任务
     */
    @PostMapping
    public ResponseEntity<CreateNotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        log.info("Received create notification request: vendorCode={}, httpMethod={}", 
                request.getVendorCode(), request.getHttpMethod());
        
        CreateNotificationResponse response = notificationService.createNotification(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询通知状态
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationStatusResponse> getNotificationStatus(
            @PathVariable String notificationId) {
        log.info("Query notification status: {}", notificationId);
        
        NotificationStatusResponse response = notificationService.getNotificationStatus(notificationId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * 全局异常处理 - 参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Request validation failed")
                .details(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 全局异常处理 - 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.of("VALIDATION_ERROR", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    /**
     * 全局异常处理 - 幂等性冲突异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        ErrorResponse errorResponse = ErrorResponse.of("IDEMPOTENCY_CONFLICT", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * 全局异常处理 - 通用异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse errorResponse = ErrorResponse.of("INTERNAL_ERROR", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
