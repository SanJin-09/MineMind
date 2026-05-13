package com.sanjin.minemind.ai;

import java.util.List;

public final class OpenAiCompatibleProvider implements AiProvider {
    private final String id;
    private final String displayName;
    private final String defaultBaseUrl;

    public OpenAiCompatibleProvider(
            String id,
            String displayName,
            String defaultBaseUrl
    ) {
        this.id = id;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public static OpenAiCompatibleProvider custom(String id) {
        return new OpenAiCompatibleProvider(
                id,
                AiProviderRegistry.prettyProviderName(id),
                ""
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
    public String complete(AiProviderSettings settings, List<AiMessage> messages) throws AiException {
        return OpenAiChatClient.complete(settings, messages);
    }

    @Override
    public AiCompletion completeWithTools(AiProviderSettings settings, List<AiMessage> messages, List<AiToolSpec> tools) throws AiException {
        return OpenAiChatClient.completeWithTools(settings, messages, tools);
    }

    @Override
    public boolean supportsNativeToolCalls() {
        return true;
    }

    @Override
    public List<String> fetchModelIds(AiProviderSettings settings) throws AiException {
        return AiModelCatalog.fetchModelIds(settings);
    }
}
