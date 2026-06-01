package com.sanjin.minemind.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class AiToolJsonFallback {
    public static final String PROMPT = """
            MineMind 支持模型自主请求本地工具。若回答当前问题需要读取或修改本地信息，请只输出 JSON：
            {"tool_calls":[{"id":"call_1","name":"tool.location.read","arguments":{}}]}
            可用工具：tool.hotbar.read、tool.inventory.read、tool.location.read、tool.entities.nearby、tool.target.read、tool.memory.read、tool.memory.write、tool.memory.delete。
            不需要工具时，直接用自然语言回答。不要请求截图工具，截图只能由玩家显式 @image 触发。
            """.trim();

    private AiToolJsonFallback() {
    }

    public static String prompt(List<AiToolSpec> tools) {
        if (tools == null || tools.isEmpty()) {
            return PROMPT;
        }
        String names = tools.stream()
                .map(AiToolSpec::name)
                .reduce((left, right) -> left + "、" + right)
                .orElse("");
        String exampleTool = tools.get(0).name();
        return """
                MineMind 支持模型自主请求本地工具。若回答当前问题需要读取或修改本地信息，请只输出 JSON：
                {"tool_calls":[{"id":"call_1","name":"%s","arguments":{}}]}
                本轮已启用工具：%s。
                不需要工具时，直接用自然语言回答。不要请求截图工具，截图只能由玩家显式 @image 触发。
                """.formatted(exampleTool, names).trim();
    }

    public static List<AiToolCall> parseToolCalls(String content) {
        String text = stripFence(content);
        if (text.isBlank()) {
            return List.of();
        }
        if (!looksLikeJson(text)) {
            return List.of();
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new FallbackException("工具调用 JSON 格式无效");
        }
        JsonArray calls = root.getAsJsonArray("tool_calls");
        if (calls == null) {
            return List.of();
        }
        List<AiToolCall> result = new ArrayList<>();
        for (int index = 0; index < calls.size(); index++) {
            JsonObject item = calls.get(index).getAsJsonObject();
            String id = string(item, "id", "call_" + (index + 1));
            String name = string(item, "name", "");
            JsonElement arguments = item.get("arguments");
            String argumentsJson = arguments == null || arguments.isJsonNull() ? "{}" : arguments.toString();
            if (!AiToolRegistry.contains(name)) {
                throw new FallbackException("模型请求了未知工具：" + name);
            }
            result.add(new AiToolCall(id, name, argumentsJson));
        }
        return List.copyOf(result);
    }

    private static String stripFence(String content) {
        String text = content == null ? "" : content.trim();
        if (!text.startsWith("```")) {
            return text;
        }
        int firstNewline = text.indexOf('\n');
        int lastFence = text.lastIndexOf("```");
        if (firstNewline >= 0 && lastFence > firstNewline) {
            return text.substring(firstNewline + 1, lastFence).trim();
        }
        return text;
    }

    private static boolean looksLikeJson(String text) {
        return text.startsWith("{") && text.endsWith("}");
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return fallback;
        }
        String text = value.getAsString();
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    public static final class FallbackException extends RuntimeException {
        public FallbackException(String message) {
            super(message);
        }
    }
}
