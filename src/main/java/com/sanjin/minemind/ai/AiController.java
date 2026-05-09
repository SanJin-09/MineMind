package com.sanjin.minemind.ai;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class AiController {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MineMind AI Request");
        thread.setDaemon(true);
        return thread;
    });
    private static final AiConversationHistory HISTORY = new AiConversationHistory();
    private static final AtomicBoolean REQUESTING = new AtomicBoolean(false);
    private static final AtomicInteger SESSION = new AtomicInteger();

    private AiController() {
    }

    public static void onClientChat(ClientChatEvent event) {
        if (!AiConfigStore.isAiMode()) {
            return;
        }
        String message = event.getMessage();
        if (message == null || message.isBlank() || message.startsWith("/")) {
            return;
        }
        event.setCanceled(true);
        ask(message);
    }

    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        resetSession();
        if (AiConfigStore.isAiMode() && isSingleplayerWorld()) {
            AiProviderSettings settings = AiConfigStore.currentSettings();
            AiChat.info("已进入 AI 对话模式，输入 /ai off 退出");
            AiChat.info("当前模型：" + settings.displayName() + " / " + settings.model());
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        resetSession();
    }

    public static boolean enableAiMode() {
        if (!isSingleplayerWorld()) {
            AiChat.error("MVP 仅支持单人游戏");
            return false;
        }
        try {
            AiConfigStore.setAiMode(true);
            AiProviderSettings settings = AiConfigStore.currentSettings();
            AiChat.info("已进入 AI 对话模式，输入 /ai off 退出");
            AiChat.info("当前模型：" + settings.displayName() + " / " + settings.model());
            return true;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return false;
        }
    }

    public static boolean disableAiMode() {
        try {
            AiConfigStore.setAiMode(false);
            AiChat.info("已退出 AI 对话模式");
            return true;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return false;
        }
    }

    public static void ask(String prompt) {
        if (!isSingleplayerWorld()) {
            AiChat.error("MVP 仅支持单人游戏");
            return;
        }
        String cleanedPrompt = prompt == null ? "" : prompt.trim();
        if (cleanedPrompt.isEmpty()) {
            AiChat.error("提问内容不能为空");
            return;
        }
        if (!REQUESTING.compareAndSet(false, true)) {
            AiChat.error("已有 AI 请求处理中，请稍后再试");
            return;
        }

        int session = SESSION.get();
        AiProviderSettings settings;
        List<AiMessage> requestMessages;
        try {
            settings = AiConfigStore.currentSettings();
            requestMessages = HISTORY.snapshotWithUser(cleanedPrompt);
        } catch (RuntimeException exception) {
            REQUESTING.set(false);
            AiChat.error("本地配置或运行状态异常");
            return;
        }
        AiChat.player(cleanedPrompt);
        AiChat.info("正在请求：" + settings.displayName() + " / " + settings.model());

        try {
            EXECUTOR.execute(() -> {
                try {
                    String answer = OpenAiChatClient.complete(settings, requestMessages);
                    Minecraft.getInstance().execute(() -> finishSuccess(session, cleanedPrompt, answer));
                } catch (AiException exception) {
                    Minecraft.getInstance().execute(() -> finishError(session, exception));
                } catch (RuntimeException exception) {
                    Minecraft.getInstance().execute(() -> finishError(session, new AiException(AiErrorType.LOCAL)));
                }
            });
        } catch (RuntimeException exception) {
            finishError(session, new AiException(AiErrorType.LOCAL));
        }
    }

    public static void clearHistory() {
        int count = HISTORY.clear();
        AiChat.info("已清空当前对话上下文，共 " + count + " 条");
    }

    public static List<String> statusLines() {
        return AiConfigStore.statusLines(isSingleplayerWorld(), REQUESTING.get(), HISTORY.size());
    }

    public static void trimHistoryToMax() {
        HISTORY.trimTo(AiConfigStore.maxHistoryMessages());
    }

    public static boolean isSingleplayerWorld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.hasSingleplayerServer();
    }

    private static void finishSuccess(int session, String prompt, String answer) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            HISTORY.addExchange(prompt, answer, AiConfigStore.maxHistoryMessages());
            AiChat.assistant(answer);
            AiChat.info("请求完成");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishError(int session, AiException exception) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error(exception.getMessage());
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void resetSession() {
        SESSION.incrementAndGet();
        REQUESTING.set(false);
        HISTORY.clear();
    }
}
