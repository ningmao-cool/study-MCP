package com.example.mcp.service.impl;

import com.example.mcp.model.McpTool;
import com.example.mcp.service.McpToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * search_codebase 工具执行器
 *
 * 参数：
 * - query: 关键词/正则子串
 * - fileType: 文件类型过滤。支持：
 *   - 扩展名：java / js / kt 或多扩展名："java,kt"
 *   - 通配符："*.java" / "*.md"
 * - includeDirs: 仅搜索这些目录（相对项目根），默认 ["src/main/java"]
 * - excludeDirs: 额外排除目录名，如 ["target", ".git"]
 *
 * 特性：
 * - 预置排除目录：.git, target, .idea, node_modules, build, out
 * - 使用 SimpleFileVisitor 深度遍历，并在 preVisitDirectory 阶段跳过排除目录
 *
 * @author NingMao
 * @since 2025-08-09
 */
@Slf4j
@Service
public class SearchToolExecutor implements McpToolExecutor {
    
    private static final String TOOL_NAME = "search_codebase";
    private static final String TOOL_DESCRIPTION = "搜索代码库";
    
    // 默认搜索目录
    private static final String DEFAULT_SEARCH_DIR = ".";
    
    // 默认排除的文件类型
    private static final Set<String> EXCLUDED_EXTENSIONS = Set.of(
        ".git", ".class", ".jar", ".war", ".ear", ".zip", ".tar", ".gz",
        ".png", ".jpg", ".jpeg", ".gif", ".ico", ".pdf", ".doc", ".docx"
    );

    // 默认需要跳过遍历的目录（按目录名匹配）
    private static final Set<String> DEFAULT_EXCLUDED_DIRECTORIES = Set.of(
        ".git", "target", ".idea", "node_modules", "build", "out"
    );
    
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
        return McpTool.createSearchTool();
    }
    
    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 验证参数
            if (!validateParameters(parameters)) {
                return ToolExecutionResult.failure("参数验证失败", System.currentTimeMillis() - startTime);
            }
            
            String query = (String) parameters.get("query");
            String fileType = (String) parameters.get("fileType");
            // 可选：只搜索的目录列表
            List<String> includeDirs = parseStringList(parameters.get("includeDirs"));
            if (includeDirs == null || includeDirs.isEmpty()) {
                includeDirs = List.of("src/main/java");
            }
            // 可选：额外排除的目录列表
            Set<String> extraExcluded = new HashSet<>(parseStringList(parameters.get("excludeDirs")));
            extraExcluded.removeIf(Objects::isNull);
            
            // 执行搜索
            List<SearchResult> results = performSearch(query, fileType, includeDirs, extraExcluded);
            
            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("results", results);
            result.put("totalCount", results.size());
            if (fileType != null) {
                result.put("fileType", fileType);
            }
            result.put("includeDirs", includeDirs);
            if (!extraExcluded.isEmpty()) {
                result.put("excludeDirs", extraExcluded);
            }
            
            log.info("搜索完成，找到 {} 个结果", results.size());
            
            return ToolExecutionResult.success(result, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("搜索执行异常: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("搜索执行异常: " + e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
    
    @Override
    public boolean validateParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return false;
        }
        
        // 检查必需参数
        if (!parameters.containsKey("query")) {
            return false;
        }
        
        String query = (String) parameters.get("query");
        
        // 验证查询不为空
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    // 解析字符串列表参数：支持数组或以逗号分隔的字符串
    private List<String> parseStringList(Object raw) {
        if (raw == null) return List.of();
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o != null) out.add(o.toString());
            }
            return out;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) return List.of();
        String[] parts = s.split(",");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (!p.trim().isEmpty()) out.add(p.trim());
        }
        return out;
    }

    /**
     * 执行搜索
     */
    private List<SearchResult> performSearch(String query, String fileType, List<String> includeDirs, Set<String> extraExcludedDirs) throws IOException {
        List<SearchResult> results = new ArrayList<>();
        // 构建文件过滤器
        PathMatcher fileMatcher = buildFileMatcher(fileType);
        // 合并默认与额外排除
        Set<String> excludedDirNames = new HashSet<>(DEFAULT_EXCLUDED_DIRECTORIES);
        for (String d : extraExcludedDirs) {
            if (d != null && !d.isBlank()) excludedDirNames.add(d);
        }

        for (String base : includeDirs) {
            Path searchDir = Paths.get(base);
            if (!Files.exists(searchDir)) {
                log.warn("包含目录不存在，跳过: {}", base);
                continue;
            }
            final Path rootDir = searchDir;
            Files.walkFileTree(searchDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Path namePath = dir.getFileName();
                    String name = namePath == null ? "" : namePath.toString();
                    // 不要跳过搜索的根目录（例如 "."），其名称也可能是以点开头
                    if (!dir.equals(rootDir) && (name.startsWith(".") || excludedDirNames.contains(name))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (fileMatcher != null && !fileMatcher.matches(file.getFileName())) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (shouldExcludeFile(file)) {
                        return FileVisitResult.CONTINUE;
                    }
                    List<SearchMatch> matches = searchInFile(file, query);
                    if (!matches.isEmpty()) {
                        results.add(new SearchResult(file.toString(), matches));
                    }
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("无法访问文件: {}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return results;
    }
    
    /** 构建文件匹配器：支持多扩展名或直接 glob 表达式 */
    private PathMatcher buildFileMatcher(String fileType) {
        if (fileType == null || fileType.trim().isEmpty()) {
            return null;
        }
        String ft = fileType.trim();
        // 如果用户直接传入通配模式（如: *.java），直接按 glob 使用
        if (ft.startsWith("*")) {
            return FileSystems.getDefault().getPathMatcher("glob:" + ft);
        }
        String pattern;
        if (ft.contains(",")) {
            // 多扩展：java,kt,groovy -> *.{java,kt,groovy}
            String alts = Arrays.stream(ft.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.startsWith(".") ? s.substring(1) : s)
                    .collect(Collectors.joining(","));
            pattern = "*.{" + alts + "}";
        } else {
            // 单扩展：txt -> *.txt
            String ext = ft.startsWith(".") ? ft.substring(1) : ft;
            pattern = "*." + ext;
        }
        return FileSystems.getDefault().getPathMatcher("glob:" + pattern);
    }
    
    /**
     * 检查是否应该排除文件
     */
    private boolean shouldExcludeFile(Path file) {
        String fileName = file.getFileName().toString();
        
        // 检查排除的扩展名
        for (String ext : EXCLUDED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        
        // 检查隐藏文件
        if (fileName.startsWith(".")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 在文件中搜索
     */
    private List<SearchMatch> searchInFile(Path file, String query) throws IOException {
        List<SearchMatch> matches = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(file);
            Pattern pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE);
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (pattern.matcher(line).find()) {
                    matches.add(new SearchMatch(i + 1, line.trim()));
                }
            }
        } catch (IOException e) {
            log.warn("无法读取文件: {}", file);
        }
        
        return matches;
    }
    
    /**
     * 搜索结果
     */
    public static class SearchResult {
        private final String filePath;
        private final List<SearchMatch> matches;
        
        public SearchResult(String filePath, List<SearchMatch> matches) {
            this.filePath = filePath;
            this.matches = matches;
        }
        
        public String getFilePath() { return filePath; }
        public List<SearchMatch> getMatches() { return matches; }
    }
    
    /**
     * 搜索匹配
     */
    public static class SearchMatch {
        private final int lineNumber;
        private final String lineContent;
        
        public SearchMatch(int lineNumber, String lineContent) {
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
        }
        
        public int getLineNumber() { return lineNumber; }
        public String getLineContent() { return lineContent; }
    }
} 