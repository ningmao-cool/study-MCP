package com.example.mcp.controller;

import com.example.mcp.model.Workflow;
import lombok.extern.slf4j.Slf4j;
import com.example.mcp.model.WorkflowExecution;
import com.example.mcp.repository.WorkflowRepository;
import com.example.mcp.service.WorkflowExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流控制器
 *
 * 提供工作流管理的REST API：
 * - GET /api/workflows: 获取所有工作流列表
 * - POST /api/workflows/{id}/execute: 执行指定工作流
 * - GET /api/workflows/executions/{executionId}: 查询执行状态
 * - GET /api/workflows/executions/running: 获取运行中的执行
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
@CrossOrigin(origins = "*")
public class WorkflowController {
    
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionService workflowExecutionService;
    
    @Autowired
    public WorkflowController(WorkflowRepository workflowRepository, 
                             WorkflowExecutionService workflowExecutionService) {
        this.workflowRepository = workflowRepository;
        this.workflowExecutionService = workflowExecutionService;
    }
    
    /**
     * 获取所有工作流列表
     */
    @GetMapping
    public ResponseEntity<List<Workflow>> listWorkflows() {
        log.info("获取工作流列表");
        try {
            List<Workflow> workflows = workflowRepository.findAll();
            log.info("找到 {} 个工作流", workflows.size());
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            log.error("获取工作流列表失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 执行指定工作流
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeWorkflow(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        log.info("执行工作流: {}, 请求参数: {}", id, request);
        
        try {
            // 提取参数
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) request.getOrDefault("parameters", new HashMap<>());
            String executedBy = (String) request.getOrDefault("executedBy", "unknown");
            
            // 启动工作流执行
            WorkflowExecution execution = workflowExecutionService.startWorkflow(
                id.toString(), parameters, executedBy);
            
            // 构造响应
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", execution.getExecutionId());
            response.put("workflowId", execution.getWorkflowId());
            response.put("status", execution.getStatus().toString());
            response.put("startedAt", execution.getStartedAt());
            response.put("totalSteps", execution.getTotalSteps());
            
            log.info("工作流执行已启动: {}", execution.getExecutionId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("工作流执行请求无效: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("工作流执行异常: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 查询工作流执行状态
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<Map<String, Object>> getExecutionStatus(@PathVariable String executionId) {
        log.debug("查询执行状态: {}", executionId);
        
        try {
            WorkflowExecution execution = workflowExecutionService.getExecutionStatus(executionId);
            
            // 构造轻量级响应，避免序列化问题
            Map<String, Object> response = new HashMap<>();
            response.put("executionId", execution.getExecutionId());
            response.put("workflowId", execution.getWorkflowId());
            response.put("status", execution.getStatus().toString());
            response.put("currentStepIndex", execution.getCurrentStepIndex());
            response.put("completedSteps", execution.getCompletedSteps());
            response.put("totalSteps", execution.getTotalSteps());
            response.put("progress", execution.getProgress());
            response.put("startedAt", execution.getStartedAt());
            response.put("completedAt", execution.getCompletedAt());
            response.put("duration", execution.getDuration());
            
            if (execution.getErrorMessage() != null) {
                response.put("errorMessage", execution.getErrorMessage());
            }
            if (execution.getResult() != null) {
                response.put("result", execution.getResult());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("执行实例不存在: {}", executionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("查询执行状态异常: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "查询执行状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * 获取运行中的执行实例
     */
    @GetMapping("/executions/running")
    public ResponseEntity<List<Map<String, Object>>> getRunningExecutions() {
        log.info("获取运行中的执行实例");
        
        try {
            List<WorkflowExecution> runningExecutions = workflowExecutionService.getRunningExecutions();
            
            // 转换为轻量级响应
            List<Map<String, Object>> response = runningExecutions.stream()
                .map(execution -> {
                    Map<String, Object> exec = new HashMap<>();
                    exec.put("executionId", execution.getExecutionId());
                    exec.put("workflowId", execution.getWorkflowId());
                    exec.put("status", execution.getStatus().toString());
                    exec.put("completedSteps", execution.getCompletedSteps());
                    exec.put("totalSteps", execution.getTotalSteps());
                    exec.put("progress", execution.getProgress());
                    exec.put("startedAt", execution.getStartedAt());
                    return exec;
                })
                .toList();
            
            log.info("找到 {} 个运行中的执行实例", response.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取运行中执行实例异常: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 取消工作流执行
     */
    @PostMapping("/executions/{executionId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelExecution(@PathVariable String executionId) {
        log.info("取消工作流执行: {}", executionId);
        
        try {
            boolean cancelled = workflowExecutionService.cancelExecution(executionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("cancelled", cancelled);
            response.put("executionId", executionId);
            
            if (cancelled) {
                log.info("工作流执行已取消: {}", executionId);
                return ResponseEntity.ok(response);
            } else {
                log.warn("无法取消工作流执行: {}", executionId);
                response.put("message", "无法取消执行，可能已经完成或不在运行状态");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            log.error("取消工作流执行异常: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "取消执行失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
