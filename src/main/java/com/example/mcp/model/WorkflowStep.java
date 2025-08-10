package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流步骤定义：
 * - TOOL：调用某个工具（如 search_codebase）
 * - CONDITION：基于上一步输出进行条件判断，决定是否继续
 * - DELAY：延迟一定时间后继续
 *
 * @author NingMao
 * @since 2025-08-09
 *
 */
@Entity
@Table(name = "workflow_steps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 步骤名称
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 步骤描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 步骤类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepType type;
    
    /**
     * 步骤顺序
     */
    @Column(nullable = false)
    private Integer orderIndex;
    
    // 注意：步骤状态已移除，因为这是步骤定义而非执行实例
    // 实际的执行状态由 StepExecution.status 来跟踪
    
    /**
     * 工具名称（如果是工具步骤）
     */
    @Column
    private String toolName;
    
    /**
     * 步骤参数
     */
    @Column(columnDefinition = "TEXT")
    @JsonProperty
    private String parameters;
    
    /**
     * 步骤配置
     */
    @Column(columnDefinition = "TEXT")
    private String config;
    
    /**
     * 条件表达式（用于条件步骤），执行引擎会评估这个表达式来决定流程走向。
     */
    @Column(columnDefinition = "TEXT")
    private String condition;
    
    /**
     * 重试次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
    
    /**
     * 重试延迟（毫秒）
     */
    @Column(nullable = false)
    @Builder.Default
    private Long retryDelay = 1000L;
    
    /**
     * 超时时间（毫秒）
     */
    @Column(nullable = false)
    @Builder.Default
    private Long timeout = 30000L;
    
    /**
     * 所属工作流
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonBackReference
    private Workflow workflow;
    
    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * 步骤类型枚举
     */
    public enum StepType {
        TOOL,       // 工具步骤
        CONDITION,  // 条件步骤
        LOOP,       // 循环步骤
        PARALLEL,   // 并行步骤
        DELAY,      // 延迟步骤
        CUSTOM      // 自定义步骤
    }
    
    // 步骤状态枚举已移除 - 使用 StepExecution.ExecutionStatus 代替
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // 移除了状态初始化，因为步骤定义不需要状态
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 创建工具步骤
     */
    public static WorkflowStep createToolStep(String name, String toolName, Integer orderIndex) {
        return WorkflowStep.builder()
                .name(name)
                .type(StepType.TOOL)
                .toolName(toolName)
                .orderIndex(orderIndex)
                .build();
    }
    
    /**
     * 创建条件步骤
     * 条件步骤会根据前一个步骤的执行结果来决定是否继续执行
     */
    public static WorkflowStep createConditionStep(String name, Integer orderIndex) {
        return WorkflowStep.builder()
                .name(name)
                .type(StepType.CONDITION)
                .orderIndex(orderIndex)
                .build();
    }
    
    /**
     * 创建延迟步骤
     */
    public static WorkflowStep createDelayStep(String name, Long delayMs, Integer orderIndex) {
        return WorkflowStep.builder()
                .name(name)
                .type(StepType.DELAY)
                .config("{\"delay\": " + delayMs + "}")
                .orderIndex(orderIndex)
                .build();
    }
} 