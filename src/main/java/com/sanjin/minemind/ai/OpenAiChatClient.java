package com.sanjin.minemind.ai;

import com.google.gson.Gson;
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
import java.util.List;

public final class OpenAiChatClient {
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private OpenAiChatClient() {
    }

    public static String complete(AiProviderSettings settings, List<AiMessage> messages) throws AiException {
        validate(settings);

        JsonObject body = new JsonObject();
        body.addProperty("model", settings.model());
        body.addProperty("stream", false);

        JsonArray jsonMessages = new JsonArray();
        for (AiMessage message : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            item.addProperty("content", message.content());
            jsonMessages.add(item);
        }
        body.add("messages", jsonMessages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(settings.baseUrl() + "/chat/completions"))
                .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + settings.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
                .build();

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
        } catch (IllegalArgumentException exception) {
            throw new AiException(AiErrorType.LOCAL, "API Base URL 无效");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AiException(AiHttpStatusClassifier.classify(response.statusCode()));
        }
        return parseContent(response.body());
    }

    private static void validate(AiProviderSettings settings) throws AiException {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new AiException(AiErrorType.MISSING_KEY);
        }
        if (settings.model() == null || settings.model().isBlank()) {
            throw new AiException(AiErrorType.MODEL);
        }
        if (settings.baseUrl() == null || settings.baseUrl().isBlank()) {
            throw new AiException(AiErrorType.LOCAL, "当前服务商没有配置 API Base URL");
        }
    }

    private static String parseContent(String body) throws AiException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            JsonObject first = choices.get(0).getAsJsonObject();
            JsonObject message = first.getAsJsonObject("message");
            if (message == null) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            JsonElement content = message.get("content");
            if (content == null || content.isJsonNull()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            String text = content.isJsonPrimitive() ? content.getAsString() : content.toString();
            if (text == null || text.isBlank()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            return text.trim();
        } catch (RuntimeException exception) {
            throw new AiException(AiErrorType.SERVICE, "服务商返回了无法解析的响应");
        }
    }
}
