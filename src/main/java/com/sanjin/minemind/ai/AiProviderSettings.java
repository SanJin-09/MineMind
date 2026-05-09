package com.sanjin.minemind.ai;

public record AiProviderSettings(
        String providerId,
        String displayName,
        String model,
        String baseUrl,
        String apiKey,
        int timeoutSeconds
) {
}
