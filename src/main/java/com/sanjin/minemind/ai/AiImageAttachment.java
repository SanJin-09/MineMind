package com.sanjin.minemind.ai;

public record AiImageAttachment(String mediaType, String base64Data, int width, int height) {
    public AiImageAttachment {
        mediaType = mediaType == null || mediaType.isBlank() ? "image/png" : mediaType.trim();
        base64Data = base64Data == null ? "" : base64Data.trim();
        width = Math.max(0, width);
        height = Math.max(0, height);
    }

    public String dataUrl() {
        return "data:" + mediaType + ";base64," + base64Data;
    }
}
