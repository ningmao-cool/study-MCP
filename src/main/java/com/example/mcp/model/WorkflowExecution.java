package com.example.mcp.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行实例：
 * - 表示一次具体的执行（有 executionId）
 * - 持有当前进度、状态与汇总信息
 * - 与多个 StepExecution 关联（单向输出为避免递归序列化在字段上 @JsonIgnore）
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Entity
@Table(name = "workflow_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 执行ID（唯一标识）
     */
    @Column(nullable = false, unique = true)
    private String executionId;
    
    /**
     * 工作流ID
     */
    @Column(nullable = false)
    private Long workflowId;
    
    /**
     * 执行状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    
    /**
     * 执行参数
     */
    @Column(columnDefinition = "TEXT")
    private String parameters;
    
    /**
     * 执行结果
     */
    @Column(columnDefinition = "TEXT")
    private String result;
    
    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 当前步骤索引
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer currentStepIndex = 0;
    
    /**
     * 已完成的步骤数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer completedSteps = 0;
    
    /**
     * 总步骤数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer totalSteps = 0;
    
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
     * 执行者
     */
    @Column(nullable = false)
    private String executedBy;
    
    /**
     * 执行环境
     */
    @Column
    private String environment;
    
    /**
     * 执行标签
     */
    @Column
    private String tags;
    
    /**
     * 执行优先级
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;
    
    /**
     * 重试次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * 执行步骤记录
     */
    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<StepExecution> stepExecutions;
    
    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED,     // 执行失败
        CANCELLED,  // 已取消
        TIMEOUT,    // 超时
        RETRYING    // 重试中
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
     * 计算执行进度
     */
    public double getProgress() {
        if (totalSteps == 0) {
            return 0.0;
        }
        return (double) completedSteps / totalSteps * 100.0;
    }
    
    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED;
    }
    
    /**
     * 检查是否可以重试
     */
    public boolean canRetry() {
        return status == ExecutionStatus.FAILED && retryCount < maxRetries;
    }
} 