package com.sanjin.minemind.ai;

public record AiToolExecution(AiToolResult result, boolean memoryMutation, String notice) {
    public AiToolExecution {
        notice = notice == null ? "" : notice.trim();
    }
}
