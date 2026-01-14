package com.notification.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP 客户端服务
 * 
 * @author Notification System
 */
@Slf4j
@Service
public class HttpClientService {
    
    private final WebClient webClient;
    
    public HttpClientService() {
        this.webClient = WebClient.builder().build();
    }
    
    /**
     * 执行 HTTP 调用
     */
    public HttpResponse call(
            String url,
            HttpMethod method,
            Map<String, String> headers,
            String body,
            int timeoutMs) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            WebClient.RequestBodySpec requestSpec = webClient
                    .method(method)
                    .uri(url)
                    .headers(httpHeaders -> {
                        if (headers != null) {
                            headers.forEach(httpHeaders::add);
                        }
                        if (!httpHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                        }
                    });
            
            // 添加请求体（如果有）
            if (body != null && !body.isEmpty() && 
                (method == HttpMethod.POST || method == HttpMethod.PUT)) {
                requestSpec.bodyValue(body);
            }
            
            // 执行请求
            String responseBody = requestSpec
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorResume(WebClientResponseException.class, ex -> 
                        Mono.just(ex.getResponseBodyAsString())
                    )
                    .block();
            
            long costMs = System.currentTimeMillis() - startTime;
            
            return HttpResponse.builder()
                    .statusCode(200)
                    .body(responseBody)
                    .costMs((int) costMs)
                    .success(true)
                    .build();
            
        } catch (WebClientResponseException e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.warn("HTTP call failed with status {}: url={}, method={}, costMs={}", 
                    e.getStatusCode().value(), url, method, costMs);
            
            return HttpResponse.builder()
                    .statusCode(e.getStatusCode().value())
                    .body(e.getResponseBodyAsString())
                    .costMs((int) costMs)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
            
        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("HTTP call failed with exception: url={}, method={}, costMs={}", 
                    url, method, costMs, e);
            
            return HttpResponse.builder()
                    .statusCode(0)
                    .body(null)
                    .costMs((int) costMs)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * HTTP 响应对象
     */
    @Data
    @Builder
    public static class HttpResponse {
        private int statusCode;
        private String body;
        private int costMs;
        private boolean success;
        private String errorMessage;
        
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        public boolean isClientError() {
            return statusCode >= 400 && statusCode < 500;
        }
        
        public boolean isServerError() {
            return statusCode >= 500 && statusCode < 600;
        }
        
        public boolean isTimeout() {
            return statusCode == 0 && errorMessage != null && 
                   errorMessage.toLowerCase().contains("timeout");
        }
    }
}
