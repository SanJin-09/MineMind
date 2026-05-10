package com.sanjin.minemind.ai;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class AiChat {
    private static final int CHAT_PART_LENGTH = 240;
    private static final String DEFAULT_PREFIX = "AI";

    private AiChat() {
    }

    public static void info(String message) {
        add(styledLine(prefix() + " ", message, ChatFormatting.DARK_AQUA, ChatFormatting.WHITE));
    }

    public static void infoHighlight(String message) {
        add(styledLine(prefix() + " ", message, ChatFormatting.DARK_AQUA, ChatFormatting.YELLOW));
    }

    public static void error(String message) {
        add(styledLine(prefix() + " 错误：", message, ChatFormatting.DARK_RED, ChatFormatting.RED));
    }

    public static void player(String message) {
        for (String part : split(message)) {
            add(styledLine("玩家 > ", part, ChatFormatting.GREEN, ChatFormatting.GRAY));
        }
    }

    public static void assistant(String message) {
        for (String part : split(message)) {
            add(styledLine(assistantName() + " > ", part, ChatFormatting.YELLOW, ChatFormatting.GRAY));
        }
    }

    private static Component styledLine(String prefix, String body, ChatFormatting prefixColor, ChatFormatting bodyColor) {
        return Component.literal(prefix)
                .withStyle(prefixColor)
                .append(Component.literal(body).withStyle(bodyColor));
    }

    private static String prefix() {
        return "[" + DEFAULT_PREFIX + "]";
    }

    private static String assistantName() {
        try {
            AiProviderSettings settings = AiConfigStore.currentSettings();
            if (settings.displayName() != null && !settings.displayName().isBlank()) {
                return settings.displayName();
            }
        } catch (RuntimeException ignored) {
            return DEFAULT_PREFIX;
        }
        return DEFAULT_PREFIX;
    }

    private static void add(Component component) {
        Minecraft minecraft = Minecraft.getInstance();
        Runnable task = () -> {
            if (minecraft.gui != null) {
                minecraft.gui.getChat().addMessage(component);
            }
        };
        if (minecraft.isSameThread()) {
            task.run();
        } else {
            minecraft.execute(task);
        }
    }

    private static List<String> split(String message) {
        List<String> parts = new ArrayList<>();
        String remaining = message == null ? "" : message.trim();
        if (remaining.isEmpty()) {
            parts.add("");
            return parts;
        }
        while (remaining.length() > CHAT_PART_LENGTH) {
            int cut = findCut(remaining);
            parts.add(remaining.substring(0, cut));
            remaining = remaining.substring(cut).stripLeading();
        }
        parts.add(remaining);
        return parts;
    }

    private static int findCut(String message) {
        int limit = Math.min(CHAT_PART_LENGTH, message.length());
        int newline = message.lastIndexOf('\n', limit);
        if (newline > 80) {
            return newline;
        }
        int space = message.lastIndexOf(' ', limit);
        if (space > 80) {
            return space;
        }
        return limit;
    }
}
