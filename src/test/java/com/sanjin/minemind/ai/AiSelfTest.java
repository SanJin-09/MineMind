package com.sanjin.minemind.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class AiSelfTest {
    private AiSelfTest() {
    }

    public static void main(String[] args) {
        testProviderRules();
        testTimeoutRules();
        testHistoryRules();
        testBaseUrlRules();
        testHttpStatusClassification();
        testResponseBodyErrorClassification();
        testModelCatalogFiltering();
        testImageQualityRules();
        testImageCapabilities();
        testToolRequestParsing();
        testImageRequestParsing();
        testMemoryRequestParsing();
        testToolContextFormatting();
        testMemoryDraft();
        testMemoryForgetDraft();
        testMemoryStore();
        testAutonomousToolRegistry();
        testAutonomousToolPermissions();
        testAutonomousMemoryPolicy();
        testOpenAiToolPayload();
        testOpenAiToolCallParsing();
        testJsonFallbackParsing();
        testToolRunLimits();
        testPromptInjection();
        testConversationHistory();
        testScreenshotSizing();
        System.out.println("AiSelfTest passed");
    }

    private static void testProviderRules() {
        assertEquals("openai", AiConfigRules.normalizeProviderId("OpenAI"), "openai normalize");
        assertEquals("deepseek", AiConfigRules.normalizeProviderId("DeepSeek"), "deepseek normalize");
        assertEquals("gemini", AiConfigRules.normalizeProviderId(" Gemini "), "gemini normalize");

        AiProvider openai = AiProviderRegistry.provider("openai");
        assertEquals("openai", openai.id(), "openai provider id");
        assertEquals("OpenAI", openai.displayName(), "openai display name");
        assertEquals("https://api.openai.com/v1", openai.defaultBaseUrl(), "openai base url");

        AiProvider deepseek = AiProviderRegistry.provider("deepseek");
        assertEquals("deepseek", deepseek.id(), "deepseek provider id");
        assertEquals("DeepSeek", deepseek.displayName(), "deepseek display name");
        assertEquals("https://api.deepseek.com", deepseek.defaultBaseUrl(), "deepseek base url");

        AiProvider qwen = AiProviderRegistry.provider("qwen");
        assertEquals("qwen", qwen.id(), "qwen provider id");
        assertEquals("Qwen", qwen.displayName(), "qwen display name");
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", qwen.defaultBaseUrl(), "qwen base url");

        AiProvider kimi = AiProviderRegistry.provider("kimi");
        assertEquals("kimi", kimi.id(), "kimi provider id");
        assertEquals("KiMi", kimi.displayName(), "kimi display name");
        assertEquals("https://api.moonshot.ai/v1", kimi.defaultBaseUrl(), "kimi base url");

        AiProvider glm = AiProviderRegistry.provider("glm");
        assertEquals("glm", glm.id(), "glm provider id");
        assertEquals("GLM", glm.displayName(), "glm display name");
        assertEquals("https://open.bigmodel.cn/api/paas/v4", glm.defaultBaseUrl(), "glm base url");

        AiProvider seed = AiProviderRegistry.provider("seed");
        assertEquals("seed", seed.id(), "seed provider id");
        assertEquals("Seed", seed.displayName(), "seed display name");
        assertEquals("https://ark.cn-beijing.volces.com/api/v3", seed.defaultBaseUrl(), "seed base url");

        AiProvider grok = AiProviderRegistry.provider("grok");
        assertEquals("grok", grok.id(), "grok provider id");
        assertEquals("Grok", grok.displayName(), "grok display name");
        assertEquals("https://api.x.ai/v1", grok.defaultBaseUrl(), "grok base url");

        AiProvider gemini = AiProviderRegistry.provider("gemini");
        assertEquals("gemini", gemini.id(), "gemini provider id");
        assertEquals("Gemini", gemini.displayName(), "gemini display name");
        assertEquals("https://generativelanguage.googleapis.com/v1beta", gemini.defaultBaseUrl(), "gemini base url");

        List<String> suggestions = AiProviderRegistry.providerSuggestions(List.of("custom"));
        assertContains(suggestions, "openai", "provider suggestions openai");
        assertContains(suggestions, "deepseek", "provider suggestions deepseek");
        assertContains(suggestions, "qwen", "provider suggestions qwen");
        assertContains(suggestions, "kimi", "provider suggestions kimi");
        assertContains(suggestions, "glm", "provider suggestions glm");
        assertContains(suggestions, "seed", "provider suggestions seed");
        assertContains(suggestions, "grok", "provider suggestions grok");
        assertContains(suggestions, "gemini", "provider suggestions gemini");
        assertNotContains(suggestions, "chatgpt", "provider suggestions no chatgpt alias");
        assertNotContains(suggestions, "custom", "provider suggestions only built-in providers");
    }

    private static void testTimeoutRules() {
        assertEquals(45, AiConfigRules.sanitizeTimeoutSeconds(0), "timeout default");
        assertEquals(5, AiConfigRules.sanitizeTimeoutSeconds(1), "timeout min clamp");
        assertEquals(120, AiConfigRules.sanitizeTimeoutSeconds(500), "timeout max clamp");
        assertEquals(60, AiConfigRules.sanitizeTimeoutSeconds(60), "timeout valid");
    }

    private static void testHistoryRules() {
        assertEquals(20, AiConfigRules.sanitizeMaxHistoryMessages(0), "history default");
        assertEquals(2, AiConfigRules.sanitizeMaxHistoryMessages(1), "history min clamp");
        assertEquals(100, AiConfigRules.sanitizeMaxHistoryMessages(500), "history max clamp");
        assertEquals(30, AiConfigRules.sanitizeMaxHistoryMessages(30), "history valid");
    }

    private static void testBaseUrlRules() {
        assertEquals("https://example.com/v1", AiConfigRules.normalizeBaseUrl(" https://example.com/v1/// "), "base normalize");
        assertTrue(AiConfigRules.isHttpUrl("https://example.com/v1"), "https base url");
        assertTrue(AiConfigRules.isHttpUrl("http://localhost:8000/v1"), "http base url");
        assertFalse(AiConfigRules.isHttpUrl("ftp://example.com"), "reject ftp");
        assertEquals("https://api.openai.com/v1", AiConfigRules.repairBaseUrl("openai", "bad-url"), "openai base repair");
        assertEquals("https://api.deepseek.com", AiConfigRules.repairBaseUrl("deepseek", "bad-url"), "deepseek base repair");
        assertEquals("https://dashscope.aliyuncs.com/compatible-mode/v1", AiConfigRules.repairBaseUrl("qwen", "bad-url"), "qwen base repair");
        assertEquals("https://api.moonshot.ai/v1", AiConfigRules.repairBaseUrl("kimi", "bad-url"), "kimi base repair");
        assertEquals("https://open.bigmodel.cn/api/paas/v4", AiConfigRules.repairBaseUrl("glm", "bad-url"), "glm base repair");
        assertEquals("https://ark.cn-beijing.volces.com/api/v3", AiConfigRules.repairBaseUrl("seed", "bad-url"), "seed base repair");
        assertEquals("https://api.x.ai/v1", AiConfigRules.repairBaseUrl("grok", "bad-url"), "grok base repair");
        assertEquals("https://generativelanguage.googleapis.com/v1beta", AiConfigRules.repairBaseUrl("gemini", "bad-url"), "gemini base repair");
        assertEquals("", AiConfigRules.repairBaseUrl("custom", "bad-url"), "custom base repair");
    }

    private static void testHttpStatusClassification() {
        assertEquals(AiErrorType.AUTH, AiHttpStatusClassifier.classify(401), "401 auth");
        assertEquals(AiErrorType.AUTH, AiHttpStatusClassifier.classify(403), "403 auth");
        assertEquals(AiErrorType.MODEL, AiHttpStatusClassifier.classify(404), "404 model");
        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(402), "402 quota");
        assertEquals(AiErrorType.TIMEOUT, AiHttpStatusClassifier.classify(408), "408 timeout");
        assertEquals(AiErrorType.TIMEOUT, AiHttpStatusClassifier.classify(504), "504 timeout");
        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(429), "429 quota");
        assertEquals(AiErrorType.SERVICE, AiHttpStatusClassifier.classify(500), "500 service");
        assertEquals(AiErrorType.REQUEST, AiHttpStatusClassifier.classify(400), "400 request");
    }

    private static void testResponseBodyErrorClassification() {
        String quotaBody = "{\"error\":{\"code\":\"insufficient_quota\",\"message\":\"You exceeded your current quota\"}}";
        String balanceBody = "{\"error\":{\"message\":\"账户余额不足\"}}";
        String modelBody = "{\"error\":{\"code\":\"model_not_found\",\"message\":\"The model does not exist\"}}";
        String geminiAuthBody = "{\"error\":{\"status\":\"PERMISSION_DENIED\",\"message\":\"API key not valid\"}}";
        String geminiQuotaBody = "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Quota exceeded\"}}";
        String geminiModelBody = "{\"error\":{\"status\":\"NOT_FOUND\",\"message\":\"Requested entity was not found\"}}";

        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(404, quotaBody), "quota body wins over 404");
        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(400, balanceBody), "balance body wins over 400");
        assertEquals(AiErrorType.MODEL, AiHttpStatusClassifier.classify(400, modelBody), "model body wins over 400");
        assertEquals(AiErrorType.AUTH, AiHttpStatusClassifier.classify(400, geminiAuthBody), "gemini auth body wins over 400");
        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(400, geminiQuotaBody), "gemini quota body wins over 400");
        assertEquals(AiErrorType.MODEL, AiHttpStatusClassifier.classify(400, geminiModelBody), "gemini model body wins over 400");
    }

    private static void testModelCatalogFiltering() {
        assertTrue(AiModelCatalog.isTextChatModelId("gpt-4o"), "gpt text model");
        assertTrue(AiModelCatalog.isTextChatModelId("ft:gpt-4o:org:name:id"), "fine tuned gpt text model");
        assertTrue(AiModelCatalog.isTextChatModelId("deepseek-chat"), "deepseek chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("qwen-max"), "qwen text model");
        assertTrue(AiModelCatalog.isTextChatModelId("moonshot-v1-32k"), "moonshot text model");
        assertTrue(AiModelCatalog.isTextChatModelId("kimi-k2-0905-preview"), "kimi text model");
        assertTrue(AiModelCatalog.isTextChatModelId("glm-4.7"), "glm text model");
        assertTrue(AiModelCatalog.isTextChatModelId("doubao-seed-1-6-250615"), "seed text model");
        assertTrue(AiModelCatalog.isTextChatModelId("grok-4.3"), "grok text model");
        assertTrue(AiModelCatalog.isTextChatModelId("gemini-2.5-pro"), "gemini text model");
        assertTrue(AiModelCatalog.isTextChatModelId("claude-3-5-sonnet"), "claude text model");
        assertTrue(AiModelCatalog.isTextChatModelId("qwen2.5-vl-72b-instruct"), "qwen vl chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("qwen-vl-ocr"), "qwen vl ocr chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("moonshot-v1-32k-vision-preview"), "kimi vision chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("glm-4v-plus"), "glm vision chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("glm-4.6v"), "glm 4.6v chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("doubao-seed-1-6-vision-250615"), "seed vision chat model");
        assertEquals(List.of(), AiModelCatalog.cachedModelIds("deepseek"), "deepseek has no default model suggestions");

        assertFalse(AiModelCatalog.isTextChatModelId("text-embedding-3-small"), "embedding rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("gpt-image-1"), "image rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("doubao-seedream-4-0"), "seedream image generation rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("gpt-4o-audio-preview"), "audio rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("whisper-1"), "whisper rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("omni-moderation-latest"), "moderation rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("qwen-omni-turbo"), "qwen omni rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("gemini-2.5-pro-preview-tts"), "gemini tts rejected");
    }

    private static void testImageQualityRules() {
        assertEquals("medium", AiImageQuality.sanitize(null), "image quality null defaults");
        assertEquals("medium", AiImageQuality.sanitize("bad"), "image quality invalid defaults");
        assertEquals("low", AiImageQuality.sanitize(" LOW "), "image quality normalize");
        assertEquals(768, AiImageQuality.maxDimension("low"), "low image max dimension");
        assertEquals(1280, AiImageQuality.maxDimension("medium"), "medium image max dimension");
        assertEquals(1920, AiImageQuality.maxDimension("high"), "high image max dimension");
        assertThrowsConfig(() -> AiImageQuality.require("ultra"), "image quality rejects invalid value");
    }

    private static void testImageCapabilities() {
        assertTrue(AiModelCapabilities.supportsImageInput(settings("openai", "gpt-4o")), "openai gpt-4o supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("openai", "gpt-4.1")), "openai gpt-4.1 supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("openai", "gpt-5.5")), "openai gpt-5 supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("gemini", "gemini-2.5-pro")), "gemini supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("qwen", "qwen2.5-vl-72b-instruct")), "qwen vl supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("qwen", "qwen3.6-plus")), "qwen3.6 supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("kimi", "moonshot-v1-32k-vision-preview")), "kimi vision supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("kimi", "kimi-k2.6")), "kimi k2.6 supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("glm", "glm-4.6v")), "glm 4.6v supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("glm", "glm-4v-plus-0111")), "glm 4v supports image");
        assertTrue(AiModelCapabilities.supportsImageInput(settings("seed", "doubao-seed-1-6-vision-250615")), "seed vision supports image");
        assertFalse(AiModelCapabilities.supportsImageInput(settings("openai", "gpt-image-1")), "image generation model is not chat image input");
        assertFalse(AiModelCapabilities.supportsImageInput(settings("deepseek", "deepseek-chat")), "deepseek image unsupported");
        assertFalse(AiModelCapabilities.supportsImageInput(settings("seed", "doubao-seedream-4-0")), "seed image generation unsupported");
        assertFalse(AiModelCapabilities.supportsImageInput(settings("qwen", "qwen-max")), "qwen text model image unsupported");
        assertEquals("glm-4.6v [图片]", AiModelCapabilities.displayModelId("glm", "glm-4.6v"), "image model list marker");
        assertEquals("deepseek-chat", AiModelCapabilities.displayModelId("deepseek", "deepseek-chat"), "text model no image marker");
    }

    private static void testToolRequestParsing() {
        AiToolRequest request = AiToolRequest.parse("@HOTBAR @inventory @hotbar 我能合成火把吗？ @unknown");
        assertEquals("我能合成火把吗？ @unknown", request.userPrompt(), "known markers removed and unknown kept");
        assertEquals(1, request.tools().size(), "inventory deduplicates hotbar");
        assertEquals(AiToolRequest.Tool.INVENTORY, request.tools().get(0), "inventory remains");

        AiToolRequest onlyTools = AiToolRequest.parse("@here @nearby");
        assertEquals(AiToolRequest.DEFAULT_PROMPT, onlyTools.userPrompt(), "default prompt for marker-only request");
        assertEquals(2, onlyTools.tools().size(), "marker-only tools kept");

        AiToolRequest noTools = AiToolRequest.parse("@unknown hello");
        assertEquals("@unknown hello", noTools.userPrompt(), "unknown marker remains ordinary text");
        assertFalse(noTools.hasTools(), "unknown marker does not trigger tools");
    }

    private static void testImageRequestParsing() {
        AiToolRequest image = AiToolRequest.parse("@IMAGE 这是什么？");
        assertTrue(image.imageRequested(), "image marker detected");
        assertEquals("这是什么？", image.userPrompt(), "image marker removed");
        assertFalse(image.hasTools(), "image marker does not trigger text tools");

        AiToolRequest imageWithColon = AiToolRequest.parse("@image: 这是什么？");
        assertTrue(imageWithColon.imageRequested(), "image marker with colon detected");
        assertEquals("这是什么？", imageWithColon.userPrompt(), "image marker with colon removed");

        AiToolRequest imageWithTools = AiToolRequest.parse("@here: @image 我现在在哪里？");
        assertTrue(imageWithTools.imageRequested(), "image with tools detected");
        assertEquals("我现在在哪里？", imageWithTools.userPrompt(), "image with tools prompt");
        assertEquals(1, imageWithTools.tools().size(), "image with here tool");
        assertEquals(AiToolRequest.Tool.HERE, imageWithTools.tools().get(0), "image with here remains");

        AiToolRequest onlyImage = AiToolRequest.parse("@image");
        assertTrue(onlyImage.imageRequested(), "image-only detected");
        assertEquals(AiToolRequest.DEFAULT_IMAGE_PROMPT, onlyImage.userPrompt(), "image-only default prompt");

        AiToolRequest rememberImage = AiToolRequest.parse("@remember @image 记住当前画面");
        assertTrue(rememberImage.hasMemoryAction(), "remember image still memory action");
        assertFalse(rememberImage.imageRequested(), "remember does not trigger image capture");
        assertEquals("@image 记住当前画面", rememberImage.userPrompt(), "remember keeps image marker as text");
    }

    private static void testMemoryRequestParsing() {
        AiToolRequest remember = AiToolRequest.parse("@remember @here 我的基地在樱花林旁边");
        assertTrue(remember.hasMemoryAction(), "remember action detected");
        assertEquals(AiToolRequest.MemoryAction.REMEMBER, remember.memoryAction(), "remember action");
        assertEquals("我的基地在樱花林旁边", remember.memoryActionText(), "remember content");
        assertEquals("我的基地在樱花林旁边", remember.userPrompt(), "remember tool marker removed");
        assertEquals(1, remember.tools().size(), "remember tool kept");
        assertEquals(AiToolRequest.Tool.HERE, remember.tools().get(0), "remember here tool");

        AiToolRequest rememberInventory = AiToolRequest.parse("@remember @inventory @hotbar 记住我的物资");
        assertEquals("记住我的物资", rememberInventory.userPrompt(), "remember inventory prompt");
        assertEquals(1, rememberInventory.tools().size(), "remember inventory deduplicates hotbar");
        assertEquals(AiToolRequest.Tool.INVENTORY, rememberInventory.tools().get(0), "remember inventory remains");

        AiToolRequest rememberMemory = AiToolRequest.parse("@remember @memory foo");
        assertFalse(rememberMemory.memoryRequested(), "remember does not trigger memory read");
        assertEquals("@memory foo", rememberMemory.userPrompt(), "remember keeps memory marker as text");
        assertEquals(0, rememberMemory.tools().size(), "remember memory marker not treated as local tool");

        AiToolRequest rememberOnlyTool = AiToolRequest.parse("@remember @here");
        assertEquals(AiToolRequest.DEFAULT_REMEMBER_PROMPT, rememberOnlyTool.userPrompt(), "remember tool-only default prompt");
        assertEquals(1, rememberOnlyTool.tools().size(), "remember tool-only keeps tool");

        AiToolRequest forget = AiToolRequest.parse("@FORGET @here 删除当前基地位置");
        assertTrue(forget.hasMemoryAction(), "forget action detected");
        assertEquals(AiToolRequest.MemoryAction.FORGET, forget.memoryAction(), "forget action");
        assertEquals("删除当前基地位置", forget.memoryActionText(), "forget prompt");
        assertEquals("删除当前基地位置", forget.userPrompt(), "forget tool marker removed");
        assertEquals(1, forget.tools().size(), "forget tool kept");
        assertEquals(AiToolRequest.Tool.HERE, forget.tools().get(0), "forget here tool");

        AiToolRequest forgetMemory = AiToolRequest.parse("@forget @memory foo");
        assertFalse(forgetMemory.memoryRequested(), "forget does not trigger memory read");
        assertEquals("@memory foo", forgetMemory.userPrompt(), "forget keeps memory marker as text");

        AiToolRequest forgetOnlyTool = AiToolRequest.parse("@forget @target");
        assertEquals(AiToolRequest.DEFAULT_FORGET_PROMPT, forgetOnlyTool.userPrompt(), "forget tool-only default prompt");
        assertEquals(1, forgetOnlyTool.tools().size(), "forget tool-only keeps tool");

        AiToolRequest memory = AiToolRequest.parse("我的基地在哪里？ @MEMORY @unknown");
        assertTrue(memory.memoryRequested(), "memory marker detected");
        assertEquals("我的基地在哪里？ @unknown", memory.userPrompt(), "memory marker removed");

        AiToolRequest onlyMemory = AiToolRequest.parse("@memory");
        assertTrue(onlyMemory.memoryRequested(), "memory-only marker detected");
        assertEquals(AiToolRequest.DEFAULT_MEMORY_PROMPT, onlyMemory.userPrompt(), "memory-only default prompt");

        AiToolRequest middleRemember = AiToolRequest.parse("请解释 @remember foo");
        assertFalse(middleRemember.hasMemoryAction(), "remember only acts at prompt start");
        assertEquals("请解释 @remember foo", middleRemember.userPrompt(), "middle remember remains ordinary text");
    }

    private static void testToolContextFormatting() {
        AiToolResult hotbar = new AiToolResult("tool.hotbar.read", "@hotbar", "快捷栏", "slot 1: minecraft:stone x64", false);
        AiToolContext context = AiToolContext.of(List.of(hotbar));
        assertTrue(context.hasResults(), "context has result");
        assertEquals("快捷栏", context.summaryLabels(), "context summary labels");
        assertTrue(context.toPromptText().contains("Minecraft Tool Context"), "context header");
        assertTrue(context.toPromptText().contains("tool.hotbar.read"), "context tool id");
        assertTrue(context.toPromptText().contains("触发标记: @hotbar"), "context trigger marker");
        assertTrue(context.toPromptText().contains("裁剪: 否"), "context clipping hint");

        String longContent = "x".repeat(AiToolContext.MAX_RESULT_CHARS + 50);
        AiToolContext truncated = AiToolContext.of(List.of(new AiToolResult("tool.inventory.read", "@inventory", "背包", longContent, false)));
        assertTrue(truncated.toPromptText().contains("单项工具结果已裁剪"), "single tool result truncates");
        assertTrue(truncated.toPromptText().contains("裁剪: 是"), "single tool result truncation hint");

        AiToolContext appended = context.append(new AiToolResult("tool.memory.read", "@memory", "长期记忆", "base in forest", false));
        assertEquals("快捷栏、长期记忆", appended.summaryLabels(), "context append memory label");
    }

    private static void testMemoryDraft() {
        assertEquals("我的基地在樱花林旁边。", AiMemoryDraft.parseMemory("{\"memory\":\"我的基地在樱花林旁边。\"}"), "draft parses json");
        assertEquals("基地坐标 x=1 y=2 z=3。", AiMemoryDraft.parseMemory("""
                ```json
                {"memory":"基地坐标 x=1 y=2 z=3。"}
                ```
                """), "draft parses fenced json");

        AiToolContext context = AiToolContext.of(List.of(new AiToolResult("tool.location.read", "@here", "位置", "x=1,y=2,z=3", false)));
        List<AiMessage> messages = AiMemoryDraft.messages(context, "我的基地在这里");
        assertEquals(3, messages.size(), "draft messages include tool context");
        assertEquals("system", messages.get(0).role(), "draft system role");
        assertTrue(messages.get(0).content().contains("JSON"), "draft system requires json");
        assertTrue(messages.get(1).content().contains("Minecraft Tool Context"), "draft includes tool context");
        assertEquals("我的基地在这里", messages.get(2).content(), "draft user prompt");

        assertThrowsDraft(() -> AiMemoryDraft.parseMemory("{\"memory\":\"\"}"), "draft rejects blank memory");
        assertThrowsDraft(() -> AiMemoryDraft.parseMemory("{\"text\":\"foo\"}"), "draft rejects missing memory");
        assertThrowsDraft(() -> AiMemoryDraft.parseMemory("not json"), "draft rejects invalid json");
        assertThrowsDraft(() -> AiMemoryDraft.parseMemory("{\"memory\":\"" + "x".repeat(501) + "\"}"), "draft rejects too long memory");
    }

    private static void testMemoryForgetDraft() {
        List<AiMemoryStore.MemoryDeletionCandidate> candidates = List.of(
                new AiMemoryStore.MemoryDeletionCandidate(1, "2026-05-12 14:30 | 玩家: SanJin | 我的基地在樱花林旁边"),
                new AiMemoryStore.MemoryDeletionCandidate(3, "2026-05-13 09:00 | 玩家: SanJin | 我把钻石放在矿洞箱子")
        );

        assertEquals(List.of(1, 3), AiMemoryForgetDraft.parseDeleteIds("{\"delete\":[1,3],\"reason\":\"删除基地和箱子记录\"}", candidates), "forget draft parses ids");
        assertEquals(List.of(3), AiMemoryForgetDraft.parseDeleteIds("""
                ```json
                {"delete":[3],"reason":"删除物资记录"}
                ```
                """, candidates), "forget draft parses fenced ids");
        assertEquals(List.of(), AiMemoryForgetDraft.parseDeleteIds("{\"delete\":[],\"reason\":\"没有匹配项\"}", candidates), "forget draft allows empty delete");

        AiToolContext context = AiToolContext.of(List.of(new AiToolResult("tool.location.read", "@here", "位置", "x=1,y=2,z=3", false)));
        List<AiMessage> messages = AiMemoryForgetDraft.messages(context, candidates, "删除当前基地位置");
        assertEquals(4, messages.size(), "forget draft messages include tool and candidates");
        assertTrue(messages.get(0).content().contains("delete"), "forget draft system requires delete json");
        assertTrue(messages.get(1).content().contains("Minecraft Tool Context"), "forget draft includes tool context");
        assertTrue(messages.get(2).content().contains("ID 1"), "forget draft includes candidate ids");
        assertEquals("删除当前基地位置", messages.get(3).content(), "forget draft user prompt");

        assertThrowsForgetDraft(() -> AiMemoryForgetDraft.parseDeleteIds("{\"delete\":[2],\"reason\":\"bad\"}", candidates), "forget draft rejects unknown id");
        assertThrowsForgetDraft(() -> AiMemoryForgetDraft.parseDeleteIds("{\"ids\":[1]}", candidates), "forget draft rejects missing delete");
        assertThrowsForgetDraft(() -> AiMemoryForgetDraft.parseDeleteIds("not json", candidates), "forget draft rejects invalid json");
    }

    private static void testMemoryStore() {
        Path directory = tempDirectory();
        try {
            Path file = directory.resolve("minemind-memory.md");
            AiMemoryStore.MemoryWriteResult result = AiMemoryStore.remember(
                    file,
                    "SanJin",
                    "我的基地在樱花林旁边",
                    LocalDateTime.of(2026, 5, 12, 14, 30)
            );
            AiMemoryStore.remember(
                    file,
                    "SanJin",
                    "我把钻石放在矿洞箱子",
                    LocalDateTime.of(2026, 5, 13, 9, 0)
            );

            assertEquals("2026-05-12", result.date(), "memory write date");
            assertEquals("我的基地在樱花林旁边", result.content(), "memory write content");
            String fileText = readString(file);
            assertTrue(fileText.contains("# MineMind Long-term Memory"), "memory file header");
            assertTrue(fileText.contains("## 2026-05-12"), "memory date heading");
            assertTrue(fileText.contains("- 2026-05-12 14:30 | 玩家: SanJin | 我的基地在樱花林旁边"), "memory entry format");

            String relevant = AiMemoryStore.readRelevant(file, "我的基地在哪里？", 20);
            assertTrue(relevant.contains("樱花林"), "memory keyword match");
            assertTrue(relevant.contains("钻石"), "memory recent fill");

            String limited = AiMemoryStore.readRelevant(file, "", 1);
            assertTrue(limited.contains("钻石"), "memory recent limit keeps newest");
            assertFalse(limited.contains("樱花林"), "memory recent limit clips older");

            List<AiMemoryStore.MemoryDeletionCandidate> candidates = AiMemoryStore.deletionCandidates(file, "基地", 20);
            assertEquals(2, candidates.size(), "memory deletion candidates include match and recent");
            assertEquals(1, candidates.get(0).id(), "memory deletion candidate keeps original id");
            assertTrue(candidates.get(0).text().contains("樱花林"), "memory deletion candidate match first");

            AiMemoryStore.MemoryDeleteResult deleteResult = AiMemoryStore.forgetByIds(file, List.of(1));
            assertEquals(1, deleteResult.count(), "memory forget by ids removes one");
            assertTrue(deleteResult.summary().contains("樱花林"), "memory forget by ids summary");
            assertFalse(readString(file).contains("樱花林"), "memory forget by ids removes content");

            AiMemoryStore.remember(
                    file,
                    "SanJin",
                    "我的基地在樱花林旁边",
                    LocalDateTime.of(2026, 5, 14, 9, 0)
            );
            assertEquals(1, AiMemoryStore.forget(file, "樱花林"), "memory forget removes one");
            assertEquals(0, AiMemoryStore.forget(file, "不存在"), "memory forget missing returns zero");
            assertFalse(readString(file).contains("樱花林"), "memory forget removes content");

            AiMemoryStore.remember(
                    file,
                    "SanJin",
                    "基地箱子里有煤炭",
                    LocalDateTime.of(2026, 5, 14, 10, 0)
            );
            AiMemoryStore.remember(
                    file,
                    "SanJin",
                    "基地箱子里有铁锭",
                    LocalDateTime.of(2026, 5, 14, 10, 1)
            );
            AiMemoryStore.MemoryDeleteResult limitedDelete = AiMemoryStore.forgetByKeywordLimit(file, "基地箱子", 1);
            assertEquals(1, limitedDelete.count(), "autonomous keyword delete is limited");
            assertTrue(readString(file).contains("基地箱子"), "autonomous keyword delete keeps extra matches");

            assertThrowsMemory(() -> AiMemoryStore.remember(file, "SanJin", " ", LocalDateTime.of(2026, 5, 12, 14, 30)), "blank remember rejected");
            assertThrowsMemory(() -> AiMemoryStore.forget(file, " "), "blank forget rejected");
        } finally {
            deleteRecursively(directory);
        }
    }

    private static void testAutonomousToolRegistry() {
        assertEquals(8, AiToolRegistry.specs().size(), "autonomous tool registry size");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.HOTBAR), "registry contains hotbar");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.INVENTORY), "registry contains inventory");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.LOCATION), "registry contains location");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.NEARBY), "registry contains nearby");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.TARGET), "registry contains target");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.MEMORY_READ), "registry contains memory read");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.MEMORY_WRITE), "registry contains memory write");
        assertTrue(AiToolRegistry.contains(AiToolRegistry.MEMORY_DELETE), "registry contains memory delete");
        assertFalse(AiToolRegistry.contains("tool.image.read"), "registry excludes image tool");
        assertFalse(AiToolRegistry.spec(AiToolRegistry.MEMORY_WRITE).orElseThrow().readOnly(), "memory write is mutation");
    }

    private static void testAutonomousToolPermissions() {
        AiToolPermissions disabled = new AiToolPermissions(false, true, true, true, true);
        assertEquals(List.of(), AiToolRegistry.specs(disabled), "disabled autonomous tools exposes none");

        AiToolPermissions safeDefault = new AiToolPermissions(true, false, false, false, false);
        List<String> defaultToolNames = AiToolRegistry.specs(safeDefault).stream().map(AiToolSpec::name).toList();
        assertContains(defaultToolNames, AiToolRegistry.HOTBAR, "default tools include hotbar");
        assertContains(defaultToolNames, AiToolRegistry.INVENTORY, "default tools include inventory");
        assertContains(defaultToolNames, AiToolRegistry.NEARBY, "default tools include nearby");
        assertContains(defaultToolNames, AiToolRegistry.TARGET, "default tools include target");
        assertNotContains(defaultToolNames, AiToolRegistry.LOCATION, "default tools exclude location");
        assertNotContains(defaultToolNames, AiToolRegistry.MEMORY_READ, "default tools exclude memory read");
        assertNotContains(defaultToolNames, AiToolRegistry.MEMORY_WRITE, "default tools exclude memory write");
        assertNotContains(defaultToolNames, AiToolRegistry.MEMORY_DELETE, "default tools exclude memory delete");

        AiToolPermissions all = new AiToolPermissions(true, true, true, true, true);
        assertEquals(8, AiToolRegistry.specs(all).size(), "all permissions expose all tools");
        assertTrue(AiToolJsonFallback.prompt(AiToolRegistry.specs(safeDefault)).contains(AiToolRegistry.HOTBAR), "fallback prompt includes enabled tool");
        assertFalse(AiToolJsonFallback.prompt(AiToolRegistry.specs(safeDefault)).contains(AiToolRegistry.MEMORY_READ), "fallback prompt excludes disabled memory tool");
    }

    private static void testAutonomousMemoryPolicy() {
        try {
            assertEquals("我的基地在樱花林旁边", AiAutonomousMemoryPolicy.requireWrite("帮我记住这个", "我的基地在樱花林旁边"), "memory write accepts explicit intent");
            assertEquals("樱花林", AiAutonomousMemoryPolicy.requireDelete("忘掉关于樱花林的记忆", "樱花林"), "memory delete accepts explicit intent");
        } catch (AiException exception) {
            throw new AssertionError("memory policy should accept explicit intent", exception);
        }

        assertThrowsAi(() -> AiAutonomousMemoryPolicy.requireWrite("我的基地在哪里？", "我的基地在樱花林旁边"), "memory write rejects missing intent");
        assertThrowsAi(() -> AiAutonomousMemoryPolicy.requireWrite("记住这个", "x".repeat(501)), "memory write rejects long content");
        assertThrowsAi(() -> AiAutonomousMemoryPolicy.requireDelete("删除记忆", "家"), "memory delete rejects short keyword");
        assertThrowsAi(() -> AiAutonomousMemoryPolicy.requireDelete("删除记忆", "全部"), "memory delete rejects broad keyword");
        assertThrowsAi(() -> AiAutonomousMemoryPolicy.requireDelete("我的基地在哪里？", "樱花林"), "memory delete rejects missing intent");
    }

    private static void testOpenAiToolPayload() {
        AiProviderSettings settings = settings("openai", "gpt-4.1");
        List<AiMessage> messages = List.of(
                new AiMessage("user", "我在哪里？"),
                AiMessage.assistantToolCalls("", List.of(new AiToolCall("call_1", AiToolRegistry.LOCATION, "{}"))),
                AiMessage.toolResult("call_1", "位置: x=1 y=64 z=2")
        );
        String body = OpenAiChatClient.requestBodyJson(settings, messages, AiToolRegistry.specs());
        assertTrue(body.contains("\"model\":\"gpt-4.1\""), "tool payload model");
        assertTrue(body.contains("\"tool_choice\":\"auto\""), "tool payload choice");
        assertTrue(body.contains("\"tools\""), "tool payload definitions");
        assertTrue(body.contains("\"tool.hotbar.read\""), "tool payload contains hotbar tool");
        assertTrue(body.contains("\"role\":\"assistant\""), "assistant tool role");
        assertTrue(body.contains("\"tool_calls\""), "assistant tool calls serialized");
        assertTrue(body.contains("\"role\":\"tool\""), "tool result role");
        assertTrue(body.contains("\"tool_call_id\":\"call_1\""), "tool result id");
    }

    private static void testOpenAiToolCallParsing() {
        try {
            AiCompletion completion = OpenAiChatClient.parseCompletion("""
                    {
                      "choices": [
                        {
                          "message": {
                            "role": "assistant",
                            "content": null,
                            "tool_calls": [
                              {
                                "id": "call_1",
                                "type": "function",
                                "function": {
                                  "name": "tool.location.read",
                                  "arguments": "{}"
                                }
                              }
                            ]
                          }
                        }
                      ]
                    }
                    """);
            assertTrue(completion.hasToolCalls(), "openai parser detects tool call");
            assertEquals(AiToolRegistry.LOCATION, completion.toolCalls().get(0).name(), "openai parser tool name");
            assertEquals("{}", completion.toolCalls().get(0).argumentsJson(), "openai parser arguments");
        } catch (AiException exception) {
            throw new AssertionError("openai tool parsing should succeed", exception);
        }
    }

    private static void testJsonFallbackParsing() {
        assertEquals(List.of(), AiToolJsonFallback.parseToolCalls("直接回答"), "fallback ordinary text");
        List<AiToolCall> calls = AiToolJsonFallback.parseToolCalls("""
                {"tool_calls":[{"id":"call_1","name":"tool.memory.read","arguments":{"query":"基地"}}]}
                """);
        assertEquals(1, calls.size(), "fallback parses one call");
        assertEquals(AiToolRegistry.MEMORY_READ, calls.get(0).name(), "fallback parses name");
        assertTrue(calls.get(0).argumentsJson().contains("基地"), "fallback parses arguments");

        List<AiToolCall> fenced = AiToolJsonFallback.parseToolCalls("""
                ```json
                {"tool_calls":[{"name":"tool.location.read","arguments":{}}]}
                ```
                """);
        assertEquals(AiToolRegistry.LOCATION, fenced.get(0).name(), "fallback parses fenced json");

        assertThrowsFallback(() -> AiToolJsonFallback.parseToolCalls("{bad}"), "fallback rejects invalid json");
        assertThrowsFallback(
                () -> AiToolJsonFallback.parseToolCalls("{\"tool_calls\":[{\"name\":\"tool.image.read\",\"arguments\":{}}]}"),
                "fallback rejects unknown tool"
        );
        assertThrowsAi(
                () -> AiAutonomousToolExecutor.execute(
                        new AiToolCall("call_1", AiToolRegistry.MEMORY_WRITE, "{}"),
                        "SanJin",
                        LocalDateTime.of(2026, 5, 14, 9, 0),
                        "帮我记住这个"
                ),
                "memory write rejects empty arguments"
        );
    }

    private static void testToolRunLimits() {
        AiToolRunState state = new AiToolRunState();
        try {
            state.requireRoundCallCount(AiToolRunState.MAX_TOOL_CALLS_PER_ROUND);
        } catch (AiException exception) {
            throw new AssertionError("tool run should allow max calls", exception);
        }
        assertThrowsAi(
                () -> state.requireRoundCallCount(AiToolRunState.MAX_TOOL_CALLS_PER_ROUND + 1),
                "tool run rejects too many calls"
        );
        try {
            state.recordMemoryMutation();
        } catch (AiException exception) {
            throw new AssertionError("tool run should allow first memory mutation", exception);
        }
        assertThrowsAi(state::recordMemoryMutation, "tool run rejects repeated memory mutation");
    }

    private static void testPromptInjection() {
        List<AiMessage> messages = AiPrompt.withSystemPrompt(List.of(new AiMessage("user", "hello")));
        assertEquals(2, messages.size(), "system prompt prepended");
        assertEquals("system", messages.get(0).role(), "system prompt role");
        assertTrue(messages.get(0).content().contains("Minecraft 游戏聊天栏"), "system prompt describes minecraft chat");
        assertTrue(messages.get(0).content().contains("@memory"), "system prompt lists memory marker");
        assertTrue(messages.get(0).content().contains("@image"), "system prompt lists image marker");
        assertEquals("user", messages.get(1).role(), "user message after system prompt");
        assertEquals("hello", messages.get(1).content(), "user message preserved");

        AiToolContext context = AiToolContext.of(List.of(new AiToolResult("tool.location.read", "@here", "位置", "x=1,y=2,z=3", false)));
        List<AiMessage> withTools = AiPrompt.withSystemPrompt(context, List.of(new AiMessage("user", "where")));
        assertEquals(3, withTools.size(), "tool context inserted after system prompt");
        assertEquals("system", withTools.get(1).role(), "tool context role");
        assertTrue(withTools.get(0).content().contains("@here"), "system prompt lists tool marker");
        assertTrue(withTools.get(1).content().contains("Minecraft Tool Context"), "tool context message");
        assertEquals("user", withTools.get(2).role(), "user follows tool context");
    }

    private static void testConversationHistory() {
        AiConversationHistory history = new AiConversationHistory();
        history.addExchange("u1", "a1", 4);
        history.addExchange("u2", "a2", 4);
        history.addExchange("u3", "a3", 4);

        assertEquals(4, history.size(), "history trimmed size");
        List<AiMessage> messages = history.snapshot();
        assertEquals("u2", messages.get(0).content(), "history trims oldest user");
        assertEquals("a3", messages.get(3).content(), "history keeps newest assistant");

        List<AiMessage> request = history.snapshotWithUser("u4");
        assertEquals(5, request.size(), "snapshot with user does not mutate trim");
        assertEquals(4, history.size(), "snapshot with user keeps stored size");

        assertEquals(4, history.clear(), "clear count");
        assertEquals(0, history.size(), "history cleared");
    }

    private static void testScreenshotSizing() {
        assertEquals(new AiScreenshot.ImageSize(1280, 720), AiScreenshot.targetSize(1920, 1080, 1280), "screenshot landscape resize");
        assertEquals(new AiScreenshot.ImageSize(720, 1280), AiScreenshot.targetSize(1080, 1920, 1280), "screenshot portrait resize");
        assertEquals(new AiScreenshot.ImageSize(800, 600), AiScreenshot.targetSize(800, 600, 1280), "screenshot keeps small image");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + ": expected true");
        }
    }

    private static void assertFalse(boolean value, String label) {
        if (value) {
            throw new AssertionError(label + ": expected false");
        }
    }

    private static void assertContains(List<String> values, String expected, String label) {
        if (!values.contains(expected)) {
            throw new AssertionError(label + ": expected list to contain <" + expected + "> but was <" + values + ">");
        }
    }

    private static void assertNotContains(List<String> values, String expected, String label) {
        if (values.contains(expected)) {
            throw new AssertionError(label + ": expected list to omit <" + expected + "> but was <" + values + ">");
        }
    }

    private static void assertThrowsConfig(Runnable runnable, String label) {
        try {
            runnable.run();
        } catch (AiConfigStore.ConfigException exception) {
            return;
        }
        throw new AssertionError(label + ": expected config exception");
    }

    private static void assertThrowsMemory(Runnable runnable, String label) {
        try {
            runnable.run();
        } catch (AiMemoryStore.MemoryException exception) {
            return;
        }
        throw new AssertionError(label + ": expected memory exception");
    }

    private static void assertThrowsDraft(Runnable runnable, String label) {
        try {
            runnable.run();
        } catch (AiMemoryDraft.DraftException exception) {
            return;
        }
        throw new AssertionError(label + ": expected draft exception");
    }

    private static void assertThrowsForgetDraft(Runnable runnable, String label) {
        try {
            runnable.run();
        } catch (AiMemoryForgetDraft.DraftException exception) {
            return;
        }
        throw new AssertionError(label + ": expected forget draft exception");
    }

    private static void assertThrowsFallback(Runnable runnable, String label) {
        try {
            runnable.run();
        } catch (AiToolJsonFallback.FallbackException exception) {
            return;
        }
        throw new AssertionError(label + ": expected fallback exception");
    }

    private static void assertThrowsAi(ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (AiException exception) {
            return;
        } catch (Exception exception) {
            throw new AssertionError(label + ": expected ai exception", exception);
        }
        throw new AssertionError(label + ": expected ai exception");
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static Path tempDirectory() {
        try {
            return Files.createTempDirectory("minemind-memory-test");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static void deleteRecursively(Path path) {
        if (path == null || Files.notExists(path)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder()).forEach(value -> {
                try {
                    Files.deleteIfExists(value);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static AiProviderSettings settings(String providerId, String model) {
        return new AiProviderSettings(providerId, providerId, model, "https://example.com/v1", "key", 45);
    }
}
