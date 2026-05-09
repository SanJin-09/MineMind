package com.sanjin.minemind.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AiConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData config;
    private static boolean loadFailed;

    private AiConfigStore() {
    }

    public static synchronized boolean isAiMode() {
        return data().aiMode;
    }

    public static synchronized void setAiMode(boolean enabled) {
        data().aiMode = enabled;
        save();
    }

    public static synchronized int maxHistoryMessages() {
        return data().maxHistoryMessages;
    }

    public static synchronized AiProviderSettings currentSettings() {
        ConfigData data = data();
        ProviderConfig provider = provider(data.currentProvider);
        return new AiProviderSettings(
                data.currentProvider,
                provider.displayName,
                provider.model,
                provider.baseUrl,
                provider.apiKey,
                data.timeoutSeconds
        );
    }

    public static synchronized void setProviderModel(String providerId, String model) {
        String normalized = normalizeProvider(providerId);
        if (model == null || model.isBlank()) {
            throw new ConfigException("模型 ID 不能为空");
        }
        ConfigData data = data();
        ProviderConfig provider = provider(normalized);
        provider.model = model.trim();
        data.currentProvider = normalized;
        save();
    }

    public static synchronized void setApiKey(String providerId, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ConfigException("API Key 不能为空");
        }
        provider(normalizeProvider(providerId)).apiKey = apiKey.trim();
        save();
    }

    public static synchronized void removeApiKey(String providerId) {
        provider(normalizeProvider(providerId)).apiKey = "";
        save();
    }

    public static synchronized void setBaseUrl(String providerId, String baseUrl) {
        String normalized = normalizeProvider(providerId);
        String cleaned = stripTrailingSlash(baseUrl == null ? "" : baseUrl.trim());
        if (!isHttpUrl(cleaned)) {
            throw new ConfigException("API Base URL 必须是 http 或 https 地址");
        }
        provider(normalized).baseUrl = cleaned;
        save();
    }

    public static synchronized List<String> providerIds() {
        return new ArrayList<>(data().providers.keySet());
    }

    public static synchronized List<String> statusLines() {
        ConfigData data = data();
        List<String> lines = new ArrayList<>();
        if (loadFailed) {
            lines.add("本地配置文件损坏或无法读取，已使用默认配置");
        }
        ProviderConfig current = provider(data.currentProvider);
        lines.add("当前模型：" + current.displayName + " / " + emptyText(current.model, "未设置"));
        lines.add("当前 Base URL：" + emptyText(current.baseUrl, "未设置"));
        for (Map.Entry<String, ProviderConfig> entry : data.providers.entrySet()) {
            ProviderConfig provider = entry.getValue();
            String key = provider.apiKey == null || provider.apiKey.isBlank()
                    ? "未配置 Key"
                    : "已配置 Key，尾号 " + mask(provider.apiKey);
            lines.add(provider.displayName + ": " + key);
        }
        return lines;
    }

    public static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "****";
        }
        String key = apiKey.trim();
        int tail = Math.min(4, key.length());
        return "****" + key.substring(key.length() - tail);
    }

    private static ConfigData data() {
        if (config == null) {
            config = load();
        }
        ensureDefaults(config);
        return config;
    }

    private static ProviderConfig provider(String providerId) {
        ConfigData data = data();
        String normalized = normalizeProvider(providerId);
        ProviderConfig provider = data.providers.get(normalized);
        if (provider == null) {
            provider = ProviderConfig.custom(normalized);
            data.providers.put(normalized, provider);
        }
        return provider;
    }

    private static ConfigData load() {
        Path path = path();
        if (!Files.exists(path)) {
            return ConfigData.defaults();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            loadFailed = false;
            return loaded == null ? ConfigData.defaults() : loaded;
        } catch (IOException | JsonParseException exception) {
            loadFailed = true;
            return ConfigData.defaults();
        }
    }

    private static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data(), writer);
            }
        } catch (IOException exception) {
            throw new ConfigException("本地配置文件保存失败");
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("minemind.json");
    }

    private static void ensureDefaults(ConfigData data) {
        if (data.currentProvider == null || data.currentProvider.isBlank()) {
            data.currentProvider = "openai";
        } else {
            data.currentProvider = normalizeProvider(data.currentProvider);
        }
        if (data.providers == null) {
            data.providers = new LinkedHashMap<>();
        }
        data.providers.putIfAbsent("openai", ProviderConfig.openAi());
        if (data.timeoutSeconds <= 0) {
            data.timeoutSeconds = 45;
        }
        if (data.maxHistoryMessages <= 0) {
            data.maxHistoryMessages = 20;
        }
        for (Map.Entry<String, ProviderConfig> entry : data.providers.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(ProviderConfig.custom(entry.getKey()));
            } else {
                entry.getValue().ensure(entry.getKey());
            }
        }
    }

    private static String normalizeProvider(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new ConfigException("服务商不能为空");
        }
        return providerId.trim().toLowerCase(Locale.ROOT);
    }

    private static String stripTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static String emptyText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static class ConfigData {
        public String currentProvider = "openai";
        public int timeoutSeconds = 45;
        public int maxHistoryMessages = 20;
        public boolean streaming = false;
        public boolean aiMode = false;
        public Map<String, ProviderConfig> providers = new LinkedHashMap<>();

        static ConfigData defaults() {
            ConfigData data = new ConfigData();
            data.providers.put("openai", ProviderConfig.openAi());
            return data;
        }
    }

    public static class ProviderConfig {
        public String displayName;
        public String model;
        public String baseUrl;
        public String apiKey = "";

        static ProviderConfig openAi() {
            ProviderConfig provider = new ProviderConfig();
            provider.displayName = "OpenAI";
            provider.model = "gpt-4.1";
            provider.baseUrl = "https://api.openai.com/v1";
            return provider;
        }

        static ProviderConfig custom(String providerId) {
            ProviderConfig provider = new ProviderConfig();
            provider.displayName = prettyName(providerId);
            provider.model = "";
            provider.baseUrl = "";
            return provider;
        }

        void ensure(String providerId) {
            if (displayName == null || displayName.isBlank()) {
                displayName = prettyName(providerId);
            }
            if (model == null) {
                model = "";
            }
            if (baseUrl == null) {
                baseUrl = "";
            }
            if (apiKey == null) {
                apiKey = "";
            }
        }

        private static String prettyName(String providerId) {
            if ("openai".equals(providerId)) {
                return "OpenAI";
            }
            return providerId.substring(0, 1).toUpperCase(Locale.ROOT) + providerId.substring(1);
        }
    }

    public static class ConfigException extends RuntimeException {
        public ConfigException(String message) {
            super(message);
        }
    }
}
