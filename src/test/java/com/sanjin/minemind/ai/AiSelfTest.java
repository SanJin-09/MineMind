package com.sanjin.minemind.ai;

import java.util.List;

public final class AiSelfTest {
    private AiSelfTest() {
    }

    public static void main(String[] args) {
        testTimeoutRules();
        testHistoryRules();
        testBaseUrlRules();
        testHttpStatusClassification();
        testConversationHistory();
        System.out.println("AiSelfTest passed");
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
        assertEquals(AiConfigRules.OPENAI_DEFAULT_BASE_URL, AiConfigRules.repairBaseUrl("openai", "bad-url"), "openai base repair");
        assertEquals("", AiConfigRules.repairBaseUrl("custom", "bad-url"), "custom base repair");
    }

    private static void testHttpStatusClassification() {
        assertEquals(AiErrorType.AUTH, AiHttpStatusClassifier.classify(401), "401 auth");
        assertEquals(AiErrorType.AUTH, AiHttpStatusClassifier.classify(403), "403 auth");
        assertEquals(AiErrorType.MODEL, AiHttpStatusClassifier.classify(404), "404 model");
        assertEquals(AiErrorType.TIMEOUT, AiHttpStatusClassifier.classify(408), "408 timeout");
        assertEquals(AiErrorType.TIMEOUT, AiHttpStatusClassifier.classify(504), "504 timeout");
        assertEquals(AiErrorType.QUOTA, AiHttpStatusClassifier.classify(429), "429 quota");
        assertEquals(AiErrorType.SERVICE, AiHttpStatusClassifier.classify(500), "500 service");
        assertEquals(AiErrorType.REQUEST, AiHttpStatusClassifier.classify(400), "400 request");
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
}
