package com.sanjin.minemind.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiToolContext {
    public static final int MAX_CONTEXT_CHARS = 6000;
    public static final int MAX_RESULT_CHARS = 1600;

    private final List<AiToolResult> results;

    private AiToolContext(List<AiToolResult> results) {
        this.results = List.copyOf(results);
    }

    public static AiToolContext empty() {
        return new AiToolContext(List.of());
    }

    public static AiToolContext of(List<AiToolResult> results) {
        if (results == null || results.isEmpty()) {
            return empty();
        }
        List<AiToolResult> sanitized = new ArrayList<>();
        for (AiToolResult result : results) {
            sanitized.add(result.truncate(MAX_RESULT_CHARS));
        }
        return new AiToolContext(sanitized);
    }

    public boolean hasResults() {
        return !results.isEmpty();
    }

    public String summaryLabels() {
        List<String> labels = new ArrayList<>();
        for (AiToolResult result : results) {
            if (!labels.contains(result.label())) {
                labels.add(result.label());
            }
        }
        return String.join("、", labels);
    }

    public String toPromptText() {
        if (results.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Minecraft Tool Context\n");
        builder.append("以下信息由 MineMind 本地工具在本轮请求中读取，仅代表当前客户端可见状态。模型只能基于这些已附加结果回答，不要假装读取未附加的信息。\n");
        for (AiToolResult result : results) {
            builder.append('\n').append(result.toPromptText());
        }
        String text = builder.toString();
        if (text.length() <= MAX_CONTEXT_CHARS) {
            return text;
        }
        return text.substring(0, MAX_CONTEXT_CHARS) + "\n[工具上下文已裁剪]";
    }
}

record AiToolResult(String toolId, String trigger, String label, String content, boolean truncated) {
    AiToolResult truncate(int maxChars) {
        String value = content == null ? "" : content.trim();
        if (value.length() <= maxChars) {
            return new AiToolResult(toolId, trigger, label, value, truncated);
        }
        return new AiToolResult(toolId, trigger, label, value.substring(0, maxChars) + "\n[单项工具结果已裁剪]", true);
    }

    String toPromptText() {
        StringBuilder builder = new StringBuilder();
        builder.append("### ").append(toolId).append('\n');
        builder.append("触发标记: ").append(trigger).append('\n');
        builder.append("裁剪: ").append(truncated ? "是" : "否").append('\n');
        builder.append("结果:\n");
        builder.append(content == null || content.isBlank() ? "无结果" : content.trim()).append('\n');
        return builder.toString();
    }
}
