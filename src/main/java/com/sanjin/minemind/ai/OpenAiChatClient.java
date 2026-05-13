package com.sanjin.minemind.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

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
        return completeWithTools(settings, messages, List.of()).content();
    }

    public static AiCompletion completeWithTools(AiProviderSettings settings, List<AiMessage> messages, List<AiToolSpec> tools) throws AiException {
        validate(settings);

        JsonObject body = requestBody(settings, messages, tools);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
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
        return parseCompletion(response.body());
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

    private static JsonElement content(AiMessage message) {
        if ("assistant".equals(message.role()) && message.hasToolCalls() && message.content().isBlank()) {
            return JsonNull.INSTANCE;
        }
        if (!message.hasImages()) {
            return new JsonPrimitive(message.content());
        }

        JsonArray parts = new JsonArray();
        JsonObject text = new JsonObject();
        text.addProperty("type", "text");
        text.addProperty("text", message.content());
        parts.add(text);

        for (AiImageAttachment image : message.images()) {
            JsonObject imageUrl = new JsonObject();
            imageUrl.addProperty("url", image.dataUrl());
            imageUrl.addProperty("detail", "auto");

            JsonObject imagePart = new JsonObject();
            imagePart.addProperty("type", "image_url");
            imagePart.add("image_url", imageUrl);
            parts.add(imagePart);
        }
        return parts;
    }

    static JsonObject requestBody(AiProviderSettings settings, List<AiMessage> messages, List<AiToolSpec> tools) {
        JsonObject body = new JsonObject();
        body.addProperty("model", settings.model());
        body.addProperty("stream", false);

        JsonArray jsonMessages = new JsonArray();
        for (AiMessage message : messages) {
            JsonObject item = new JsonObject();
            item.addProperty("role", message.role());
            item.add("content", content(message));
            if ("tool".equals(message.role())) {
                item.addProperty("tool_call_id", message.toolCallId());
            }
            if (message.hasToolCalls()) {
                item.add("tool_calls", toolCalls(message.toolCalls()));
            }
            jsonMessages.add(item);
        }
        body.add("messages", jsonMessages);

        if (tools != null && !tools.isEmpty()) {
            body.add("tools", toolDefinitions(tools));
            body.addProperty("tool_choice", "auto");
        }
        return body;
    }

    static String requestBodyJson(AiProviderSettings settings, List<AiMessage> messages, List<AiToolSpec> tools) {
        return GSON.toJson(requestBody(settings, messages, tools));
    }

    static JsonArray toolDefinitions(List<AiToolSpec> tools) {
        JsonArray definitions = new JsonArray();
        for (AiToolSpec spec : tools) {
            JsonObject function = new JsonObject();
            function.addProperty("name", spec.name());
            function.addProperty("description", spec.description());
            function.add("parameters", spec.parameters());

            JsonObject definition = new JsonObject();
            definition.addProperty("type", "function");
            definition.add("function", function);
            definitions.add(definition);
        }
        return definitions;
    }

    private static JsonArray toolCalls(List<AiToolCall> calls) {
        JsonArray result = new JsonArray();
        for (AiToolCall call : calls) {
            JsonObject function = new JsonObject();
            function.addProperty("name", call.name());
            function.addProperty("arguments", call.argumentsJson());

            JsonObject item = new JsonObject();
            item.addProperty("id", call.id());
            item.addProperty("type", "function");
            item.add("function", function);
            result.add(item);
        }
        return result;
    }

    static AiCompletion parseCompletion(String body) throws AiException {
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
            List<AiToolCall> toolCalls = parseToolCalls(message);
            JsonElement content = message.get("content");
            if ((content == null || content.isJsonNull()) && toolCalls.isEmpty()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            String text = content == null || content.isJsonNull()
                    ? ""
                    : (content.isJsonPrimitive() ? content.getAsString() : content.toString());
            if ((text == null || text.isBlank()) && toolCalls.isEmpty()) {
                throw new AiException(AiErrorType.EMPTY_RESPONSE);
            }
            return new AiCompletion(text, toolCalls);
        } catch (RuntimeException exception) {
            throw new AiException(AiErrorType.SERVICE, "服务商返回了无法解析的响应");
        }
    }

    private static List<AiToolCall> parseToolCalls(JsonObject message) {
        JsonArray values = message.getAsJsonArray("tool_calls");
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<AiToolCall> calls = new java.util.ArrayList<>();
        for (JsonElement element : values) {
            JsonObject item = element.getAsJsonObject();
            String id = string(item, "id", "call_" + (calls.size() + 1));
            JsonObject function = item.getAsJsonObject("function");
            if (function == null) {
                continue;
            }
            String name = string(function, "name", "");
            String arguments = string(function, "arguments", "{}");
            calls.add(new AiToolCall(id, name, arguments));
        }
        return List.copyOf(calls);
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return fallback;
        }
        String text = value.getAsString();
        return text == null || text.isBlank() ? fallback : text.trim();
    }
}
