package com.sanjin.minemind.ai;

import java.util.List;

public interface AiProvider {
    String id();

    String displayName();

    String defaultBaseUrl();

    String complete(AiProviderSettings settings, List<AiMessage> messages) throws AiException;

    default AiCompletion completeWithTools(AiProviderSettings settings, List<AiMessage> messages, List<AiToolSpec> tools) throws AiException {
        return AiCompletion.text(complete(settings, messages));
    }

    default boolean supportsNativeToolCalls() {
        return false;
    }

    List<String> fetchModelIds(AiProviderSettings settings) throws AiException;

    default boolean supportsImageInput(AiProviderSettings settings) {
        return AiModelCapabilities.supportsImageInput(settings);
    }
}
