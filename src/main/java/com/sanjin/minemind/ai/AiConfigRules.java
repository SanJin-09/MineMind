package com.sanjin.minemind.ai;

import java.net.URI;
import java.net.URISyntaxException;

public final class AiConfigRules {
    public static final int DEFAULT_TIMEOUT_SECONDS = 45;
    public static final int MIN_TIMEOUT_SECONDS = 5;
    public static final int MAX_TIMEOUT_SECONDS = 120;
    public static final int DEFAULT_MAX_HISTORY_MESSAGES = 20;
    public static final int MIN_MAX_HISTORY_MESSAGES = 2;
    public static final int MAX_MAX_HISTORY_MESSAGES = 100;

    private AiConfigRules() {
    }

    public static String normalizeProviderId(String providerId) {
        return AiProviderRegistry.normalizeProviderId(providerId);
    }

    public static int sanitizeTimeoutSeconds(int value) {
        if (value <= 0) {
            return DEFAULT_TIMEOUT_SECONDS;
        }
        return clamp(value, MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS);
    }

    public static int sanitizeMaxHistoryMessages(int value) {
        if (value <= 0) {
            return DEFAULT_MAX_HISTORY_MESSAGES;
        }
        return clamp(value, MIN_MAX_HISTORY_MESSAGES, MAX_MAX_HISTORY_MESSAGES);
    }

    public static void requireTimeoutSeconds(int value) {
        if (value < MIN_TIMEOUT_SECONDS || value > MAX_TIMEOUT_SECONDS) {
            throw new AiConfigStore.ConfigException("请求超时必须在 " + MIN_TIMEOUT_SECONDS + "-" + MAX_TIMEOUT_SECONDS + " 秒之间");
        }
    }

    public static void requireMaxHistoryMessages(int value) {
        if (value < MIN_MAX_HISTORY_MESSAGES || value > MAX_MAX_HISTORY_MESSAGES) {
            throw new AiConfigStore.ConfigException("上下文条数必须在 " + MIN_MAX_HISTORY_MESSAGES + "-" + MAX_MAX_HISTORY_MESSAGES + " 之间");
        }
    }

    public static String requireBaseUrl(String baseUrl) {
        String cleaned = normalizeBaseUrl(baseUrl);
        if (!isHttpUrl(cleaned)) {
            throw new AiConfigStore.ConfigException("API Base URL 必须是 http 或 https 地址");
        }
        return cleaned;
    }

    public static String repairBaseUrl(String providerId, String baseUrl) {
        String cleaned = normalizeBaseUrl(baseUrl);
        if (isHttpUrl(cleaned)) {
            return cleaned;
        }
        return AiProviderRegistry.registeredProvider(providerId)
                .map(AiProvider::defaultBaseUrl)
                .orElse("");
    }

    public static String normalizeBaseUrl(String baseUrl) {
        String result = baseUrl == null ? "" : baseUrl.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
