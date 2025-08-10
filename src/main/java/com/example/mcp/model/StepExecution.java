package com.example.mcp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * 步骤执行记录：
 * - 记录某一个步骤的输入参数、输出结果、执行日志与耗时状态
 * - 通过 @JsonIgnore 打断回指 WorkflowExecution，避免序列化递归
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Entity
@Table(name = "step_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 步骤名称
     */
    @Column(nullable = false)
    private String stepName;
    
    /**
     * 步骤类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStep.StepType stepType;
    
    /**
     * 执行状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    
    /**
     * 步骤顺序
     */
    @Column(nullable = false)
    private Integer orderIndex;
    
    /**
     * 输入参数
     */
    @Column(columnDefinition = "TEXT")
    private String inputParameters;
    
    /**
     * 输出结果
     */
    @Column(columnDefinition = "TEXT")
    private String outputResult;
    
    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 开始时间
     */
    @Column(nullable = false)
    private LocalDateTime startedAt;
    
    /**
     * 结束时间
     */
    @Column
    private LocalDateTime completedAt;
    
    /**
     * 执行时长（毫秒）
     */
    @Column
    private Long duration;
    
    /**
     * 重试次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 工具名称（如果是工具步骤）
     */
    @Column
    private String toolName;
    
    /**
     * 执行日志
     */
    @Column(columnDefinition = "TEXT")
    private String executionLog;
    
    /**
     * 所属工作流执行实例
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    @JsonIgnore
    private WorkflowExecution execution;
    
    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED,     // 执行失败
        SKIPPED,    // 已跳过
        CANCELLED,  // 已取消
        TIMEOUT     // 超时
    }
    
    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        if (status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED) {
            completedAt = LocalDateTime.now();
            if (startedAt != null) {
                duration = java.time.Duration.between(startedAt, completedAt).toMillis();
            }
        }
    }
    
    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED;
    }
    
    /**
     * 检查是否成功
     */
    public boolean isSuccessful() {
        return status == ExecutionStatus.COMPLETED;
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return status == ExecutionStatus.FAILED && retryCount < 3;
    }
    
    /**
     * 添加执行日志
     */
    public void addLog(String message) {
        if (executionLog == null) {
            executionLog = "";
        }
        executionLog += LocalDateTime.now() + " - " + message + "\n";
    }
} 