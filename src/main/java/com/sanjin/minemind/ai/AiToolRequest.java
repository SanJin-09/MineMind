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
    public static final String DEFAULT_IMAGE_PROMPT = "请根据已附加的游戏截图进行简短说明。";
    public static final String DEFAULT_MEMORY_PROMPT = "请根据已附加的长期记忆进行简短说明。";
    public static final String DEFAULT_REMEMBER_PROMPT = "请根据已附加的 Minecraft 信息整理一条长期记忆。";
    public static final String DEFAULT_FORGET_PROMPT = "请根据已附加的 Minecraft 信息判断需要删除的长期记忆。";
    private static final Pattern MEMORY_ACTION = Pattern.compile("(?i)^@(remember|forget)(?:\\s+(.*))?$");
    private static final Pattern TOOL_MARKER = Pattern.compile("(?i)(?<!\\S)@(hotbar|inventory|here|nearby|target|memory|image)(?:[:：])?(?!\\S)");
    private static final Pattern LOCAL_TOOL_MARKER = Pattern.compile("(?i)(?<!\\S)@(hotbar|inventory|here|nearby|target)(?:[:：])?(?!\\S)");

    private final String originalPrompt;
    private final String userPrompt;
    private final List<Tool> tools;
    private final boolean memoryRequested;
    private final boolean imageRequested;
    private final MemoryAction memoryAction;
    private final String memoryActionText;

    private AiToolRequest(
            String originalPrompt,
            String userPrompt,
            List<Tool> tools,
            boolean memoryRequested,
            boolean imageRequested,
            MemoryAction memoryAction,
            String memoryActionText
    ) {
        this.originalPrompt = originalPrompt;
        this.userPrompt = userPrompt;
        this.tools = List.copyOf(tools);
        this.memoryRequested = memoryRequested;
        this.imageRequested = imageRequested;
        this.memoryAction = memoryAction;
        this.memoryActionText = memoryActionText == null ? "" : memoryActionText.trim();
    }

    public static AiToolRequest parse(String prompt) {
        String original = prompt == null ? "" : prompt.trim();
        Matcher actionMatcher = MEMORY_ACTION.matcher(original);
        if (actionMatcher.matches()) {
            MemoryAction action = MemoryAction.fromMarker(actionMatcher.group(1));
            String content = actionMatcher.group(2) == null ? "" : actionMatcher.group(2).trim();
            if (action == MemoryAction.REMEMBER || action == MemoryAction.FORGET) {
                ParsedLocalTools parsed = parseLocalTools(content);
                String userPrompt = parsed.userPrompt();
                if (userPrompt.isBlank() && !parsed.tools().isEmpty()) {
                    userPrompt = action == MemoryAction.REMEMBER ? DEFAULT_REMEMBER_PROMPT : DEFAULT_FORGET_PROMPT;
                }
                return new AiToolRequest(original, userPrompt, parsed.tools(), false, false, action, userPrompt);
            }
            return new AiToolRequest(original, content, List.of(), false, false, action, content);
        }

        Matcher matcher = TOOL_MARKER.matcher(original);
        Set<Tool> tools = new LinkedHashSet<>();
        StringBuilder cleaned = new StringBuilder();
        boolean memoryRequested = false;
        boolean imageRequested = false;
        while (matcher.find()) {
            String marker = matcher.group(1);
            if ("memory".equalsIgnoreCase(marker)) {
                memoryRequested = true;
                matcher.appendReplacement(cleaned, " ");
                continue;
            }
            if ("image".equalsIgnoreCase(marker)) {
                imageRequested = true;
                matcher.appendReplacement(cleaned, " ");
                continue;
            }

            Tool tool = Tool.fromMarker(marker);
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
        if (userPrompt.isBlank() && (!tools.isEmpty() || memoryRequested || imageRequested)) {
            if (memoryRequested && tools.isEmpty() && !imageRequested) {
                userPrompt = DEFAULT_MEMORY_PROMPT;
            } else if (imageRequested && tools.isEmpty() && !memoryRequested) {
                userPrompt = DEFAULT_IMAGE_PROMPT;
            } else {
                userPrompt = DEFAULT_PROMPT;
            }
        }
        return new AiToolRequest(original, userPrompt, new ArrayList<>(tools), memoryRequested, imageRequested, null, "");
    }

    private static ParsedLocalTools parseLocalTools(String prompt) {
        String original = prompt == null ? "" : prompt.trim();
        Matcher matcher = LOCAL_TOOL_MARKER.matcher(original);
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
        return new ParsedLocalTools(cleaned.toString().replaceAll("\\s+", " ").trim(), new ArrayList<>(tools));
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

    public boolean memoryRequested() {
        return memoryRequested;
    }

    public boolean imageRequested() {
        return imageRequested;
    }

    public boolean hasMemoryAction() {
        return memoryAction != null;
    }

    public MemoryAction memoryAction() {
        return memoryAction;
    }

    public String memoryActionText() {
        return memoryActionText;
    }

    public enum MemoryAction {
        REMEMBER,
        FORGET;

        private static MemoryAction fromMarker(String marker) {
            if (marker == null) {
                return null;
            }
            return switch (marker.toLowerCase(Locale.ROOT)) {
                case "remember" -> REMEMBER;
                case "forget" -> FORGET;
                default -> null;
            };
        }
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

    private record ParsedLocalTools(String userPrompt, List<Tool> tools) {
    }
}
