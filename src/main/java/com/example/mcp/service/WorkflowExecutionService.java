package com.example.mcp.service;

import com.example.mcp.model.*;
import com.example.mcp.repository.WorkflowExecutionRepository;
import com.example.mcp.repository.WorkflowRepository;
import com.example.mcp.repository.StepExecutionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流执行服务
 *
 * 职责：
 * - 创建与持久化工作流执行实例（WorkflowExecution）
 * - 异步按序执行步骤（工具、条件、延迟等），并更新执行状态/进度
 * - 提供执行状态查询
 *
 * 关键点：
 * - 使用 afterCommit 触发异步执行，避免读到未提交数据
 * - 条件步骤会读取上一个"已完成"的步骤输出进行简单表达式评估
 * - 对步骤输入/输出使用 ObjectMapper 进行 JSON 序列化，避免直接 toString 带来的格式问题
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@Service
public class WorkflowExecutionService {
    
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final McpService mcpService;
    
    // 正在执行的工作流缓存
    private final Map<String, WorkflowExecution> runningExecutions = new ConcurrentHashMap<>();
    
    @Autowired
    public WorkflowExecutionService(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository,
            StepExecutionRepository stepExecutionRepository,
            McpService mcpService) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.stepExecutionRepository = stepExecutionRepository;
        this.mcpService = mcpService;
    }
    
    /**
     * 启动一个工作流执行，返回持久化后的执行实体。
     * 注意：实际异步执行在事务提交后开始（afterCommit）。
     */
    @Transactional
    public WorkflowExecution startWorkflow(String workflowId, Map<String, Object> parameters, String executedBy) {
        log.info("启动工作流执行: workflowId={}, executedBy={}", workflowId, executedBy);
        
        // 查找工作流
        Workflow workflow = workflowRepository.findById(Long.valueOf(workflowId))
                .orElseThrow(() -> new IllegalArgumentException("工作流不存在: " + workflowId));
        
        // 检查工作流状态
        if (workflow.getStatus() != Workflow.WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("工作流未激活: " + workflowId);
        }
        
        // 创建执行实例
        String executionId = UUID.randomUUID().toString();
        String paramJson;
        try {
            paramJson = parameters != null ? new ObjectMapper().writeValueAsString(parameters) : "{}";
        } catch (Exception ex) {
            paramJson = parameters != null ? parameters.toString() : "{}";
        }
        WorkflowExecution execution = WorkflowExecution.builder()
                .executionId(executionId)
                .workflowId(workflow.getId())
                .status(WorkflowExecution.ExecutionStatus.PENDING)
                .parameters(paramJson)
                .executedBy(executedBy)
                .totalSteps(workflow.getSteps().size())
                .build();
        
        // 保存执行实例
        execution = executionRepository.save(execution);
        
        // 在事务提交后再异步启动执行，避免读取不到刚保存的数据
        final String capturedExecutionId = executionId;
        final Long capturedWorkflowId = Long.valueOf(workflowId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                log.info("事务已提交，开始异步执行工作流: {}", capturedExecutionId);
                CompletableFuture.runAsync(() -> executeWorkflow(capturedExecutionId, capturedWorkflowId));
            }
        });
        
        return execution;
    }
    
    /**
     * 真正执行工作流（异步），依序执行步骤，处理异常与重试，维护当前索引和进度。
     */
    @Transactional
    public void executeWorkflow(String executionId, Long workflowId) {
        log.info("开始执行工作流: {}", executionId);
        try {
            // 在事务中重新加载实体
            WorkflowExecution execution = executionRepository.findByExecutionId(executionId)
                    .orElseThrow(() -> new IllegalArgumentException("执行实例不存在: " + executionId));
            Workflow workflow = workflowRepository.findById(workflowId)
                    .orElseThrow(() -> new IllegalArgumentException("工作流不存在: " + workflowId));
            // 更新状态为运行中
            execution.setStatus(WorkflowExecution.ExecutionStatus.RUNNING);
            executionRepository.save(execution);
            runningExecutions.put(executionId, execution);
            
            // 按顺序执行步骤
            List<WorkflowStep> steps = workflow.getSteps();
            steps.sort(Comparator.comparing(WorkflowStep::getOrderIndex));
            
            for (int i = 0; i < steps.size(); i++) {
                WorkflowStep step = steps.get(i);
                
                // 更新当前步骤
                execution.setCurrentStepIndex(i);
                executionRepository.save(execution);
                
                // 执行步骤
                boolean stepSuccess = executeStep(execution, step);
                
                if (stepSuccess) {
                    execution.setCompletedSteps(execution.getCompletedSteps() + 1);
                } else {
                    // 步骤执行失败，停止工作流
                    execution.setStatus(WorkflowExecution.ExecutionStatus.FAILED);
                    execution.setErrorMessage("步骤执行失败: " + step.getName());
                    executionRepository.save(execution);
                    runningExecutions.remove(executionId);
                    return;
                }
            }
            
            // 所有步骤执行完成
            execution.setStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
            execution.setResult("工作流执行成功");
            executionRepository.save(execution);
            
            log.info("工作流执行完成: {}", executionId);
            
        } catch (Exception e) {
            log.error("工作流执行异常: {}", e.getMessage(), e);
            // 无法加载实体或其它异常时记录并返回
        } finally {
            runningExecutions.remove(executionId);
        }
    }
    
    /**
     * 执行单个步骤
     */
    private boolean executeStep(WorkflowExecution execution, WorkflowStep step) {
        String stepName = step.getName();
        log.info("执行步骤: {}", stepName);
        
        // 创建步骤执行记录
        StepExecution stepExecution = StepExecution.builder()
                .stepName(stepName)
                .stepType(step.getType())
                .orderIndex(step.getOrderIndex())
                .status(StepExecution.ExecutionStatus.RUNNING)
                .execution(execution)
                .toolName(step.getToolName())
                .build();
        
        stepExecution = stepExecutionRepository.save(stepExecution);
        
        try {
            boolean success = false;
            
            switch (step.getType()) {
                case TOOL:
                    success = executeToolStep(stepExecution, step);
                    break;
                case CONDITION:
                    success = executeConditionStep(stepExecution, step);
                    break;
                case DELAY:
                    success = executeDelayStep(stepExecution, step);
                    break;
                default:
                    stepExecution.setErrorMessage("不支持的步骤类型: " + step.getType());
                    success = false;
            }
            
            // 更新步骤执行状态
            stepExecution.setStatus(success ? StepExecution.ExecutionStatus.COMPLETED : StepExecution.ExecutionStatus.FAILED);
            stepExecutionRepository.save(stepExecution);
            
            return success;
            
        } catch (Exception e) {
            log.error("步骤执行异常: {}", e.getMessage(), e);
            stepExecution.setStatus(StepExecution.ExecutionStatus.FAILED);
            stepExecution.setErrorMessage("执行异常: " + e.getMessage());
            stepExecutionRepository.save(stepExecution);
            return false;
        }
    }
    
    /**
     * 执行工具步骤：
     * - 从步骤配置与参数解析输入
     * - 通过 McpService 找到具体工具执行器
     * - 记录输入输出与日志
     */
    private boolean executeToolStep(StepExecution stepExecution, WorkflowStep step) {
        String toolName = step.getToolName();
        if (toolName == null) {
            stepExecution.setErrorMessage("工具名称不能为空");
            return false;
        }
        
        // 检查工具是否存在
        if (!mcpService.hasTool(toolName)) {
            stepExecution.setErrorMessage("工具不存在: " + toolName);
            return false;
        }
        
        // 获取工具参数
        Map<String, Object> parameters = parseParameters(step.getParameters());
        // 合并执行级参数（可覆盖步骤内参数）
        Map<String, Object> execParams = parseParameters(stepExecution.getExecution().getParameters());
        if (execParams != null && !execParams.isEmpty()) {
            parameters.putAll(execParams);
        }
        
        // 执行工具
        McpToolExecutor.ToolExecutionResult result = mcpService.getToolExecutor(toolName).execute(parameters);
        
        // 记录结果
        try {
            stepExecution.setInputParameters(new ObjectMapper().writeValueAsString(parameters));
        } catch (Exception ex) {
            stepExecution.setInputParameters(parameters.toString());
        }
        try {
            Object res = result.getResult();
            String resJson = res != null ? new ObjectMapper().writeValueAsString(res) : "";
            stepExecution.setOutputResult(resJson);
        } catch (Exception ex) {
            stepExecution.setOutputResult(result.getResult() != null ? result.getResult().toString() : "");
        }
        
        if (result.isSuccess()) {
            stepExecution.addLog("工具执行成功: " + toolName);
            return true;
        } else {
            stepExecution.setErrorMessage(result.getErrorMessage());
            stepExecution.addLog("工具执行失败: " + toolName + " - " + result.getErrorMessage());
            return false;
        }
    }
    
    /**
     * 执行条件步骤：
     * - 读取该执行实例下最后一个已完成步骤的输出
     * - 支持基于 resultCount 的简单表达式（如：resultCount > 0）
     * 这里是永远为真的，我们就只需要知道条件步骤是根据上一个步骤的执行结果来决定是否继续执行
     */
    private boolean executeConditionStep(StepExecution stepExecution, WorkflowStep step) {
        String condition = step.getCondition();
        if (condition == null || condition.trim().isEmpty()) {
            stepExecution.setErrorMessage("条件表达式不能为空");
            return false;
        }
        
        // 支持引用上一步结果的简单表达式（如：resultCount > 0）
        int resultCount = 0;
        try {
            List<StepExecution> steps = stepExecutionRepository.findByExecution(stepExecution.getExecution());
            // 取该执行实例下“最后一个已完成”的步骤（按 orderIndex 最大）
            StepExecution prevCompleted = steps.stream()
                    .filter(s -> s.getStatus() == StepExecution.ExecutionStatus.COMPLETED)
                    .max(Comparator.comparing(StepExecution::getOrderIndex))
                    .orElse(null);
            if (prevCompleted != null) {
                resultCount = extractTotalCountFromOutput(prevCompleted.getOutputResult());
            }
        } catch (Exception ignore) { }
        boolean result = evaluateCondition(condition, resultCount);
        
        stepExecution.setOutputResult("条件评估结果: " + result + ", resultCount=" + resultCount);
        stepExecution.addLog("条件步骤执行: " + condition + " = " + result);
        
        return result;
    }
    
    /**
     * 执行延迟步骤：
     * - 从 config 的 delay 字段读取毫秒值（Number 或 String），进行安全转换后 sleep
     */
    private boolean executeDelayStep(StepExecution stepExecution, WorkflowStep step) {
        try {
            // 解析延迟配置
            Map<String, Object> config = parseParameters(step.getConfig());
            Object delayObj = config.get("delay");
            Long delayMs = null;
            if (delayObj instanceof Number n) {
                delayMs = n.longValue();
            } else if (delayObj != null) {
                try { delayMs = Long.parseLong(delayObj.toString()); } catch (Exception ignore) {}
            }
            
            if (delayMs == null || delayMs <= 0) {
                stepExecution.setErrorMessage("无效的延迟时间");
                return false;
            }
            
            stepExecution.addLog("开始延迟: " + delayMs + "ms");
            Thread.sleep(delayMs);
            stepExecution.addLog("延迟完成");
            
            stepExecution.setOutputResult("延迟执行完成: " + delayMs + "ms");
            return true;
            
        } catch (InterruptedException e) {
            stepExecution.setErrorMessage("延迟被中断: " + e.getMessage());
            return false;
        } catch (Exception e) {
            stepExecution.setErrorMessage("延迟执行异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 解析简单条件表达式。目前支持：
     * - resultCount > N / >= / == / != / < / <= N
     */
    private boolean evaluateCondition(String condition, int resultCount) {
        String expr = condition.trim().replaceAll("\\s+", " ");
        try {
            if (expr.matches("(?i)resultCount\\s*>\\s*\\d+")) {
                int n = Integer.parseInt(expr.replaceAll("(?i)resultCount\\s*>\\s*", ""));
                return resultCount > n;
            }
            if (expr.matches("(?i)resultCount\\s*>=\\s*\\d+")) {
                int n = Integer.parseInt(expr.replaceAll("(?i)resultCount\\s*>=\\s*", ""));
                return resultCount >= n;
            }
            if (expr.matches("(?i)resultCount\\s*==\\s*\\d+")) {
                int n = Integer.parseInt(expr.replaceAll("(?i)resultCount\\s*==\\s*", ""));
                return resultCount == n;
            }
            if (expr.matches("(?i)resultCount\\s*<=\\s*\\d+")) {
                int n = Integer.parseInt(expr.replaceAll("(?i)resultCount\\s*<=\\s*", ""));
                return resultCount <= n;
            }
            if (expr.matches("(?i)resultCount\\s*<\\s*\\d+")) {
                int n = Integer.parseInt(expr.replaceAll("(?i)resultCount\\s*<\\s*", ""));
                return resultCount < n;
            }
        } catch (Exception ignore) {}
        // 兜底：true/1
        return expr.toLowerCase().contains("true") || expr.contains("1");
    }

    private int extractTotalCountFromOutput(String outputJson) {
        if (outputJson == null || outputJson.isBlank()) return 0;
        try {
            var node = new ObjectMapper().readTree(outputJson);
            if (node.has("totalCount")) return node.get("totalCount").asInt(0);
        } catch (Exception ignore) {}
        return 0;
    }
    
    /**
     * 解析参数字符串为Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParameters(String parameters) {
        if (parameters == null || parameters.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // 使用Jackson解析JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(parameters, Map.class);
        } catch (Exception e) {
            log.warn("参数解析失败: {}", parameters, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 供控制器轮询查询指定 executionId 的最新状态。
     */
    public WorkflowExecution getExecutionStatus(String executionId) {
        return executionRepository.findByExecutionId(executionId)
                .orElseThrow(() -> new IllegalArgumentException("执行实例不存在: " + executionId));
    }
    
    /**
     * 取消执行
     */
    @Transactional
    public boolean cancelExecution(String executionId) {
        WorkflowExecution execution = getExecutionStatus(executionId);
        
        if (execution.getStatus() == WorkflowExecution.ExecutionStatus.RUNNING) {
            execution.setStatus(WorkflowExecution.ExecutionStatus.CANCELLED);
            executionRepository.save(execution);
            runningExecutions.remove(executionId);
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取正在执行的实例
     */
    public List<WorkflowExecution> getRunningExecutions() {
        return new ArrayList<>(runningExecutions.values());
    }
    
    /**
     * 获取执行历史
     */
    public List<WorkflowExecution> getExecutionHistory(Long workflowId, int limit) {
        return executionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId, 
            org.springframework.data.domain.PageRequest.of(0, limit));
    }
} 