package com.sanjin.minemind.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AiModelCatalog {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private AiModelCatalog() {
    }

    public static List<String> cachedModelIds(String providerId) {
        try {
            String normalized = AiConfigRules.normalizeProviderId(providerId);
            return CACHE.getOrDefault(normalized, List.of());
        } catch (AiConfigStore.ConfigException exception) {
            return List.of();
        }
    }

    public static void cacheModelIds(String providerId, List<String> modelIds) {
        try {
            String normalized = AiConfigRules.normalizeProviderId(providerId);
            CACHE.put(normalized, List.copyOf(modelIds));
        } catch (AiConfigStore.ConfigException ignored) {
        }
    }

    public static List<String> fetchModelIds(AiProviderSettings settings) throws AiException {
        validate(settings);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.baseUrl() + "/models"))
                    .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .GET()
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new AiException(AiErrorType.LOCAL, "API Base URL 无效");
        }

        HttpResponse<String> response;
        try {
            response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException exception) {
            throw new AiException(AiErrorType.TIMEOUT, exception);
        } catch (ConnectException exception) {
            throw new AiException(AiErrorType.NETWORK, exception);
        } catch (IOException exception) {
            throw new AiException(AiErrorType.NETWORK, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiException(AiErrorType.LOCAL, "请求已中断");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiException(AiHttpStatusClassifier.classify(response.statusCode(), response.body()));
        }

        List<String> modelIds = parseModelIds(response.body());
        CACHE.put(settings.providerId(), modelIds);
        return modelIds;
    }

    private static void validate(AiProviderSettings settings) throws AiException {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new AiException(AiErrorType.MISSING_KEY);
        }
        if (settings.baseUrl() == null || settings.baseUrl().isBlank()) {
            throw new AiException(AiErrorType.LOCAL, "当前服务商没有配置 API Base URL");
        }
    }

    private static List<String> parseModelIds(String body) throws AiException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray data = root.getAsJsonArray("data");
            if (data == null || data.isEmpty()) {
                throw new AiException(AiErrorType.MODEL, "未获取到可用模型列表");
            }
            List<String> modelIds = new ArrayList<>();
            for (JsonElement element : data) {
                JsonObject item = element.getAsJsonObject();
                JsonElement id = item.get("id");
                if (id != null && !id.isJsonNull()) {
                    String modelId = id.getAsString();
                    if (isTextChatModelId(modelId)) {
                        modelIds.add(modelId);
                    }
                }
            }
            if (modelIds.isEmpty()) {
                throw new AiException(AiErrorType.MODEL, "未获取到可用文本对话模型列表");
            }
            Collections.sort(modelIds);
            return List.copyOf(modelIds);
        } catch (AiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiException(AiErrorType.SERVICE, "服务商返回了无法解析的模型列表");
        }
    }

    static boolean isTextChatModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return false;
        }
        String id = modelId.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        if (containsAny(id,
                "embedding",
                "dall-e",
                "gpt-image",
                "image-generation",
                "imagegen",
                "seedream",
                "text-to-image",
                "txt2img",
                "t2i",
                "i2i",
                "recraft",
                "stable-diffusion",
                "realtime",
                "audio",
                "tts",
                "whisper",
                "transcribe",
                "speech",
                "moderation",
                "omni",
                "sora",
                "video",
                "seedance",
                "search-preview",
                "computer-use",
                "babbage",
                "davinci",
                "qvq"
        )) {
            return false;
        }
        return startsWithAny(id,
                "gpt-",
                "ft:gpt-",
                "o1",
                "ft:o1",
                "o3",
                "ft:o3",
                "o4",
                "ft:o4",
                "o5",
                "ft:o5",
                "deepseek-",
                "qwen",
                "glm",
                "moonshot",
                "kimi",
                "doubao",
                "seed",
                "grok",
                "claude",
                "yi-",
                "hunyuan",
                "abab",
                "llama",
                "mistral",
                "mixtral",
                "gemini"
        ) || containsAny(id, "chat", "instruct", "reasoner");
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
