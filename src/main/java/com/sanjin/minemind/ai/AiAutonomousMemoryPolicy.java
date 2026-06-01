package com.sanjin.minemind.ai;

import java.util.Locale;

public final class AiAutonomousMemoryPolicy {
    public static final int MAX_MEMORY_CONTENT_CHARS = 500;
    public static final int MIN_DELETE_KEYWORD_CHARS = 2;
    public static final int MAX_DELETE_MATCHES = 5;

    private AiAutonomousMemoryPolicy() {
    }

    public static String requireWrite(String userIntent, String content) throws AiException {
        if (!hasWriteIntent(userIntent)) {
            throw new AiException(AiErrorType.REQUEST, "模型请求写入长期记忆，但玩家本轮没有明确记忆写入意图");
        }
        String cleaned = singleLine(content);
        if (cleaned.isBlank()) {
            throw new AiException(AiErrorType.REQUEST, "长期记忆内容不能为空");
        }
        if (cleaned.length() > MAX_MEMORY_CONTENT_CHARS) {
            throw new AiException(AiErrorType.REQUEST, "长期记忆内容过长，模型需要整理为 500 字以内");
        }
        return cleaned;
    }

    public static String requireDelete(String userIntent, String keyword) throws AiException {
        if (!hasDeleteIntent(userIntent)) {
            throw new AiException(AiErrorType.REQUEST, "模型请求删除长期记忆，但玩家本轮没有明确记忆删除意图");
        }
        String cleaned = singleLine(keyword);
        if (cleaned.isBlank()) {
            throw new AiException(AiErrorType.REQUEST, "遗忘关键词不能为空");
        }
        if (cleaned.length() < MIN_DELETE_KEYWORD_CHARS || isBroadDeleteKeyword(cleaned)) {
            throw new AiException(AiErrorType.REQUEST, "遗忘关键词过短或过宽，请提供更具体的关键词");
        }
        return cleaned;
    }

    static boolean hasWriteIntent(String userIntent) {
        String text = normalize(userIntent);
        return containsAny(text,
                "记住",
                "记录",
                "保存",
                "写入记忆",
                "加入记忆",
                "存入记忆",
                "长期记忆",
                "帮我记",
                "记一下",
                "remember",
                "memorize",
                "save",
                "record"
        );
    }

    static boolean hasDeleteIntent(String userIntent) {
        String text = normalize(userIntent);
        return containsAny(text,
                "忘掉",
                "遗忘",
                "删除",
                "删掉",
                "清除",
                "移除",
                "从记忆里去掉",
                "forget",
                "delete",
                "remove",
                "erase"
        );
    }

    private static boolean isBroadDeleteKeyword(String keyword) {
        String normalized = normalize(keyword);
        return normalized.equals("*")
                || normalized.equals("all")
                || normalized.equals("全部")
                || normalized.equals("所有")
                || normalized.equals("全部记忆")
                || normalized.equals("所有记忆")
                || normalized.equals("长期记忆");
    }

    private static String singleLine(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String normalize(String value) {
        return singleLine(value).toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
