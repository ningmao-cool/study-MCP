package com.example.mcp.config;

import com.example.mcp.model.Workflow;
import com.example.mcp.model.WorkflowStep;
import com.example.mcp.repository.WorkflowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 应用启动时初始化演示用工作流与工具配置。
 *
 * 功能：
 * - 实现 CommandLineRunner 接口，在应用启动后自动执行
 * - 检查数据库中是否已有工作流数据，避免重复初始化
 * - 创建三个示例工作流：
 *   1) 文件处理工作流：创建文件 -> 延迟 -> 搜索该文件内容
 *   2) 代码搜索工作流：在源码中搜索 class 关键字 -> 条件判断 -> 延迟处理
 *   3) 工作流执行工作流：演示工作流嵌套执行
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {
    
    private final WorkflowRepository workflowRepository;
    
    @Autowired
    public DataInitializer(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化示例数据...");
        
        // 创建示例工作流
        createSampleWorkflows();
        
        log.info("示例数据初始化完成");
    }
    
    /**
     * 创建示例工作流
     */
    private void createSampleWorkflows() {
        // 检查是否已有数据
        if (workflowRepository.count() > 0) {
            log.info("数据库中已有工作流数据，跳过初始化");
            return;
        }
        
        // 示例工作流1：文件处理工作流
        Workflow fileProcessingWorkflow = Workflow.builder()
                .name("文件处理工作流")
                .description("创建文件并搜索内容的示例工作流")
                .version("1.0.0")
                .status(Workflow.WorkflowStatus.ACTIVE)
                .createdBy("system")
                .steps(Arrays.asList(
                    WorkflowStep.builder()
                        .name("创建示例文件")
                        .type(WorkflowStep.StepType.TOOL)
                        .toolName("create_file")
                        .orderIndex(1)
                        .parameters("{\"path\": \"sample.txt\", \"content\": \"这是一个示例文件内容\\n用于测试工作流执行。\"}")
                        .build(),
                    WorkflowStep.createDelayStep("等待处理", 2000L, 2),
                    WorkflowStep.builder()
                        .name("搜索文件内容")
                        .type(WorkflowStep.StepType.TOOL)
                        .toolName("search_codebase")
                        .orderIndex(3)
                        .parameters("{\"query\": \"示例文件\", \"fileType\": \"txt\", \"includeDirs\": [\".\"]}")
                        .build()
                ))
                .build();
        
        // 设置步骤的工作流关联
        fileProcessingWorkflow.getSteps().forEach(step -> step.setWorkflow(fileProcessingWorkflow));
        
        // 示例工作流2：简单搜索工作流
        Workflow searchWorkflow = Workflow.builder()
                .name("代码搜索工作流")
                .description("搜索代码库中的特定内容")
                .version("1.0.0")
                .status(Workflow.WorkflowStatus.ACTIVE)
                .createdBy("system")
                .steps(Arrays.asList(
                    WorkflowStep.builder()
                        .name("搜索Java文件")
                        .type(WorkflowStep.StepType.TOOL)
                        .toolName("search_codebase")
                        .orderIndex(1)
                        .parameters("{\"query\": \"class\", \"fileType\": \"java\"}")
                        .build(),
                    WorkflowStep.createConditionStep("检查搜索结果", 2),
                    WorkflowStep.createDelayStep("处理延迟", 1000L, 3)
                ))
                .build();
        
        // 设置步骤的工作流关联
        searchWorkflow.getSteps().forEach(step -> step.setWorkflow(searchWorkflow));
        
        // 示例工作流3：工作流执行工作流
        Workflow workflowExecutionWorkflow = Workflow.builder()
                .name("工作流执行工作流")
                .description("演示工作流嵌套执行")
                .version("1.0.0")
                .status(Workflow.WorkflowStatus.ACTIVE)
                .createdBy("system")
                .steps(Arrays.asList(
                    WorkflowStep.builder()
                        .name("执行文件工作流")
                        .type(WorkflowStep.StepType.TOOL)
                        .toolName("execute_workflow")
                        .orderIndex(1)
                        .parameters("{\"workflowId\": \"1\", \"parameters\": {}}")
                        .build(),
                    WorkflowStep.createDelayStep("等待完成", 5000L, 2),
                    WorkflowStep.builder()
                        .name("执行搜索工作流")
                        .type(WorkflowStep.StepType.TOOL)
                        .toolName("execute_workflow")
                        .orderIndex(3)
                        .parameters("{\"workflowId\": \"2\", \"parameters\": {}}")
                        .build()
                ))
                .build();
        
        // 设置步骤的工作流关联
        workflowExecutionWorkflow.getSteps().forEach(step -> step.setWorkflow(workflowExecutionWorkflow));
        
        // 保存工作流
        List<Workflow> workflows = Arrays.asList(
            fileProcessingWorkflow,
            searchWorkflow,
            workflowExecutionWorkflow
        );
        
        workflowRepository.saveAll(workflows);
        
        log.info("创建了 {} 个示例工作流", workflows.size());
    }
} 