package com.sanjin.minemind.ai;

import java.util.Locale;

public final class AiHttpStatusClassifier {
    private AiHttpStatusClassifier() {
    }

    public static AiErrorType classify(int statusCode) {
        return classify(statusCode, "");
    }

    public static AiErrorType classify(int statusCode, String responseBody) {
        String body = responseBody == null ? "" : responseBody.toLowerCase(Locale.ROOT);
        if (containsAny(body, "invalid_api_key", "incorrect api key", "authentication", "unauthorized", "forbidden")) {
            return AiErrorType.AUTH;
        }
        if (containsAny(body,
                "insufficient_quota",
                "quota",
                "billing",
                "balance",
                "credit",
                "credits",
                "rate_limit",
                "rate limit",
                "limit exceeded",
                "额度",
                "余额"
        )) {
            return AiErrorType.QUOTA;
        }
        if (containsAny(body, "model_not_found", "model does not exist", "model_not_exist", "unknown model")) {
            return AiErrorType.MODEL;
        }
        return switch (statusCode) {
            case 400, 413, 422 -> AiErrorType.REQUEST;
            case 402, 429 -> AiErrorType.QUOTA;
            case 401, 403 -> AiErrorType.AUTH;
            case 404 -> AiErrorType.MODEL;
            case 408, 504 -> AiErrorType.TIMEOUT;
            default -> statusCode >= 500
                    ? AiErrorType.SERVICE
                    : AiErrorType.REQUEST;
        };
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
