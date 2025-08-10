package com.example.mcp.repository;

import com.example.mcp.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工作流数据访问接口
 *
 * 提供工作流定义的CRUD操作和查询功能：
 * - 基础的增删改查操作
 * - 按状态、创建者、名称等条件查询
 * - 查询激活状态的工作流
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
    
    /**
     * 根据状态查找工作流·
     */
    List<Workflow> findByStatus(Workflow.WorkflowStatus status);
    
    /**
     * 根据创建者查找工作流
     */
    List<Workflow> findByCreatedBy(String createdBy);
    
    /**
     * 根据名称查找工作流
     */
    List<Workflow> findByNameContainingIgnoreCase(String name);
    
    /**
     * 查找激活的工作流
     */
    @Query("SELECT w FROM Workflow w WHERE w.status = 'ACTIVE'")
    List<Workflow> findActiveWorkflows();
    
    /**
     * 根据版本查找工作流
     */
    List<Workflow> findByVersion(String version);
} 