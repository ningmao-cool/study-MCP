package com.example.mcp.service.impl;

import com.example.mcp.model.McpTool;
import com.example.mcp.service.McpToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * execute_workflow 工具执行器
 *
 * 功能：在"工作流步骤中"作为子工作流触发器，启动指定工作流的执行。
 * 参数：
 * - workflowId: 要执行的工作流 ID
 * - parameters: 传递给子工作流的参数（可选）
 *
 * 返回：
 * - { executionId, workflowId, status: RUNNING, startedAt, parameters? }
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@Service
public class WorkflowToolExecutor implements McpToolExecutor {
    
    private static final String TOOL_NAME = "execute_workflow";
    private static final String TOOL_DESCRIPTION = "执行工作流";
    
    @Override
    public String getToolName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getToolDescription() {
        return TOOL_DESCRIPTION;
    }
    
    @Override
    public McpTool getToolModel() {
        return McpTool.createWorkflowTool();
    }
    
    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证参数
            if (!validateParameters(parameters)) {
                return ToolExecutionResult.failure("参数验证失败", System.currentTimeMillis() - startTime);
            }
            
            String workflowId = (String) parameters.get("workflowId");
            @SuppressWarnings("unchecked")
            Map<String, Object> workflowParameters = (Map<String, Object>) parameters.get("parameters");
            
            // 生成执行ID
            String executionId = UUID.randomUUID().toString();
            
            log.info("开始执行工作流: {}, 执行ID: {}", workflowId, executionId);
            
            // 这里应该调用工作流执行服务
            // 为了演示，我们创建一个模拟的执行结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("executionId", executionId);
            result.put("workflowId", workflowId);
            result.put("status", "RUNNING");
            result.put("startedAt", System.currentTimeMillis());
            if (workflowParameters != null) {
                result.put("parameters", workflowParameters);
            }
            
            log.info("工作流执行已启动: {}", executionId);
            
            return ToolExecutionResult.success(result, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("工作流执行异常: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("工作流执行异常: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 检查必需参数
        if (!parameters.containsKey("workflowId")) {
            return false;
        }
        
        String workflowId = (String) parameters.get("workflowId");
        
        // 验证工作流ID不为空
        if (workflowId == null || workflowId.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
} 