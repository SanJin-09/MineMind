package com.sanjin.minemind.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class AiMemoryDraft {
    private static final int MAX_MEMORY_LENGTH = 500;
    private static final String SYSTEM_PROMPT = """
            你正在为 Minecraft 玩家整理一条长期记忆。请根据玩家的原始意图，以及本轮上下文中明确附加的 Minecraft Tool Context，把信息整理为一条适合长期保存的简短中文记忆。

            只允许使用玩家输入和已附加工具结果中的信息；不要编造坐标、生态群系、物品、实体或其他游戏状态。输出必须是单个 JSON 对象，格式严格为 {"memory":"整理后的记忆内容"}，不要输出解释、Markdown 或额外字段。
            """.trim();

    private AiMemoryDraft() {
    }

    public static List<AiMessage> messages(AiToolContext toolContext, String userPrompt) {
        List<AiMessage> messages = new ArrayList<>();
        messages.add(new AiMessage("system", SYSTEM_PROMPT));
        if (toolContext != null && toolContext.hasResults()) {
            messages.add(new AiMessage("system", toolContext.toPromptText()));
        }
        messages.add(new AiMessage("user", userPrompt == null ? "" : userPrompt.trim()));
        return List.copyOf(messages);
    }

    public static String parseMemory(String response) {
        String json = extractJson(response);
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (!object.has("memory") || object.get("memory").isJsonNull()) {
                throw new DraftException();
            }
            String memory = object.get("memory").getAsString().replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
            if (memory.isBlank() || memory.length() > MAX_MEMORY_LENGTH) {
                throw new DraftException();
            }
            return memory;
        } catch (IllegalStateException | UnsupportedOperationException | JsonParseException exception) {
            throw new DraftException();
        }
    }

    private static String extractJson(String response) {
        String value = response == null ? "" : response.trim();
        if (value.startsWith("```")) {
            int firstLine = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                value = value.substring(firstLine + 1, lastFence).trim();
            }
        }

        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new DraftException();
        }
        return value.substring(start, end + 1);
    }

    public static final class DraftException extends RuntimeException {
    }
}
