package com.sanjin.minemind.ai;

public final class AiHttpStatusClassifier {
    private AiHttpStatusClassifier() {
    }

    public static AiErrorType classify(int statusCode) {
        return switch (statusCode) {
            case 400, 413, 422 -> AiErrorType.REQUEST;
            case 401, 403 -> AiErrorType.AUTH;
            case 404 -> AiErrorType.MODEL;
            case 408, 504 -> AiErrorType.TIMEOUT;
            case 429 -> AiErrorType.QUOTA;
            default -> statusCode >= 500
                    ? AiErrorType.SERVICE
                    : AiErrorType.REQUEST;
        };
    }
}
