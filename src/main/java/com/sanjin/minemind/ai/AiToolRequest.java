package com.sanjin.minemind.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiToolRequest {
    public static final String DEFAULT_PROMPT = "请根据已附加的 Minecraft 信息进行简短说明。";
    private static final Pattern TOOL_MARKER = Pattern.compile("(?i)(?<!\\S)@(hotbar|inventory|here|nearby|target)(?!\\S)");

    private final String originalPrompt;
    private final String userPrompt;
    private final List<Tool> tools;

    private AiToolRequest(String originalPrompt, String userPrompt, List<Tool> tools) {
        this.originalPrompt = originalPrompt;
        this.userPrompt = userPrompt;
        this.tools = List.copyOf(tools);
    }

    public static AiToolRequest parse(String prompt) {
        String original = prompt == null ? "" : prompt.trim();
        Matcher matcher = TOOL_MARKER.matcher(original);
        Set<Tool> tools = new LinkedHashSet<>();
        StringBuilder cleaned = new StringBuilder();
        while (matcher.find()) {
            Tool tool = Tool.fromMarker(matcher.group(1));
            if (tool != null) {
                tools.add(tool);
                matcher.appendReplacement(cleaned, " ");
            }
        }
        matcher.appendTail(cleaned);
        if (tools.contains(Tool.INVENTORY)) {
            tools.remove(Tool.HOTBAR);
        }

        String userPrompt = cleaned.toString().replaceAll("\\s+", " ").trim();
        if (userPrompt.isBlank() && !tools.isEmpty()) {
            userPrompt = DEFAULT_PROMPT;
        }
        return new AiToolRequest(original, userPrompt, new ArrayList<>(tools));
    }

    public String originalPrompt() {
        return originalPrompt;
    }

    public String userPrompt() {
        return userPrompt;
    }

    public List<Tool> tools() {
        return tools;
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }

    public enum Tool {
        HOTBAR("@hotbar", "tool.hotbar.read", "快捷栏"),
        INVENTORY("@inventory", "tool.inventory.read", "背包"),
        HERE("@here", "tool.location.read", "位置"),
        NEARBY("@nearby", "tool.entities.nearby", "附近生物"),
        TARGET("@target", "tool.target.read", "准星目标");

        private final String marker;
        private final String toolId;
        private final String label;

        Tool(String marker, String toolId, String label) {
            this.marker = marker;
            this.toolId = toolId;
            this.label = label;
        }

        public String marker() {
            return marker;
        }

        public String toolId() {
            return toolId;
        }

        public String label() {
            return label;
        }

        private static Tool fromMarker(String marker) {
            if (marker == null) {
                return null;
            }
            String normalized = marker.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "hotbar" -> HOTBAR;
                case "inventory" -> INVENTORY;
                case "here" -> HERE;
                case "nearby" -> NEARBY;
                case "target" -> TARGET;
                default -> null;
            };
        }
    }
}
