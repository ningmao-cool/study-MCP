package com.example.mcp.service;

import com.example.mcp.model.McpMessage;
import com.example.mcp.model.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 服务：聚合并暴露工具执行器。
 *
 * 职责：
 * - 在应用启动后收集 Spring 容器中所有 McpToolExecutor
 * - 提供名称到执行器的查找
 * - 提供工具列表给控制器/前端展示
 * - 处理MCP JSON-RPC协议消息
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@Service
public class McpService {
    
    private final Map<String, McpToolExecutor> toolExecutors = new ConcurrentHashMap<>();
    
    @Autowired
    public McpService(List<McpToolExecutor> executors) {
        // 注册所有工具执行器
        for (McpToolExecutor executor : executors) {
            toolExecutors.put(executor.getToolName(), executor);
            log.info("注册工具执行器: {}", executor.getToolName());
        }
    }
    
    /**
     * 处理MCP消息
     */
    public McpMessage handleMessage(McpMessage message) {
        log.info("处理MCP消息: {}", message.getMethod());
        
        try {
            switch (message.getMethod()) {
                case "tools/list":
                    return handleListTools(message);
                case "tools/call":
                    return handleCallTool(message);
                case "tools/get":
                    return handleGetTool(message);
                default:
                    return McpMessage.createError(message.getId(), -32601, "方法不存在");
            }
        } catch (Exception e) {
            log.error("处理MCP消息异常: {}", e.getMessage(), e);
            return McpMessage.createError(message.getId(), -32603, "内部错误: " + e.getMessage());
        }
    }
    
    /**
     * 处理工具列表请求
     */
    private McpMessage handleListTools(McpMessage message) {
        List<McpTool> tools = new ArrayList<>();
        
        for (McpToolExecutor executor : toolExecutors.values()) {
            tools.add(executor.getToolModel());
        }
        
        return McpMessage.createResponse(message.getId(), Map.of("tools", tools));
    }
    
    /**
     * 处理工具调用请求
     */
    private McpMessage handleCallTool(McpMessage message) {
        Map<String, Object> params = message.getParams();
        if (params == null) {
            return McpMessage.createError(message.getId(), -32602, "缺少参数");
        }
        
        String toolName = (String) params.get("name");
        if (toolName == null) {
            return McpMessage.createError(message.getId(), -32602, "缺少工具名称");
        }
        
        McpToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            return McpMessage.createError(message.getId(), -32601, "工具不存在: " + toolName);
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        // 执行工具
        McpToolExecutor.ToolExecutionResult result = executor.execute(arguments);
        
        if (result.isSuccess()) {
            return McpMessage.createResponse(message.getId(), Map.of(
                "content", List.of(Map.of(
                    "type", "text",
                    "text", result.getResult().toString()
                ))
            ));
        } else {
            return McpMessage.createError(message.getId(), -32603, result.getErrorMessage());
        }
    }
    
    /**
     * 处理获取工具信息请求
     */
    private McpMessage handleGetTool(McpMessage message) {
        Map<String, Object> params = message.getParams();
        if (params == null) {
            return McpMessage.createError(message.getId(), -32602, "缺少参数");
        }
        
        String toolName = (String) params.get("name");
        if (toolName == null) {
            return McpMessage.createError(message.getId(), -32602, "缺少工具名称");
        }
        
        McpToolExecutor executor = toolExecutors.get(toolName);
        if (executor == null) {
            return McpMessage.createError(message.getId(), -32601, "工具不存在: " + toolName);
        }
        
        return McpMessage.createResponse(message.getId(), executor.getToolModel());
    }
    
    /**
     * 获取所有可用工具
     */
    public List<McpTool> getAvailableTools() {
        List<McpTool> tools = new ArrayList<>();
        for (McpToolExecutor executor : toolExecutors.values()) {
            tools.add(executor.getToolModel());
        }
        return tools;
    }
    
    /**
     * 获取工具执行器
     */
    public McpToolExecutor getToolExecutor(String toolName) {
        return toolExecutors.get(toolName);
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return toolExecutors.containsKey(toolName);
    }
    
    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return toolExecutors.size();
    }
} 