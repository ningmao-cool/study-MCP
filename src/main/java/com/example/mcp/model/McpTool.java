package com.example.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * 工具元数据模型：
 * - 向前端/调用方暴露工具名称、描述、分类、输入 schema 等
 * - 与具体执行器（McpToolExecutor 实现）相互独立
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpTool {
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具描述
     */
    private String description;
    
    /**
     * 输入参数模式
     *
     * 使用类似 JSON Schema 的格式定义了调用此工具所需要的输入参数的结构和约束。
     * "type": "object": 表示输入参数应该是一个 JSON 对象。
     * "properties": {...}: 定义了该对象中可以包含的每个参数的名称、类型和描述。
     * "required": [...]: 列出了哪些参数是必须提供的。
     */
    private Map<String, Object> inputSchema;
    
    /**
     * 工具分类
     */
    private String category;
    
    /**
     * 工具标签
     */
    private List<String> tags;
    
    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * 工具版本
     */
    private String version;
    
    /**
     * 工具作者
     */
    private String author;
    
    /**
     * 工具配置
     *
     * 用于存储那些不属于调用参数、但对工具执行很重要的配置信息（例如，API 密钥、默认路径、超时设置等）。
     */
    private Map<String, Object> config;
    
    /**
     * 创建文件操作工具
     */
    public static McpTool createFileTool() {
        return McpTool.builder()
                .name("create_file")
                .description("创建新文件")
                .category("file")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "path", Map.of("type", "string", "description", "文件路径"),
                        "content", Map.of("type", "string", "description", "文件内容")
                    ),
                    "required", List.of("path", "content")
                ))
                .tags(List.of("file", "create"))
                .version("1.0.0")
                .author("MCP Learning Project")
                .build();
    }
    
    /**
     * 创建搜索工具
     */
    public static McpTool createSearchTool() {
        return McpTool.builder()
                .name("search_codebase")
                .description("搜索代码库")
                .category("search")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "搜索查询"),
                        "fileType", Map.of("type", "string", "description", "文件类型过滤"),
                        "includeDirs", Map.of("type", "array", "items", Map.of("type", "string"), "description", "仅搜索这些目录"),
                        "excludeDirs", Map.of("type", "array", "items", Map.of("type", "string"), "description", "额外排除的目录名")
                    ),
                    "required", List.of("query")
                ))
                .tags(List.of("search", "codebase"))
                .version("1.0.0")
                .author("MCP Learning Project")
                .build();
    }
    
    /**
     * 创建工作流工具
     */
    public static McpTool createWorkflowTool() {
        return McpTool.builder()
                .name("execute_workflow")
                .description("执行工作流")
                .category("workflow")
                .inputSchema(Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "workflowId", Map.of("type", "string", "description", "工作流ID"),
                        "parameters", Map.of("type", "object", "description", "工作流参数")
                    ),
                    "required", List.of("workflowId")
                ))
                .tags(List.of("workflow", "execution"))
                .version("1.0.0")
                .author("MCP Learning Project")
                .build();
    }
} 