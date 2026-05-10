package com.sanjin.minemind.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    public static synchronized int timeoutSeconds() {
        return data().timeoutSeconds;
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

    public static synchronized AiProviderSettings settingsForProvider(String providerId) {
        ConfigData data = data();
        String normalized = AiConfigRules.normalizeProviderId(providerId);
        ProviderConfig provider = provider(normalized);
        return new AiProviderSettings(
                normalized,
                provider.displayName,
                provider.model,
                provider.baseUrl,
                provider.apiKey,
                data.timeoutSeconds
        );
    }

    public static synchronized void setProviderModel(String providerId, String model) {
        String normalized = AiConfigRules.normalizeProviderId(providerId);
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
        provider(AiConfigRules.normalizeProviderId(providerId)).apiKey = apiKey.trim();
        save();
    }

    public static synchronized void removeApiKey(String providerId) {
        provider(AiConfigRules.normalizeProviderId(providerId)).apiKey = "";
        save();
    }

    public static synchronized void setBaseUrl(String providerId, String baseUrl) {
        String normalized = AiConfigRules.normalizeProviderId(providerId);
        String cleaned = AiConfigRules.requireBaseUrl(baseUrl);
        provider(normalized).baseUrl = cleaned;
        save();
    }

    public static synchronized void setTimeoutSeconds(int timeoutSeconds) {
        AiConfigRules.requireTimeoutSeconds(timeoutSeconds);
        data().timeoutSeconds = timeoutSeconds;
        save();
    }

    public static synchronized void setMaxHistoryMessages(int maxHistoryMessages) {
        AiConfigRules.requireMaxHistoryMessages(maxHistoryMessages);
        data().maxHistoryMessages = maxHistoryMessages;
        save();
    }

    public static synchronized List<String> providerIds() {
        return AiProviderRegistry.providerSuggestions(data().providers.keySet());
    }

    public static synchronized List<String> statusLines(boolean singleplayerWorld, boolean requesting, int historyMessages) {
        ConfigData data = data();
        List<String> lines = new ArrayList<>();
        if (loadFailed) {
            lines.add("本地配置文件损坏或无法读取，已使用默认配置");
        }
        ProviderConfig current = provider(data.currentProvider);
        lines.add("AI 模式：" + (data.aiMode ? "已开启" : "已关闭"));
        lines.add("单人世界：" + (singleplayerWorld ? "是" : "否"));
        lines.add("请求状态：" + (requesting ? "处理中" : "空闲"));
        lines.add("当前服务商：" + current.displayName + " (" + data.currentProvider + ")");
        lines.add("当前模型：" + emptyText(current.model, "未设置"));
        lines.add("当前 Base URL：" + emptyText(current.baseUrl, "未设置"));
        lines.add("请求超时：" + data.timeoutSeconds + " 秒");
        lines.add("最大上下文：" + data.maxHistoryMessages + " 条");
        lines.add("当前上下文：" + historyMessages + " 条");
        lines.addAll(keyStatusLines());
        return lines;
    }

    public static synchronized List<String> keyStatusLines() {
        ConfigData data = data();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, ProviderConfig> entry : data.providers.entrySet()) {
            ProviderConfig provider = entry.getValue();
            String key = provider.apiKey == null || provider.apiKey.isBlank()
                    ? "未配置 Key"
                    : "已配置 Key，尾号 " + mask(provider.apiKey);
            lines.add(provider.displayName + " (" + entry.getKey() + "): " + key);
        }
        return lines;
    }

    public static synchronized List<String> configuredKeyStatusLines() {
        ConfigData data = data();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, ProviderConfig> entry : data.providers.entrySet()) {
            ProviderConfig provider = entry.getValue();
            if (provider.apiKey != null && !provider.apiKey.isBlank()) {
                lines.add(provider.displayName + " (" + entry.getKey() + "): 已配置 Key，尾号 " + mask(provider.apiKey));
            }
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
            if (ensureDefaults(config)) {
                save(config);
            }
        } else {
            ensureDefaults(config);
        }
        return config;
    }

    private static ProviderConfig provider(String providerId) {
        ConfigData data = data();
        String normalized = AiConfigRules.normalizeProviderId(providerId);
        ProviderConfig provider = data.providers.get(normalized);
        if (provider == null) {
            provider = ProviderConfig.defaultFor(normalized);
            data.providers.put(normalized, provider);
        }
        return provider;
    }

    private static ConfigData load() {
        Path path = path();
        if (!Files.exists(path)) {
            loadFailed = false;
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
        save(data());
    }

    private static void save(ConfigData data) {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException exception) {
            throw new ConfigException("本地配置文件保存失败");
        }
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("minemind.json");
    }

    private static boolean ensureDefaults(ConfigData data) {
        boolean changed = false;
        if (data.currentProvider == null || data.currentProvider.isBlank()) {
            data.currentProvider = AiProviderRegistry.OPENAI_PROVIDER_ID;
            changed = true;
        } else {
            String normalized = AiConfigRules.normalizeProviderId(data.currentProvider);
            changed |= !normalized.equals(data.currentProvider);
            data.currentProvider = normalized;
        }
        if (data.providers == null) {
            data.providers = new LinkedHashMap<>();
            changed = true;
        }
        changed |= normalizeProviderKeys(data);
        for (AiProvider provider : AiProviderRegistry.registeredProviders()) {
            if (!data.providers.containsKey(provider.id())) {
                data.providers.put(provider.id(), ProviderConfig.fromProvider(provider));
                changed = true;
            }
        }
        int sanitizedTimeout = AiConfigRules.sanitizeTimeoutSeconds(data.timeoutSeconds);
        if (sanitizedTimeout != data.timeoutSeconds) {
            data.timeoutSeconds = sanitizedTimeout;
            changed = true;
        }
        int sanitizedHistory = AiConfigRules.sanitizeMaxHistoryMessages(data.maxHistoryMessages);
        if (sanitizedHistory != data.maxHistoryMessages) {
            data.maxHistoryMessages = sanitizedHistory;
            changed = true;
        }
        for (Map.Entry<String, ProviderConfig> entry : data.providers.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(ProviderConfig.defaultFor(entry.getKey()));
                changed = true;
            } else {
                changed |= entry.getValue().ensure(entry.getKey());
            }
        }
        return changed;
    }

    private static boolean normalizeProviderKeys(ConfigData data) {
        boolean changed = false;
        Map<String, ProviderConfig> normalizedProviders = new LinkedHashMap<>();
        for (Map.Entry<String, ProviderConfig> entry : data.providers.entrySet()) {
            String original = entry.getKey();
            String normalized;
            try {
                normalized = AiConfigRules.normalizeProviderId(original);
            } catch (ConfigException exception) {
                changed = true;
                continue;
            }
            ProviderConfig existing = normalizedProviders.get(normalized);
            if (existing == null) {
                normalizedProviders.put(normalized, entry.getValue());
            } else {
                mergeProviderConfig(existing, entry.getValue());
                changed = true;
            }
            changed |= !normalized.equals(original);
        }
        if (changed) {
            data.providers = normalizedProviders;
        }
        return changed;
    }

    private static void mergeProviderConfig(ProviderConfig target, ProviderConfig source) {
        if (target == null || source == null) {
            return;
        }
        if (isBlank(target.displayName) && !isBlank(source.displayName)) {
            target.displayName = source.displayName;
        }
        if (isBlank(target.model) && !isBlank(source.model)) {
            target.model = source.model;
        }
        if (isBlank(target.baseUrl) && !isBlank(source.baseUrl)) {
            target.baseUrl = source.baseUrl;
        }
        if (isBlank(target.apiKey) && !isBlank(source.apiKey)) {
            target.apiKey = source.apiKey;
        }
    }

    private static String emptyText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class ConfigData {
        public String currentProvider = AiProviderRegistry.OPENAI_PROVIDER_ID;
        public int timeoutSeconds = AiConfigRules.DEFAULT_TIMEOUT_SECONDS;
        public int maxHistoryMessages = AiConfigRules.DEFAULT_MAX_HISTORY_MESSAGES;
        public boolean streaming = false;
        public boolean aiMode = false;
        public Map<String, ProviderConfig> providers = new LinkedHashMap<>();

        static ConfigData defaults() {
            ConfigData data = new ConfigData();
            for (AiProvider provider : AiProviderRegistry.registeredProviders()) {
                data.providers.put(provider.id(), ProviderConfig.fromProvider(provider));
            }
            return data;
        }
    }

    public static class ProviderConfig {
        public String displayName;
        public String model;
        public String baseUrl;
        public String apiKey = "";

        static ProviderConfig fromProvider(AiProvider aiProvider) {
            ProviderConfig provider = new ProviderConfig();
            provider.displayName = aiProvider.displayName();
            provider.model = "";
            provider.baseUrl = aiProvider.defaultBaseUrl();
            return provider;
        }

        static ProviderConfig defaultFor(String providerId) {
            return AiProviderRegistry.registeredProvider(providerId)
                    .map(ProviderConfig::fromProvider)
                    .orElseGet(() -> custom(providerId));
        }

        static ProviderConfig custom(String providerId) {
            ProviderConfig provider = new ProviderConfig();
            provider.displayName = prettyName(providerId);
            provider.model = "";
            provider.baseUrl = "";
            return provider;
        }

        boolean ensure(String providerId) {
            boolean changed = false;
            AiProvider registeredProvider = AiProviderRegistry.registeredProvider(providerId).orElse(null);
            if (displayName == null || displayName.isBlank()) {
                displayName = registeredProvider == null ? prettyName(providerId) : registeredProvider.displayName();
                changed = true;
            }
            if (model == null) {
                model = "";
                changed = true;
            }
            String repairedBaseUrl = AiConfigRules.repairBaseUrl(providerId, baseUrl);
            if (!repairedBaseUrl.equals(baseUrl)) {
                baseUrl = repairedBaseUrl;
                changed = true;
            }
            if (apiKey == null) {
                apiKey = "";
                changed = true;
            }
            return changed;
        }

        private static String prettyName(String providerId) {
            return AiProviderRegistry.prettyProviderName(providerId);
        }
    }

    public static class ConfigException extends RuntimeException {
        public ConfigException(String message) {
            super(message);
        }
    }
}
