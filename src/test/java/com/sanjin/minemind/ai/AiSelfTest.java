package com.sanjin.minemind.ai;

import java.util.List;

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
        testConversationHistory();
        System.out.println("AiSelfTest passed");
    }

    private static void testProviderRules() {
        assertEquals("openai", AiConfigRules.normalizeProviderId("OpenAI"), "openai normalize");
        assertEquals("openai", AiConfigRules.normalizeProviderId("ChatGPT"), "chatgpt alias");
        assertEquals("deepseek", AiConfigRules.normalizeProviderId("DeepSeek"), "deepseek normalize");

        AiProvider openai = AiProviderRegistry.provider("openai");
        assertEquals("openai", openai.id(), "openai provider id");
        assertEquals("OpenAI", openai.displayName(), "openai display name");
        assertEquals("https://api.openai.com/v1", openai.defaultBaseUrl(), "openai base url");
        assertEquals("gpt-4.1", openai.recommendedModelId(), "openai recommended model");

        AiProvider chatgpt = AiProviderRegistry.provider("chatgpt");
        assertEquals("openai", chatgpt.id(), "chatgpt provider alias");

        AiProvider deepseek = AiProviderRegistry.provider("deepseek");
        assertEquals("deepseek", deepseek.id(), "deepseek provider id");
        assertEquals("DeepSeek", deepseek.displayName(), "deepseek display name");
        assertEquals("https://api.deepseek.com", deepseek.defaultBaseUrl(), "deepseek base url");
        assertEquals("deepseek-chat", deepseek.recommendedModelId(), "deepseek recommended model");

        List<String> suggestions = AiProviderRegistry.providerSuggestions(List.of("custom"));
        assertContains(suggestions, "openai", "provider suggestions openai");
        assertContains(suggestions, "chatgpt", "provider suggestions chatgpt");
        assertContains(suggestions, "deepseek", "provider suggestions deepseek");
        assertContains(suggestions, "custom", "provider suggestions custom");
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

        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(404, quotaBody), "quota body wins over 404");
        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(400, balanceBody), "balance body wins over 400");
        assertEquals(AiErrorType.MODEL, AiHttpStatusClassifier.classify(400, modelBody), "model body wins over 400");
    }

    private static void testModelCatalogFiltering() {
        assertTrue(AiModelCatalog.isTextChatModelId("gpt-4.1"), "gpt text model");
        assertTrue(AiModelCatalog.isTextChatModelId("ft:gpt-4.1:org:name:id"), "fine tuned gpt text model");
        assertTrue(AiModelCatalog.isTextChatModelId("deepseek-chat"), "deepseek chat model");
        assertTrue(AiModelCatalog.isTextChatModelId("qwen-max"), "qwen text model");
        assertTrue(AiModelCatalog.isTextChatModelId("claude-3-5-sonnet"), "claude text model");
        assertEquals(List.of("deepseek-chat", "deepseek-reasoner"), AiModelCatalog.suggestedModelIds("deepseek"), "deepseek model suggestions");

        assertFalse(AiModelCatalog.isTextChatModelId("text-embedding-3-small"), "embedding rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("gpt-image-1"), "image rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("gpt-4o-audio-preview"), "audio rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("whisper-1"), "whisper rejected");
        assertFalse(AiModelCatalog.isTextChatModelId("omni-moderation-latest"), "moderation rejected");
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
}
