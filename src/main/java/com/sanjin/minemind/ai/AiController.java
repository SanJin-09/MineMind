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
        showStartupConfig();
        if (AiConfigStore.isAiMode() && isSingleplayerWorld()) {
            AiChat.info("已进入 AI 对话模式，输入 /ai off 退出");
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
            AiChat.info("当前模型：" + modelText(settings));
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
        AiProvider provider;
        List<AiMessage> requestMessages;
        try {
            settings = AiConfigStore.currentSettings();
            provider = AiProviderRegistry.provider(settings.providerId());
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
                    String answer = provider.complete(settings, requestMessages);
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

    public static void listModels(String providerId) {
        AiProviderSettings settings;
        AiProvider provider;
        try {
            settings = AiConfigStore.settingsForProvider(providerId);
            provider = AiProviderRegistry.provider(settings.providerId());
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return;
        }

        AiChat.info("正在获取模型列表：" + settings.displayName());
        EXECUTOR.execute(() -> {
            try {
                List<String> modelIds = provider.fetchModelIds(settings);
                Minecraft.getInstance().execute(() -> showModelList(settings, modelIds));
            } catch (AiException exception) {
                Minecraft.getInstance().execute(() -> AiChat.error(exception.getMessage()));
            } catch (RuntimeException exception) {
                Minecraft.getInstance().execute(() -> AiChat.error(AiErrorType.LOCAL.message()));
            }
        });
    }

    public static void selectModel(String providerId, String modelId) {
        String cleanedModelId = modelId == null ? "" : modelId.trim();
        if (cleanedModelId.isEmpty()) {
            AiChat.error("模型 ID 不能为空");
            return;
        }

        AiProviderSettings settings;
        AiProvider provider;
        try {
            settings = AiConfigStore.settingsForProvider(providerId);
            provider = AiProviderRegistry.provider(settings.providerId());
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return;
        }

        AiChat.info("正在校验模型列表：" + settings.displayName());
        EXECUTOR.execute(() -> {
            try {
                List<String> modelIds = provider.fetchModelIds(settings);
                Minecraft.getInstance().execute(() -> applyModelSelection(providerId, cleanedModelId, modelIds));
            } catch (AiException exception) {
                Minecraft.getInstance().execute(() -> AiChat.error(exception.getMessage()));
            } catch (RuntimeException exception) {
                Minecraft.getInstance().execute(() -> AiChat.error(AiErrorType.LOCAL.message()));
            }
        });
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

    private static void showStartupConfig() {
        try {
            AiProviderSettings settings = AiConfigStore.currentSettings();
            AiChat.infoHighlight("============ MineMind AI 配置 =============");
            AiChat.info("当前模型：" + modelText(settings));
            AiChat.info("剩余额度：" + AiQuotaStatus.describe(settings));
        } catch (RuntimeException exception) {
            AiChat.infoHighlight("============ MineMind AI 配置 =============");
            AiChat.info("当前模型：未配置");
            AiChat.info("剩余额度：未配置");
        }
    }

    private static void showModelList(AiProviderSettings settings, List<String> modelIds) {
        AiChat.infoHighlight("============ MineMind AI 可选模型 =============");
        AiChat.info("服务商：" + settings.displayName() + " (" + settings.providerId() + ")");
        AiChat.info("可选型号：" + modelIds.size() + " 个");
        for (String line : joinModelLines(modelIds)) {
            AiChat.info(line);
        }
    }

    private static void applyModelSelection(String providerId, String modelId, List<String> modelIds) {
        if (!modelIds.contains(modelId)) {
            AiChat.error("模型 ID 不在当前服务商可选列表中，请先执行 /ai model <provider> 查看");
            return;
        }
        try {
            AiConfigStore.setProviderModel(providerId, modelId);
            AiProviderSettings settings = AiConfigStore.currentSettings();
            AiChat.info("当前模型已切换为：" + settings.displayName() + " / " + settings.model());
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
        }
    }

    private static List<String> joinModelLines(List<String> modelIds) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String modelId : modelIds) {
            String part = line.isEmpty() ? modelId : ", " + modelId;
            if (!line.isEmpty() && line.length() + part.length() > 180) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(modelId);
            } else {
                line.append(part);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private static String modelText(AiProviderSettings settings) {
        if (settings.model() == null || settings.model().isBlank()) {
            return "未配置";
        }
        if (settings.displayName() == null || settings.displayName().isBlank()) {
            return settings.model();
        }
        return settings.displayName() + " / " + settings.model();
    }

    private static void resetSession() {
        SESSION.incrementAndGet();
        REQUESTING.set(false);
        HISTORY.clear();
    }
}
