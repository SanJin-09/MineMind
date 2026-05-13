package com.sanjin.minemind.ai;

import java.util.Locale;

public final class AiModelCapabilities {
    private static final String IMAGE_MARKER = " [图片]";

    private AiModelCapabilities() {
    }

    public static boolean supportsImageInput(AiProviderSettings settings) {
        if (settings == null) {
            return false;
        }
        return supportsImageInput(settings.providerId(), settings.model());
    }

    public static boolean supportsImageInput(String providerId, String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return false;
        }
        String normalizedProviderId;
        try {
            normalizedProviderId = AiConfigRules.normalizeProviderId(providerId);
        } catch (AiConfigStore.ConfigException exception) {
            return false;
        }
        String normalizedModelId = normalizeModelId(modelId);
        if (isNonChatMediaModel(normalizedModelId)) {
            return false;
        }
        return switch (normalizedProviderId) {
            case "openai" -> isOpenAiImageInputModel(normalizedModelId);
            case "gemini" -> normalizedModelId.startsWith("gemini-");
            case "qwen" -> isQwenImageInputModel(normalizedModelId);
            case "kimi" -> isKimiImageInputModel(normalizedModelId);
            case "glm" -> isGlmImageInputModel(normalizedModelId);
            case "seed" -> isSeedImageInputModel(normalizedModelId);
            case "grok" -> containsAny(normalizedModelId, "vision", "visual");
            default -> false;
        };
    }

    public static String displayModelId(String providerId, String modelId) {
        String cleaned = modelId == null ? "" : modelId.trim();
        if (cleaned.isBlank()) {
            return cleaned;
        }
        return supportsImageInput(providerId, cleaned) ? cleaned + IMAGE_MARKER : cleaned;
    }

    private static boolean isOpenAiImageInputModel(String modelId) {
        return startsWithAny(modelId,
                "gpt-4o",
                "chatgpt-4o",
                "gpt-4.1",
                "gpt-4.5",
                "gpt-5",
                "o3",
                "o4"
        );
    }

    private static boolean isQwenImageInputModel(String modelId) {
        return containsAny(modelId,
                "qwen-vl",
                "qwen2-vl",
                "qwen2.5-vl",
                "qwen3-vl",
                "qwen3.5-flash",
                "qwen3.6-plus",
                "-vl",
                "vl-"
        );
    }

    private static boolean isKimiImageInputModel(String modelId) {
        return containsAny(modelId, "vision") || startsWithAny(modelId, "kimi-k2.6");
    }

    private static boolean isGlmImageInputModel(String modelId) {
        return containsAny(modelId,
                "glm-5v",
                "glm-4.6v",
                "glm-4.5v",
                "glm-4v",
                "glm-4-v"
        );
    }

    private static boolean isSeedImageInputModel(String modelId) {
        return containsAny(modelId,
                "vision",
                "visual",
                "-vl",
                "vl-",
                "doubao-vision",
                "doubao-seed-vision",
                "doubao-seed-1-6-vision",
                "doubao-seed-1.6-vision"
        );
    }

    private static boolean isNonChatMediaModel(String modelId) {
        return containsAny(modelId,
                "embedding",
                "dall-e",
                "gpt-image",
                "image-generation",
                "realtime",
                "audio",
                "tts",
                "whisper",
                "transcribe",
                "speech",
                "moderation",
                "sora",
                "video"
        );
    }

    private static String normalizeModelId(String modelId) {
        return modelId.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String value, String... prefixes) {
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
