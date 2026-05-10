package com.sanjin.minemind.ai;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AiProviderRegistry {
    public static final String OPENAI_PROVIDER_ID = "openai";
    public static final String DEEPSEEK_PROVIDER_ID = "deepseek";

    private static final Map<String, AiProvider> PROVIDERS = new LinkedHashMap<>();

    static {
        register(new OpenAiCompatibleProvider(
                OPENAI_PROVIDER_ID,
                "OpenAI",
                "https://api.openai.com/v1"
        ));
        register(new OpenAiCompatibleProvider(
                DEEPSEEK_PROVIDER_ID,
                "DeepSeek",
                "https://api.deepseek.com"
        ));
    }

    private AiProviderRegistry() {
    }

    public static String normalizeProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new AiConfigStore.ConfigException("服务商不能为空");
        }
        return providerId.trim().toLowerCase(Locale.ROOT);
    }

    public static AiProvider provider(String providerId) {
        String normalized = normalizeProviderId(providerId);
        AiProvider provider = PROVIDERS.get(normalized);
        return provider == null ? OpenAiCompatibleProvider.custom(normalized) : provider;
    }

    public static Optional<AiProvider> registeredProvider(String providerId) {
        String normalized = normalizeProviderId(providerId);
        return Optional.ofNullable(PROVIDERS.get(normalized));
    }

    public static List<AiProvider> registeredProviders() {
        return List.copyOf(PROVIDERS.values());
    }

    public static List<String> registeredProviderIds() {
        return List.copyOf(PROVIDERS.keySet());
    }

    public static List<String> providerSuggestions(Collection<String> configuredProviderIds) {
        return registeredProviderIds();
    }

    public static String prettyProviderName(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return "Custom";
        }
        String normalized = providerId.trim().toLowerCase(Locale.ROOT);
        AiProvider registered = PROVIDERS.get(normalized);
        if (registered != null) {
            return registered.displayName();
        }
        return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
    }

    private static void register(AiProvider provider) {
        PROVIDERS.put(provider.id(), provider);
    }
}
