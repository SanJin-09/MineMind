package com.sanjin.minemind.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AiMemoryForgetDraft {
    private static final String SYSTEM_PROMPT = """
            你正在为 Minecraft 玩家整理长期记忆删除操作。请根据玩家的删除意图、候选长期记忆条目，以及本轮上下文中明确附加的 Minecraft Tool Context，选择应该删除的候选条目编号。

            只能选择候选列表中已有的 id；不要编造 id，不要删除与玩家意图无关的条目。如果没有明确匹配项，delete 返回空数组。输出必须是单个 JSON 对象，格式严格为 {"delete":[1,2],"reason":"简短原因"}，不要输出解释、Markdown 或额外字段。
            """.trim();

    private AiMemoryForgetDraft() {
    }

    public static List<AiMessage> messages(
            AiToolContext toolContext,
            List<AiMemoryStore.MemoryDeletionCandidate> candidates,
            String userPrompt
    ) {
        List<AiMessage> messages = new ArrayList<>();
        messages.add(new AiMessage("system", SYSTEM_PROMPT));
        if (toolContext != null && toolContext.hasResults()) {
            messages.add(new AiMessage("system", toolContext.toPromptText()));
        }
        messages.add(new AiMessage("system", candidateText(candidates)));
        messages.add(new AiMessage("user", userPrompt == null ? "" : userPrompt.trim()));
        return List.copyOf(messages);
    }

    public static List<Integer> parseDeleteIds(String response, List<AiMemoryStore.MemoryDeletionCandidate> candidates) {
        Set<Integer> validIds = new LinkedHashSet<>();
        if (candidates != null) {
            for (AiMemoryStore.MemoryDeletionCandidate candidate : candidates) {
                validIds.add(candidate.id());
            }
        }

        String json = extractJson(response);
        try {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (!object.has("delete") || !object.get("delete").isJsonArray()) {
                throw new DraftException();
            }
            JsonArray array = object.getAsJsonArray("delete");
            Set<Integer> ids = new LinkedHashSet<>();
            for (JsonElement element : array) {
                int id = element.getAsInt();
                if (!validIds.contains(id)) {
                    throw new DraftException();
                }
                ids.add(id);
            }
            return List.copyOf(ids);
        } catch (NumberFormatException | ClassCastException | IllegalStateException | UnsupportedOperationException | JsonParseException exception) {
            throw new DraftException();
        }
    }

    private static String candidateText(List<AiMemoryStore.MemoryDeletionCandidate> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("Long-term Memory Delete Candidates\n");
        if (candidates == null || candidates.isEmpty()) {
            builder.append("无候选条目。");
            return builder.toString();
        }
        for (AiMemoryStore.MemoryDeletionCandidate candidate : candidates) {
            builder.append("ID ").append(candidate.id()).append(": ").append(candidate.text()).append('\n');
        }
        return builder.toString();
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
