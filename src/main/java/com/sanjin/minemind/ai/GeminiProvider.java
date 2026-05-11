package com.sanjin.minemind.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GeminiProvider implements AiProvider {
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String id;
    private final String displayName;
    private final String defaultBaseUrl;

    public GeminiProvider(String id, String displayName, String defaultBaseUrl) {
        this.id = id;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String defaultBaseUrl() {
        return defaultBaseUrl;
    }

    @Override
    public String complete(AiProviderSettings settings, List<AiMessage> messages) throws AiException {
        validateChat(settings);

        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        StringBuilder systemInstruction = new StringBuilder();
        for (AiMessage message : messages) {
            if ("system".equals(message.role())) {
                if (!systemInstruction.isEmpty()) {
                    systemInstruction.append('\n');
                }
                systemInstruction.append(message.content());
                continue;
            }

            JsonObject item = new JsonObject();
            item.addProperty("role", geminiRole(message.role()));

            item.add("parts", textParts(message.content()));
            contents.add(item);
        }
        if (!systemInstruction.isEmpty()) {
            JsonObject instruction = new JsonObject();
            instruction.add("parts", textParts(systemInstruction.toString()));
            body.add("systemInstruction", instruction);
        }
        body.add("contents", contents);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.baseUrl() + "/models/" + encodePath(modelId(settings.model())) + ":generateContent"))
                    .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", settings.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException exception) {
            throw new AiException(AiErrorType.LOCAL, "API Base URL 无效");
        }

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiException(AiHttpStatusClassifier.classify(response.statusCode(), response.body()));
        }
        return parseContent(response.body());
    }

    @Override
    public List<String> fetchModelIds(AiProviderSettings settings) throws AiException {
        validateCatalog(settings);

        Set<String> modelIds = new LinkedHashSet<>();
        String pageToken = "";
        do {
            HttpRequest request;
            try {
                String uri = settings.baseUrl() + "/models?pageSize=1000" + pageTokenQuery(pageToken);
                request = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                        .header("x-goog-api-key", settings.apiKey())
                        .GET()
                        .build();
            } catch (IllegalArgumentException exception) {
                throw new AiException(AiErrorType.LOCAL, "API Base URL 无效");
            }

            HttpResponse<String> response = send(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiException(AiHttpStatusClassifier.classify(response.statusCode(), response.body()));
            }

            GeminiModelPage page = parseModelPage(response.body());
            modelIds.addAll(page.modelIds());
            pageToken = page.nextPageToken();
        } while (!pageToken.isBlank());

        if (modelIds.isEmpty()) {
            throw new AiException(AiErrorType.MODEL, "未获取到可用文本对话模型列表");
        }

        List<String> sorted = new ArrayList<>(modelIds);
        Collections.sort(sorted);
        List<String> result = List.copyOf(sorted);
        AiModelCatalog.cacheModelIds(settings.providerId(), result);
        return result;
    }

    private static void validateChat(AiProviderSettings settings) throws AiException {
        validateCatalog(settings);
        if (settings.model() == null || settings.model().isBlank()) {
            throw new AiException(AiErrorType.MODEL);
        }
    }

    private static void validateCatalog(AiProviderSettings settings) throws AiException {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new AiException(AiErrorType.MISSING_KEY);
        }
        if (settings.baseUrl() == null || settings.baseUrl().isBlank()) {
            throw new AiException(AiErrorType.LOCAL, "当前服务商没有配置 API Base URL");
        }
    }

    private static HttpResponse<String> send(HttpRequest request) throws AiException {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
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
    }

    private static String parseContent(String body) throws AiException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }

            JsonObject first = candidates.get(0).getAsJsonObject();
            JsonObject content = first.getAsJsonObject("content");
            if (content == null) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }

            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }

            StringBuilder text = new StringBuilder();
            for (JsonElement element : parts) {
                JsonObject part = element.getAsJsonObject();
                JsonElement value = part.get("text");
                if (value != null && !value.isJsonNull()) {
                    if (!text.isEmpty()) {
                        text.append('\n');
                    }
                    text.append(value.getAsString());
                }
            }

            String result = text.toString().trim();
            if (result.isBlank()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            return result;
        } catch (AiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiException(AiErrorType.SERVICE, "服务商返回了无法解析的响应");
        }
    }

    private static GeminiModelPage parseModelPage(String body) throws AiException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray models = root.getAsJsonArray("models");
            if (models == null || models.isEmpty()) {
                throw new AiException(AiErrorType.MODEL, "未获取到可用模型列表");
            }

            List<String> modelIds = new ArrayList<>();
            for (JsonElement element : models) {
                JsonObject item = element.getAsJsonObject();
                JsonElement name = item.get("name");
                if (name != null && !name.isJsonNull()) {
                    String modelId = modelId(name.getAsString());
                    if (supportsGenerateContent(item) && AiModelCatalog.isTextChatModelId(modelId)) {
                        modelIds.add(modelId);
                    }
                }
            }

            String nextPageToken = "";
            JsonElement token = root.get("nextPageToken");
            if (token != null && !token.isJsonNull()) {
                nextPageToken = token.getAsString();
            }
            return new GeminiModelPage(List.copyOf(modelIds), nextPageToken);
        } catch (AiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiException(AiErrorType.SERVICE, "服务商返回了无法解析的模型列表");
        }
    }

    private static boolean supportsGenerateContent(JsonObject model) {
        JsonArray methods = model.getAsJsonArray("supportedGenerationMethods");
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        for (JsonElement method : methods) {
            if ("generateContent".equalsIgnoreCase(method.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static String geminiRole(String role) {
        return "assistant".equals(role) ? "model" : "user";
    }

    private static JsonArray textParts(String content) {
        JsonArray parts = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("text", content == null ? "" : content);
        parts.add(text);
        return parts;
    }

    private static String modelId(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.startsWith("models/")) {
            return cleaned.substring("models/".length());
        }
        return cleaned;
    }

    private static String pageTokenQuery(String pageToken) {
        if (pageToken == null || pageToken.isBlank()) {
            return "";
        }
        return "&pageToken=" + encodeQuery(pageToken);
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record GeminiModelPage(List<String> modelIds, String nextPageToken) {
    }
}
