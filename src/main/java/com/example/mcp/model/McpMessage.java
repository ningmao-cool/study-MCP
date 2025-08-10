package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * 一个通用的数据传输对象 (DTO)，旨在封装 JSON-RPC 2.0 协议中定义的所有可能的消息类型：
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpMessage {
    
    @JsonProperty("jsonrpc")
    @Builder.Default
    private String jsonrpc = "2.0";
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("params")
    private Map<String, Object> params;
    
    @JsonProperty("result")
    private Object result;
    
    @JsonProperty("error")
    private McpError error;
    
    /**
     * 创建请求消息
     */
    public static McpMessage createRequest(String id, String method, Map<String, Object> params) {
        return McpMessage.builder()
                .id(id)
                .method(method)
                .params(params)
                .build();
    }
    
    /**
     * 创建响应消息
     */
    public static McpMessage createResponse(String id, Object result) {
        return McpMessage.builder()
                .id(id)
                .result(result)
                .build();
    }
    
    /**
     * 创建错误消息
     */
    public static McpMessage createError(String id, int code, String message) {
        return McpMessage.builder()
                .id(id)
                .error(new McpError(code, message))
                .build();
    }
    
    /**
     * MCP错误模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpError {
        private int code;
        private String message;
        private Object data;
        
        public McpError(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
} 