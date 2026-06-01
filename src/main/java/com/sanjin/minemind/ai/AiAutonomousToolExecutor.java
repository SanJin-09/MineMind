package com.sanjin.minemind.ai;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;

public final class AiAutonomousToolExecutor {
    private AiAutonomousToolExecutor() {
    }

    public static AiToolExecution execute(AiToolCall call, String playerName, LocalDateTime time, String userIntent) throws AiException {
        AiToolSpec spec = AiToolRegistry.spec(call.name())
                .orElseThrow(() -> new AiException(AiErrorType.REQUEST, "模型请求了未知工具：" + call.name()));
        JsonObject arguments;
        try {
            arguments = call.argumentsObject();
        } catch (AiToolCall.ToolCallException exception) {
            throw new AiException(AiErrorType.REQUEST, exception.getMessage());
        }

        return switch (spec.name()) {
            case AiToolRegistry.HOTBAR -> minecraft(AiToolRequest.Tool.HOTBAR);
            case AiToolRegistry.INVENTORY -> minecraft(AiToolRequest.Tool.INVENTORY);
            case AiToolRegistry.LOCATION -> minecraft(AiToolRequest.Tool.HERE);
            case AiToolRegistry.NEARBY -> minecraft(AiToolRequest.Tool.NEARBY);
            case AiToolRegistry.TARGET -> minecraft(AiToolRequest.Tool.TARGET);
            case AiToolRegistry.MEMORY_READ -> memoryRead(arguments);
            case AiToolRegistry.MEMORY_WRITE -> memoryWrite(arguments, playerName, time, userIntent);
            case AiToolRegistry.MEMORY_DELETE -> memoryDelete(arguments, userIntent);
            default -> throw new AiException(AiErrorType.REQUEST, "模型请求了未知工具：" + call.name());
        };
    }

    private static AiToolExecution minecraft(AiToolRequest.Tool tool) throws AiException {
        try {
            return new AiToolExecution(AiMinecraftTools.collect(tool), false, "");
        } catch (AiMinecraftTools.AiToolException exception) {
            throw new AiException(AiErrorType.LOCAL, "本地工具读取失败，请重试");
        }
    }

    private static AiToolExecution memoryRead(JsonObject arguments) {
        String query = string(arguments, "query", "");
        return new AiToolExecution(AiMemoryStore.readContextResult(query), false, "");
    }

    private static AiToolExecution memoryWrite(JsonObject arguments, String playerName, LocalDateTime time, String userIntent) throws AiException {
        String content = AiAutonomousMemoryPolicy.requireWrite(userIntent, firstString(arguments, "content", "memory"));
        try {
            AiMemoryStore.MemoryWriteResult result = AiMemoryStore.remember(playerName, content, time);
            AiToolResult toolResult = new AiToolResult(
                    AiToolRegistry.MEMORY_WRITE,
                    "tool_call",
                    "长期记忆",
                    "已写入长期记忆：" + result.content(),
                    false
            );
            return new AiToolExecution(toolResult, true, "已写入长期记忆：" + result.content());
        } catch (AiMemoryStore.MemoryException exception) {
            throw new AiException(AiErrorType.LOCAL, exception.getMessage());
        }
    }

    private static AiToolExecution memoryDelete(JsonObject arguments, String userIntent) throws AiException {
        String keyword = AiAutonomousMemoryPolicy.requireDelete(userIntent, firstString(arguments, "keyword", "query"));
        try {
            AiMemoryStore.MemoryDeleteResult result = AiMemoryStore.forgetByKeywordLimit(keyword, AiAutonomousMemoryPolicy.MAX_DELETE_MATCHES);
            AiToolResult toolResult = new AiToolResult(
                    AiToolRegistry.MEMORY_DELETE,
                    "tool_call",
                    "长期记忆",
                    "已删除长期记忆：" + result.count() + " 条" + (result.count() > 0 ? "；" + result.summary() : ""),
                    false
            );
            return new AiToolExecution(toolResult, true, "已删除长期记忆：" + result.count() + " 条");
        } catch (AiMemoryStore.MemoryException exception) {
            throw new AiException(AiErrorType.LOCAL, exception.getMessage());
        }
    }

    private static String firstString(JsonObject object, String first, String second) throws AiException {
        String value = string(object, first, "");
        if (value.isBlank()) {
            value = string(object, second, "");
        }
        if (value.isBlank()) {
            throw new AiException(AiErrorType.REQUEST, "模型工具参数缺少：" + first);
        }
        return value;
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return fallback;
        }
        String text = value.getAsString();
        return text == null ? fallback : text.trim();
    }
}
