package com.sanjin.minemind.ai;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientChatEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import java.time.LocalDateTime;
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
        AiToolRequest parsedRequest = AiToolRequest.parse(cleanedPrompt);
        if (parsedRequest.hasMemoryAction()) {
            handleMemoryAction(cleanedPrompt, parsedRequest);
            return;
        }
        if (!REQUESTING.compareAndSet(false, true)) {
            AiChat.error("已有 AI 请求处理中，请稍后再试");
            return;
        }

        int session = SESSION.get();
        AiProviderSettings settings;
        AiProvider provider;
        AiToolRequest toolRequest;
        AiToolContext toolContext;
        try {
            toolRequest = parsedRequest;
            toolContext = AiMinecraftTools.collect(toolRequest);
            if (toolRequest.memoryRequested()) {
                toolContext = toolContext.append(AiMemoryStore.readContextResult(toolRequest.userPrompt()));
            }
            settings = AiConfigStore.currentSettings();
            provider = AiProviderRegistry.provider(settings.providerId());
            if (toolRequest.imageRequested() && !provider.supportsImageInput(settings)) {
                REQUESTING.set(false);
                AiChat.error("当前模型不支持图片输入，请切换到支持图片输入的模型后再使用 @image");
                return;
            }
        } catch (AiMinecraftTools.AiToolException exception) {
            REQUESTING.set(false);
            AiChat.error("本地工具读取失败，请重试");
            return;
        } catch (AiMemoryStore.MemoryException exception) {
            REQUESTING.set(false);
            AiChat.error(exception.getMessage());
            return;
        } catch (RuntimeException exception) {
            REQUESTING.set(false);
            AiChat.error("本地配置或运行状态异常");
            return;
        }
        final AiProviderSettings requestSettings = settings;
        final AiProvider requestProvider = provider;
        final AiToolRequest requestToolRequest = toolRequest;
        final AiToolContext requestToolContext = toolContext;
        AiChat.player(cleanedPrompt);
        String attachedLabels = attachedLabels(requestToolContext, requestToolRequest.imageRequested());
        if (!attachedLabels.isBlank()) {
            AiChat.info("已附加：" + attachedLabels);
        }
        if (requestToolRequest.imageRequested()) {
            AiChat.info("附带图片的请求回复速度会稍慢");
            AiChat.info("截图可能包含聊天栏、坐标或服务器信息");
        }
        AiChat.info("正在等待回复......");

        if (requestToolRequest.imageRequested()) {
            try {
                AiScreenshot.capture(AiConfigStore.imageQuality()).whenComplete((image, throwable) -> {
                    if (throwable != null) {
                        Minecraft.getInstance().execute(() -> finishImageError(session));
                        return;
                    }
                    startCompletion(session, requestSettings, requestProvider, requestToolContext, requestToolRequest, image);
                });
            } catch (RuntimeException exception) {
                finishImageError(session);
            }
            return;
        }

        startCompletion(session, requestSettings, requestProvider, requestToolContext, requestToolRequest, null);
    }

    private static void startCompletion(
            int session,
            AiProviderSettings settings,
            AiProvider provider,
            AiToolContext toolContext,
            AiToolRequest toolRequest,
            AiImageAttachment image
    ) {
        if (session != SESSION.get()) {
            return;
        }

        AiMessage userMessage = image == null
                ? new AiMessage("user", toolRequest.userPrompt())
                : new AiMessage("user", toolRequest.userPrompt(), List.of(image));
        List<AiMessage> requestMessages;
        try {
            requestMessages = AiPrompt.withSystemPrompt(toolContext, HISTORY.snapshotWithUser(userMessage));
        } catch (RuntimeException exception) {
            Minecraft.getInstance().execute(() -> finishError(session, new AiException(AiErrorType.LOCAL)));
            return;
        }

        try {
            EXECUTOR.execute(() -> {
                try {
                    String answer = AiToolOrchestrator.complete(
                            provider,
                            settings,
                            requestMessages,
                            AiChat.playerName(),
                            toolRequest.userPrompt(),
                            AiConfigStore.autonomousToolPermissions()
                    );
                    Minecraft.getInstance().execute(() -> finishSuccess(session, toolRequest.userPrompt(), answer));
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

    private static void handleMemoryAction(String originalPrompt, AiToolRequest request) {
        if (request.memoryAction() == AiToolRequest.MemoryAction.REMEMBER) {
            rememberWithModel(originalPrompt, request);
            return;
        }

        forgetWithModel(originalPrompt, request);
    }

    private static void forgetWithModel(String originalPrompt, AiToolRequest request) {
        AiChat.player(originalPrompt);
        if (request.userPrompt().isBlank() && !request.hasTools()) {
            AiChat.error("遗忘内容不能为空");
            return;
        }
        if (!REQUESTING.compareAndSet(false, true)) {
            AiChat.error("已有 AI 请求处理中，请稍后再试");
            return;
        }

        int session = SESSION.get();
        LocalDateTime recordTime = LocalDateTime.now();
        AiProviderSettings settings;
        AiProvider provider;
        AiToolContext toolContext;
        List<AiMemoryStore.MemoryDeletionCandidate> candidates;
        List<AiMessage> requestMessages;
        try {
            toolContext = AiMinecraftTools.collect(request);
            candidates = AiMemoryStore.deletionCandidates(request.userPrompt());
            if (candidates.isEmpty()) {
                REQUESTING.set(false);
                AiChat.info("长期记忆为空，无需删除");
                return;
            }
            settings = AiConfigStore.currentSettings();
            provider = AiProviderRegistry.provider(settings.providerId());
            requestMessages = AiMemoryForgetDraft.messages(toolContext, candidates, request.userPrompt());
        } catch (AiMinecraftTools.AiToolException exception) {
            REQUESTING.set(false);
            AiChat.error("本地工具读取失败，请重试");
            return;
        } catch (AiMemoryStore.MemoryException exception) {
            REQUESTING.set(false);
            AiChat.error(exception.getMessage());
            return;
        } catch (RuntimeException exception) {
            REQUESTING.set(false);
            AiChat.error("本地配置或运行状态异常");
            return;
        }

        AiChat.info("尝试记忆删除，记录日期：" + recordTime.toLocalDate());
        AiChat.info("已附加：" + attachedMemoryText(toolContext));

        try {
            EXECUTOR.execute(() -> {
                try {
                    String answer = provider.complete(settings, requestMessages);
                    List<Integer> ids = AiMemoryForgetDraft.parseDeleteIds(answer, candidates);
                    AiMemoryStore.MemoryDeleteResult result = AiMemoryStore.forgetByIds(ids);
                    Minecraft.getInstance().execute(() -> finishForgetSuccess(session, result));
                } catch (AiException exception) {
                    Minecraft.getInstance().execute(() -> finishForgetAiError(session, exception));
                } catch (AiMemoryForgetDraft.DraftException exception) {
                    Minecraft.getInstance().execute(() -> finishForgetDraftError(session));
                } catch (AiMemoryStore.MemoryException exception) {
                    Minecraft.getInstance().execute(() -> finishForgetMemoryError(session, exception));
                } catch (RuntimeException exception) {
                    Minecraft.getInstance().execute(() -> finishForgetRuntimeError(session));
                }
            });
        } catch (RuntimeException exception) {
            finishForgetRuntimeError(session);
        }
    }

    private static void rememberWithModel(String originalPrompt, AiToolRequest request) {
        AiChat.player(originalPrompt);
        if (request.userPrompt().isBlank() && !request.hasTools()) {
            AiChat.error("记忆内容不能为空");
            return;
        }
        if (!REQUESTING.compareAndSet(false, true)) {
            AiChat.error("已有 AI 请求处理中，请稍后再试");
            return;
        }

        int session = SESSION.get();
        LocalDateTime recordTime = LocalDateTime.now();
        String playerName = AiChat.playerName();
        AiProviderSettings settings;
        AiProvider provider;
        AiToolContext toolContext;
        List<AiMessage> requestMessages;
        try {
            toolContext = AiMinecraftTools.collect(request);
            settings = AiConfigStore.currentSettings();
            provider = AiProviderRegistry.provider(settings.providerId());
            requestMessages = AiMemoryDraft.messages(toolContext, request.userPrompt());
        } catch (AiMinecraftTools.AiToolException exception) {
            REQUESTING.set(false);
            AiChat.error("本地工具读取失败，请重试");
            return;
        } catch (RuntimeException exception) {
            REQUESTING.set(false);
            AiChat.error("本地配置或运行状态异常");
            return;
        }

        AiChat.info("尝试记忆写入，记录日期：" + recordTime.toLocalDate());
        if (toolContext.hasResults()) {
            AiChat.info("已附加：" + toolContext.summaryLabels());
        }

        try {
            EXECUTOR.execute(() -> {
                try {
                    String answer = provider.complete(settings, requestMessages);
                    String memory = AiMemoryDraft.parseMemory(answer);
                    AiMemoryStore.MemoryWriteResult result = AiMemoryStore.remember(playerName, memory, recordTime);
                    Minecraft.getInstance().execute(() -> finishRememberSuccess(session, result));
                } catch (AiException exception) {
                    Minecraft.getInstance().execute(() -> finishRememberAiError(session, exception));
                } catch (AiMemoryDraft.DraftException exception) {
                    Minecraft.getInstance().execute(() -> finishRememberDraftError(session));
                } catch (AiMemoryStore.MemoryException exception) {
                    Minecraft.getInstance().execute(() -> finishRememberMemoryError(session, exception));
                } catch (RuntimeException exception) {
                    Minecraft.getInstance().execute(() -> finishRememberRuntimeError(session));
                }
            });
        } catch (RuntimeException exception) {
            finishRememberRuntimeError(session);
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
        fetchModels(settings, provider);
    }

    public static void refreshModelsAfterKeySet(String providerId) {
        AiProviderSettings settings;
        AiProvider provider;
        try {
            settings = AiConfigStore.settingsForProvider(providerId);
            provider = AiProviderRegistry.provider(settings.providerId());
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return;
        }

        AiChat.info("正在自动获取模型列表：" + settings.displayName());
        fetchModels(settings, provider);
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

    private static void finishImageError(int session) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error("截图失败，请重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishRememberSuccess(int session, AiMemoryStore.MemoryWriteResult result) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.info("记忆成功写入！本次写入内容：" + result.content());
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishRememberAiError(int session, AiException exception) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error(exception.getMessage());
            AiChat.error("本次记忆写入失败，请检查网络环境或 API 余额后重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishRememberDraftError(int session) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error("记忆整理失败，请重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishRememberMemoryError(int session, AiMemoryStore.MemoryException exception) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error(exception.getMessage());
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishRememberRuntimeError(int session) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error("本次记忆写入失败，请检查网络环境或 API 余额后重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishForgetSuccess(int session, AiMemoryStore.MemoryDeleteResult result) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            if (result.count() <= 0) {
                AiChat.info("记忆删除完成！本次删除内容：无");
            } else {
                AiChat.info("记忆成功删除！本次删除内容：" + result.summary());
            }
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishForgetAiError(int session, AiException exception) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error(exception.getMessage());
            AiChat.error("本次记忆删除失败，请检查网络环境或 API 余额后重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishForgetDraftError(int session) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error("记忆删除整理失败，请重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishForgetMemoryError(int session, AiMemoryStore.MemoryException exception) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error(exception.getMessage());
        } finally {
            REQUESTING.set(false);
        }
    }

    private static void finishForgetRuntimeError(int session) {
        if (session != SESSION.get()) {
            return;
        }
        try {
            AiChat.error("本次记忆删除失败，请检查网络环境或 API 余额后重试");
        } finally {
            REQUESTING.set(false);
        }
    }

    private static String attachedMemoryText(AiToolContext toolContext) {
        if (toolContext != null && toolContext.hasResults()) {
            return "长期记忆、" + toolContext.summaryLabels();
        }
        return "长期记忆";
    }

    private static String attachedLabels(AiToolContext toolContext, boolean imageRequested) {
        String labels = toolContext != null && toolContext.hasResults() ? toolContext.summaryLabels() : "";
        if (!imageRequested) {
            return labels;
        }
        if (labels.isBlank()) {
            return "图片";
        }
        return labels + "、图片";
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
        if (hasImageInputModels(settings, modelIds)) {
            AiChat.info("标记：[图片] 支持 @image 图片识别");
        }
        for (String line : joinModelLines(settings, modelIds)) {
            AiChat.info(line);
        }
    }

    private static void fetchModels(AiProviderSettings settings, AiProvider provider) {
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

    private static boolean hasImageInputModels(AiProviderSettings settings, List<String> modelIds) {
        for (String modelId : modelIds) {
            if (AiModelCapabilities.supportsImageInput(settings.providerId(), modelId)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> joinModelLines(AiProviderSettings settings, List<String> modelIds) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String modelId : modelIds) {
            String displayModelId = AiModelCapabilities.displayModelId(settings.providerId(), modelId);
            String part = line.isEmpty() ? displayModelId : ", " + displayModelId;
            if (!line.isEmpty() && line.length() + part.length() > 180) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(displayModelId);
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
