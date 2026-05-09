package com.sanjin.minemind.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiConversationHistory {
    private final List<AiMessage> messages = new ArrayList<>();

    public synchronized List<AiMessage> snapshot() {
        return new ArrayList<>(messages);
    }

    public synchronized List<AiMessage> snapshotWithUser(String prompt) {
        List<AiMessage> result = new ArrayList<>(messages);
        result.add(new AiMessage("user", prompt));
        return result;
    }

    public synchronized void addExchange(String prompt, String answer, int maxMessages) {
        messages.add(new AiMessage("user", prompt));
        messages.add(new AiMessage("assistant", answer));
        trim(maxMessages);
    }

    public synchronized int clear() {
        int count = messages.size();
        messages.clear();
        return count;
    }

    public synchronized void trimTo(int maxMessages) {
        trim(maxMessages);
    }

    public synchronized int size() {
        return messages.size();
    }

    private void trim(int maxMessages) {
        int max = Math.max(AiConfigRules.MIN_MAX_HISTORY_MESSAGES, maxMessages);
        while (messages.size() > max) {
            messages.remove(0);
        }
    }
}
