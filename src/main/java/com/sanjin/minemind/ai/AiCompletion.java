package com.sanjin.minemind.ai;

import java.util.List;

public record AiCompletion(String content, List<AiToolCall> toolCalls) {
    public AiCompletion {
        content = content == null ? "" : content.trim();
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static AiCompletion text(String content) {
        return new AiCompletion(content, List.of());
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
