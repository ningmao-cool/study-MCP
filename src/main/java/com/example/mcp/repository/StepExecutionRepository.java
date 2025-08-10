package com.example.mcp.repository;

import com.example.mcp.model.StepExecution;
import com.example.mcp.model.WorkflowExecution;
import com.example.mcp.model.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 步骤执行数据访问接口
 *
 * 提供工作流步骤执行记录的数据访问功能：
 * - 步骤执行记录的CRUD操作
 * - 按执行实例、状态、工具名称、步骤类型查询
 * - 提供失败步骤查询和执行统计功能
 * - 支持性能分析（最长执行时间统计）
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Repository
public interface StepExecutionRepository extends JpaRepository<StepExecution, Long> {
    
    /**
     * 根据执行实例查找步骤执行记录
     */
    List<StepExecution> findByExecution(WorkflowExecution execution);
    
    /**
     * 根据执行实例和状态查找步骤执行记录
     */
    List<StepExecution> findByExecutionAndStatus(WorkflowExecution execution, StepExecution.ExecutionStatus status);
    
    /**
     * 根据工具名称查找步骤执行记录
     */
    List<StepExecution> findByToolName(String toolName);
    
    /**
     * 根据步骤类型查找步骤执行记录
     */
    List<StepExecution> findByStepType(WorkflowStep.StepType stepType);
    
    /**
     * 查找失败的步骤执行记录
     */
    @Query("SELECT s FROM StepExecution s WHERE s.status = 'FAILED'")
    List<StepExecution> findFailedStepExecutions();
    
    /**
     * 统计步骤执行状态
     */
    @Query("SELECT s.status, COUNT(s) FROM StepExecution s GROUP BY s.status")
    List<Object[]> countByStatus();
    
    /**
     * 查找执行时间最长的步骤
     */
    @Query("SELECT s FROM StepExecution s WHERE s.duration IS NOT NULL ORDER BY s.duration DESC")
    List<StepExecution> findLongestExecutingSteps();
} 