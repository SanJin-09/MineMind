package com.sanjin.minemind.ai;

import java.util.List;

public record AiMessage(
        String role,
        String content,
        List<AiImageAttachment> images,
        List<AiToolCall> toolCalls,
        String toolCallId
) {
    public AiMessage {
        role = role == null || role.isBlank() ? "user" : role.trim();
        content = content == null ? "" : content;
        images = images == null ? List.of() : List.copyOf(images);
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
        toolCallId = toolCallId == null ? "" : toolCallId.trim();
    }

    public AiMessage(String role, String content) {
        this(role, content, List.of(), List.of(), "");
    }

    public AiMessage(String role, String content, List<AiImageAttachment> images) {
        this(role, content, images, List.of(), "");
    }

    public static AiMessage assistantToolCalls(String content, List<AiToolCall> toolCalls) {
        return new AiMessage("assistant", content, List.of(), toolCalls, "");
    }

    public static AiMessage toolResult(String toolCallId, String content) {
        return new AiMessage("tool", content, List.of(), List.of(), toolCallId);
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }
}
