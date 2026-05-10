package com.sanjin.minemind.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AiProviderRegistry {
    public static final String OPENAI_PROVIDER_ID = "openai";
    public static final String DEEPSEEK_PROVIDER_ID = "deepseek";

    private static final Map<String, AiProvider> PROVIDERS = new LinkedHashMap<>();
    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    static {
        register(new OpenAiCompatibleProvider(
                OPENAI_PROVIDER_ID,
                "OpenAI",
                "https://api.openai.com/v1",
                "gpt-4.1",
                List.of("gpt-4.1"),
                List.of("chatgpt")
        ));
        register(new OpenAiCompatibleProvider(
                DEEPSEEK_PROVIDER_ID,
                "DeepSeek",
                "https://api.deepseek.com",
                "deepseek-chat",
                List.of("deepseek-chat", "deepseek-reasoner"),
                List.of()
        ));
    }

    private AiProviderRegistry() {
    }

    public static String normalizeProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new AiConfigStore.ConfigException("服务商不能为空");
        }
        String normalized = providerId.trim().toLowerCase(Locale.ROOT);
        return ALIASES.getOrDefault(normalized, normalized);
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
        Set<String> suggestions = new LinkedHashSet<>();
        for (AiProvider provider : PROVIDERS.values()) {
            suggestions.add(provider.id());
            suggestions.addAll(provider.aliases());
        }
        if (configuredProviderIds != null) {
            for (String providerId : configuredProviderIds) {
                if (providerId != null && !providerId.isBlank()) {
                    suggestions.add(providerId.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return new ArrayList<>(suggestions);
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
        for (String alias : provider.aliases()) {
            ALIASES.put(alias.toLowerCase(Locale.ROOT), provider.id());
        }
    }
}
