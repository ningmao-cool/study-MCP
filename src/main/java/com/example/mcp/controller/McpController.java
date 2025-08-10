package com.example.mcp.controller;

import com.example.mcp.model.McpMessage;
import com.example.mcp.model.McpTool;
import com.example.mcp.service.McpService;
import com.example.mcp.service.McpToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP 控制器：
 * - /api/mcp/status: 获取系统状态（工具数量等）
 * - /api/mcp/tools: 列出可用工具（名称、描述、输入 schema）
 * - /api/mcp/tools/{toolName}/execute: 执行指定工具
 * - /api/mcp/message: 兼容 MCP JSON-RPC 风格的消息处理
 *
 * 返回体约定：
 * - 工具执行统一返回 { success, executionTime, result?, errorMessage? }
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@RestController
@RequestMapping("/api/mcp")
@CrossOrigin(origins = "*")
public class McpController {
    
    private final McpService mcpService;
    
    @Autowired
    public McpController(McpService mcpService) {
        this.mcpService = mcpService;
    }
    
    /**
     * 处理MCP消息
     */
    @PostMapping("/message")
    public ResponseEntity<McpMessage> handleMessage(@RequestBody McpMessage message) {
        log.info("收到MCP消息: {}", message.getMethod());
        
        try {
            McpMessage response = mcpService.handleMessage(message);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("处理MCP消息异常: {}", e.getMessage(), e);
            McpMessage errorResponse = McpMessage.createError(
                message.getId(), -32603, "内部错误: " + e.getMessage()
            );
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /** 列出可用工具 */
    @GetMapping("/tools")
    public ResponseEntity<List<McpTool>> listTools() {
        log.info("获取可用工具列表");
        
        try {
            List<McpTool> tools = mcpService.getAvailableTools();
            return ResponseEntity.ok(tools);
        } catch (Exception e) {
            log.error("获取工具列表异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取工具信息
     */
    @GetMapping("/tools/{toolName}")
    public ResponseEntity<McpTool> getTool(@PathVariable String toolName) {
        log.info("获取工具信息: {}", toolName);
        
        try {
            if (!mcpService.hasTool(toolName)) {
                return ResponseEntity.notFound().build();
            }
            
            McpTool tool = mcpService.getToolExecutor(toolName).getToolModel();
            return ResponseEntity.ok(tool);
        } catch (Exception e) {
            log.error("获取工具信息异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /** 执行某个工具 */
    @PostMapping("/tools/{toolName}/execute")
    public ResponseEntity<Map<String, Object>> executeTool(
            @PathVariable String toolName,
            @RequestBody Map<String, Object> parameters) {
        log.info("执行工具: {}, 参数: {}", toolName, parameters);
        
        try {
            if (!mcpService.hasTool(toolName)) {
                Map<String, Object> errorResponse = new java.util.HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("errorMessage", "工具不存在: " + toolName);
                errorResponse.put("executionTime", 0L);
                return ResponseEntity.ok(errorResponse);
            }
            
            log.info("开始执行工具: {}, 参数: {}", toolName, parameters);
            McpToolExecutor executor = mcpService.getToolExecutor(toolName);
            log.info("获取到执行器: {}", executor.getClass().getSimpleName());
            
            McpToolExecutor.ToolExecutionResult result = executor.execute(parameters);
            log.info("工具执行完成: success={}, errorMessage={}", result.isSuccess(), result.getErrorMessage());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", result.isSuccess());
            response.put("executionTime", result.getExecutionTime());
            if (result.getResult() != null) {
                response.put("result", result.getResult());
            }
            if (result.getErrorMessage() != null) {
                response.put("errorMessage", result.getErrorMessage());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("执行工具异常: {}", e.getMessage(), e);
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = e.getClass().getSimpleName();
            }
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorMessage", "执行工具异常: " + errorMsg);
            errorResponse.put("executionTime", 0L);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /** 获取系统状态（演示用途） */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info("获取系统状态");
        
        try {
            Map<String, Object> status = Map.of(
                "toolCount", mcpService.getToolCount(),
                "availableTools", mcpService.getAvailableTools().stream()
                    .map(McpTool::getName)
                    .toList(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("获取系统状态异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 