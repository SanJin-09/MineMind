package com.sanjin.minemind.ai;

import net.minecraft.client.Minecraft;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiToolOrchestrator {
    private AiToolOrchestrator() {
    }

    public static String complete(
            AiProvider provider,
            AiProviderSettings settings,
            List<AiMessage> messages,
            String playerName
    ) throws AiException {
        List<AiMessage> working = new ArrayList<>(messages);
        List<AiToolSpec> tools = AiToolRegistry.specs();
        AiToolRunState state = new AiToolRunState();
        Map<String, AiToolExecution> readCache = new LinkedHashMap<>();
        boolean nativeTools = provider.supportsNativeToolCalls();
        boolean fallbackPromptAdded = false;

        for (int round = 0; round < AiToolRunState.MAX_TOOL_ROUNDS; round++) {
            AiCompletion completion;
            if (nativeTools) {
                try {
                    completion = provider.completeWithTools(settings, working, tools);
                } catch (AiException exception) {
                    if (exception.type() != AiErrorType.REQUEST) {
                        throw exception;
                    }
                    nativeTools = false;
                    if (!fallbackPromptAdded) {
                        working.add(new AiMessage("system", AiToolJsonFallback.PROMPT));
                        fallbackPromptAdded = true;
                    }
                    completion = fallbackCompletion(provider, settings, working);
                }
            } else {
                if (!fallbackPromptAdded) {
                    working.add(new AiMessage("system", AiToolJsonFallback.PROMPT));
                    fallbackPromptAdded = true;
                }
                completion = fallbackCompletion(provider, settings, working);
            }

            if (!completion.hasToolCalls()) {
                if (completion.content().isBlank()) {
                    throw new AiException(AiErrorType.EMPTY_RESPONSE);
                }
                return completion.content();
            }

            state.requireRoundCallCount(completion.toolCalls().size());
            List<AiToolExecution> executions = executeToolCalls(completion.toolCalls(), playerName, state, readCache);
            notifyToolCalls(executions);

            if (nativeTools) {
                working.add(AiMessage.assistantToolCalls(completion.content(), completion.toolCalls()));
                for (int index = 0; index < completion.toolCalls().size(); index++) {
                    AiToolCall call = completion.toolCalls().get(index);
                    AiToolExecution execution = executions.get(index);
                    working.add(AiMessage.toolResult(call.id(), execution.result().toPromptText()));
                }
            } else {
                List<AiToolResult> results = executions.stream().map(AiToolExecution::result).toList();
                working.add(new AiMessage("system", AiToolContext.of(results).toPromptText()));
            }
        }

        throw new AiException(AiErrorType.REQUEST, "模型工具调用次数过多，请换一种问法");
    }

    private static AiCompletion fallbackCompletion(
            AiProvider provider,
            AiProviderSettings settings,
            List<AiMessage> messages
    ) throws AiException {
        String content = provider.complete(settings, messages);
        List<AiToolCall> calls;
        try {
            calls = AiToolJsonFallback.parseToolCalls(content);
        } catch (AiToolJsonFallback.FallbackException exception) {
            throw new AiException(AiErrorType.REQUEST, exception.getMessage());
        }
        return new AiCompletion(calls.isEmpty() ? content : "", calls);
    }

    private static List<AiToolExecution> executeToolCalls(
            List<AiToolCall> calls,
            String playerName,
            AiToolRunState state,
            Map<String, AiToolExecution> readCache
    ) throws AiException {
        preflightToolCalls(calls, state);
        List<AiToolExecution> executions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (AiToolCall call : calls) {
            AiToolSpec spec = AiToolRegistry.spec(call.name())
                    .orElseThrow(() -> new AiException(AiErrorType.REQUEST, "模型请求了未知工具：" + call.name()));
            if (!spec.readOnly()) {
                state.recordMemoryMutation();
            }
            String cacheKey = state.memoryMutations() + "|" + call.name() + "|" + call.argumentsJson();
            if (spec.readOnly() && readCache.containsKey(cacheKey)) {
                executions.add(readCache.get(cacheKey));
                continue;
            }

            AiToolExecution execution = AiAutonomousToolExecutor.execute(call, playerName, now);
            if (spec.readOnly()) {
                readCache.put(cacheKey, execution);
            }
            executions.add(execution);
        }
        return List.copyOf(executions);
    }

    private static void preflightToolCalls(List<AiToolCall> calls, AiToolRunState state) throws AiException {
        int mutations = 0;
        for (AiToolCall call : calls) {
            AiToolSpec spec = AiToolRegistry.spec(call.name())
                    .orElseThrow(() -> new AiException(AiErrorType.REQUEST, "模型请求了未知工具：" + call.name()));
            if (!spec.readOnly()) {
                mutations++;
            }
        }
        if (state.memoryMutations() + mutations > AiToolRunState.MAX_MEMORY_MUTATIONS) {
            throw new AiException(AiErrorType.REQUEST, "本轮最多允许一次长期记忆修改");
        }
    }

    private static void notifyToolCalls(List<AiToolExecution> executions) {
        List<String> labels = new ArrayList<>();
        List<String> notices = new ArrayList<>();
        for (AiToolExecution execution : executions) {
            String label = execution.result().label();
            if (!label.isBlank() && !labels.contains(label)) {
                labels.add(label);
            }
            if (!execution.notice().isBlank()) {
                notices.add(execution.notice());
            }
        }
        Minecraft.getInstance().execute(() -> {
            if (!labels.isEmpty()) {
                AiChat.info("模型调用工具：" + String.join("、", labels));
            }
            for (String notice : notices) {
                AiChat.info(notice);
            }
        });
    }
}
