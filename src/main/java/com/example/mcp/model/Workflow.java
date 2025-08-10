package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流定义模型：
 * - 描述一个可执行的业务流程，由多个步骤（WorkflowStep）组成
 * - 仅承载定义信息（名称、描述、步骤等），不包含执行状态
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Entity
@Table(name = "workflows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 工作流名称
     */
    @Column(nullable = false)
    private String name;
    
    /**
     * 工作流描述
     */
    @Column(length = 1000)
    private String description;
    
    /**
     * 工作流版本
     */
    @Column(nullable = false)
    private String version;
    
    /**
     * 工作流状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;
    
    /**
     * 工作流步骤
     */
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<WorkflowStep> steps;
    
    /**
     * 工作流参数
     */
    @Column(columnDefinition = "TEXT")
    private String parameters;
    
    /**
     * 工作流配置
     */
    @Column(columnDefinition = "TEXT")
    private String config;
    
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
     * 创建者
     */
    @Column(nullable = false)
    private String createdBy;
    
    /**
     * 工作流状态枚举
     */
    public enum WorkflowStatus {
        DRAFT,      // 草稿
        ACTIVE,     // 激活
        INACTIVE,   // 非激活
        DEPRECATED  // 已弃用
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = WorkflowStatus.DRAFT;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 