package com.sanjin.minemind.ai;

import java.util.List;

public record AiMessage(String role, String content, List<AiImageAttachment> images) {
    public AiMessage {
        role = role == null || role.isBlank() ? "user" : role.trim();
        content = content == null ? "" : content;
        images = images == null ? List.of() : List.copyOf(images);
    }

    public AiMessage(String role, String content) {
        this(role, content, List.of());
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }
}
