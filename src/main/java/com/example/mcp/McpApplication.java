package com.example.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP学习项目主应用入口类：
 * - 基于Spring Boot的Web应用
 * - 实现MCP协议的工具管理和工作流编排功能
 * - 提供REST API和Web界面
 *
 * @author NingMao
 * @since 2025-08-09
 */
@SpringBootApplication
public class McpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }

}
