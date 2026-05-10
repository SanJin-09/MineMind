package com.sanjin.minemind.ai;

public final class AiQuotaStatus {
    private AiQuotaStatus() {
    }

    public static String describe(AiProviderSettings settings) {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            return "未配置";
        }
        if (AiProviderRegistry.OPENAI_PROVIDER_ID.equals(settings.providerId())) {
            return "暂不支持自动获取，请在 OpenAI 控制台查看";
        }
        return "暂不支持自动获取";
    }
}
