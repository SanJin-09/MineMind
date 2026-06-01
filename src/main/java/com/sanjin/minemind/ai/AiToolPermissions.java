package com.sanjin.minemind.ai;

public record AiToolPermissions(
        boolean autonomousTools,
        boolean locationTool,
        boolean memoryReadTool,
        boolean memoryWriteTool,
        boolean memoryDeleteTool
) {
    public boolean isAllowed(AiToolSpec spec) {
        if (spec == null || !autonomousTools) {
            return false;
        }
        return switch (spec.name()) {
            case AiToolRegistry.LOCATION -> locationTool;
            case AiToolRegistry.MEMORY_READ -> memoryReadTool;
            case AiToolRegistry.MEMORY_WRITE -> memoryWriteTool;
            case AiToolRegistry.MEMORY_DELETE -> memoryDeleteTool;
            default -> true;
        };
    }
}
