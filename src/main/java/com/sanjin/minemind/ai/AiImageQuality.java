package com.sanjin.minemind.ai;

import java.util.List;
import java.util.Locale;

public final class AiImageQuality {
    public static final String DEFAULT = "medium";
    private static final List<String> IDS = List.of("low", "medium", "high");

    private AiImageQuality() {
    }

    public static List<String> ids() {
        return IDS;
    }

    public static String sanitize(String value) {
        String normalized = normalize(value);
        return IDS.contains(normalized) ? normalized : DEFAULT;
    }

    public static String require(String value) {
        String normalized = normalize(value);
        if (!IDS.contains(normalized)) {
            throw new AiConfigStore.ConfigException("截图质量必须是 low、medium 或 high");
        }
        return normalized;
    }

    public static int maxDimension(String value) {
        return switch (sanitize(value)) {
            case "low" -> 768;
            case "high" -> 1920;
            default -> 1280;
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
