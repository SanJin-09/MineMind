package com.sanjin.minemind.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public record AiToolCall(String id, String name, String argumentsJson) {
    public AiToolCall {
        id = id == null || id.isBlank() ? "call_" + Math.abs(System.nanoTime()) : id.trim();
        name = name == null ? "" : name.trim();
        argumentsJson = argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson.trim();
    }

    public JsonObject argumentsObject() {
        try {
            return JsonParser.parseString(argumentsJson).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new ToolCallException("工具参数格式无效");
        }
    }

    public static final class ToolCallException extends RuntimeException {
        public ToolCallException(String message) {
            super(message);
        }
    }
}
