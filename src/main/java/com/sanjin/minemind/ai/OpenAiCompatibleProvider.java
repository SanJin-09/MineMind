package com.sanjin.minemind.ai;

import java.util.List;

public final class OpenAiCompatibleProvider implements AiProvider {
    private final String id;
    private final String displayName;
    private final String defaultBaseUrl;
    private final String recommendedModelId;
    private final List<String> recommendedModelIds;
    private final List<String> aliases;

    public OpenAiCompatibleProvider(
            String id,
            String displayName,
            String defaultBaseUrl,
            String recommendedModelId,
            List<String> recommendedModelIds,
            List<String> aliases
    ) {
        this.id = id;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.recommendedModelId = recommendedModelId;
        this.recommendedModelIds = List.copyOf(recommendedModelIds);
        this.aliases = List.copyOf(aliases);
    }

    public static OpenAiCompatibleProvider custom(String id) {
        return new OpenAiCompatibleProvider(
                id,
                AiProviderRegistry.prettyProviderName(id),
                "",
                "",
                List.of(),
                List.of()
        );
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
    public String recommendedModelId() {
        return recommendedModelId;
    }

    @Override
    public List<String> recommendedModelIds() {
        return recommendedModelIds;
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }

    @Override
    public String complete(AiProviderSettings settings, List<AiMessage> messages) throws AiException {
        return OpenAiChatClient.complete(settings, messages);
    }

    @Override
    public List<String> fetchModelIds(AiProviderSettings settings) throws AiException {
        return AiModelCatalog.fetchModelIds(settings);
    }
}
