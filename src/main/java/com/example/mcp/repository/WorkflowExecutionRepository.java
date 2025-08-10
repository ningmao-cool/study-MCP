package com.example.mcp.repository;

import com.example.mcp.model.WorkflowExecution;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工作流执行数据访问接口
 *
 * 提供工作流执行实例的数据访问功能：
 * - 执行记录的CRUD操作
 * - 按执行ID、工作流ID、状态、执行者等条件查询
 * - 支持时间范围查询和分页查询
 * - 提供执行状态统计功能
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    
    /**
     * 根据执行ID查找
     */
    Optional<WorkflowExecution> findByExecutionId(String executionId);
    
    /**
     * 根据工作流ID查找执行记录
     */
    List<WorkflowExecution> findByWorkflowId(Long workflowId);
    
    /**
     * 根据工作流ID查找执行记录（按开始时间倒序）
     */
    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(Long workflowId, Pageable pageable);
    
    /**
     * 根据状态查找执行记录
     */
    List<WorkflowExecution> findByStatus(WorkflowExecution.ExecutionStatus status);
    
    /**
     * 根据执行者查找执行记录
     */
    List<WorkflowExecution> findByExecutedBy(String executedBy);
    
    /**
     * 查找指定时间范围内的执行记录
     */
    List<WorkflowExecution> findByStartedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找失败的执行记录
     */
    @Query("SELECT e FROM WorkflowExecution e WHERE e.status IN ('FAILED', 'TIMEOUT')")
    List<WorkflowExecution> findFailedExecutions();
    
    /**
     * 统计执行状态
     */
    @Query("SELECT e.status, COUNT(e) FROM WorkflowExecution e GROUP BY e.status")
    List<Object[]> countByStatus();
    
    /**
     * 查找运行中的执行记录
     */
    @Query("SELECT e FROM WorkflowExecution e WHERE e.status = 'RUNNING'")
    List<WorkflowExecution> findRunningExecutions();
} 