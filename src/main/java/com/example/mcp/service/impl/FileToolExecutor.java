package com.example.mcp.service.impl;

import com.example.mcp.model.McpTool;
import com.example.mcp.service.McpToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * create_file 工具执行器
 *
 * 功能：创建/写入指定路径的文件。
 * 参数：
 * - path: 文件相对路径（相对项目根）
 * - content: 文件内容（可选）
 *
 * 返回：
 * - { path, size, created }
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@Service
public class FileToolExecutor implements McpToolExecutor {
    
    private static final String TOOL_NAME = "create_file";
    private static final String TOOL_DESCRIPTION = "创建新文件";
    
    @Override
    public String getToolName() {
        return TOOL_NAME;
    }
    
    @Override
    public String getToolDescription() {
        return TOOL_DESCRIPTION;
    }
    
    @Override
    public McpTool getToolModel() {
        return McpTool.createFileTool();
    }
    
    /** 执行文件创建/写入 */
    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始执行文件创建工具，参数: {}", parameters);
            
            // 验证参数
            if (!validateParameters(parameters)) {
                log.warn("参数验证失败");
                return ToolExecutionResult.failure("参数验证失败", System.currentTimeMillis() - startTime);
            }
            
            String path = (String) parameters.get("path");
            String content = (String) parameters.get("content");
            
            log.info("解析参数 - path: {}, content: {}", path, content);
            
            // 创建文件路径
            Path filePath = Paths.get(path);
            
            // 确保父目录存在
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 写入文件内容
            Files.write(filePath, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            log.info("文件创建成功: {}", path);
            
            Map<String, Object> result = Map.of(
                "path", path,
                "size", content.length(),
                "created", true
            );
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("工具执行完成，耗时: {}ms", executionTime);
            
            return ToolExecutionResult.success(result, executionTime);
            
        } catch (IOException e) {
            log.error("文件创建失败: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("文件创建失败: " + e.getMessage(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("工具执行异常: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("工具执行异常: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 检查必需参数
        if (!parameters.containsKey("path") || !parameters.containsKey("content")) {
            return false;
        }
        
        String path = (String) parameters.get("path");
        String content = (String) parameters.get("content");
        
        // 验证路径不为空
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        
        // 验证内容不为空
        if (content == null) {
            return false;
        }
        
        return true;
    }
} 