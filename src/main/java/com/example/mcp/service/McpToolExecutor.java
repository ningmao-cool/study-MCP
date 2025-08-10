package com.example.mcp.service;

import com.example.mcp.model.McpTool;
import java.util.Map;

/**
 * 所有工具执行器需实现的接口。
 *
 * 约定：
 * - 入参/出参采用 Map 与简单 POJO，便于通过 HTTP JSON 传输
 * - 返回结构统一使用 ToolExecutionResult 封装 success、result、errorMessage、executionTime
 * - 实现类需要标注 @Service 注解以便自动注册到 McpService
 *
 * @author NingMao
 * @since 2025-08-09
 */
public interface McpToolExecutor {
    
    /**
     * 获取工具名称
     */
    String getToolName();
    
    /**
     * 获取工具描述
     */
    String getToolDescription();
    
    /**
     * 获取工具模型
     */
    McpTool getToolModel();
    
    /** 执行工具，传入参数 Map，返回统一结果结构 */
    ToolExecutionResult execute(Map<String, Object> parameters);
    
    /**
     * 验证参数
     * 
     * @param parameters 工具参数
     * @return 验证结果
     */
    boolean validateParameters(Map<String, Object> parameters);
    
    /**
     * 工具执行统一结果：
     * - success: 是否成功
     * - result: 任意类型的业务结果（可为空）
     * - errorMessage: 失败原因（可为空）
     * - executionTime: 执行耗时（毫秒）
     */
    class ToolExecutionResult {
        private final boolean success;
        private final Object result;
        private final String errorMessage;
        private final long executionTime;
        
        public ToolExecutionResult(boolean success, Object result, String errorMessage, long executionTime) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.executionTime = executionTime;
        }
        
        public static ToolExecutionResult success(Object result, long executionTime) {
            return new ToolExecutionResult(true, result, null, executionTime);
        }
        
        public static ToolExecutionResult failure(String errorMessage, long executionTime) {
            return new ToolExecutionResult(false, null, errorMessage, executionTime);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public Object getResult() { return result; }
        public String getErrorMessage() { return errorMessage; }
        public long getExecutionTime() { return executionTime; }
    }
} 