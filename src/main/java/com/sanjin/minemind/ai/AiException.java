package com.sanjin.minemind.ai;

public class AiException extends Exception {
    private final AiErrorType type;

    public AiException(AiErrorType type) {
        super(type.message());
        this.type = type;
    }

    public AiException(AiErrorType type, String message) {
        super(message);
        this.type = type;
    }

    public AiException(AiErrorType type, Throwable cause) {
        super(type.message(), cause);
        this.type = type;
    }

    public AiErrorType type() {
        return type;
    }
}
