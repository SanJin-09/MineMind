package com.sanjin.minemind.ai;

public final class AiToolRunState {
    public static final int MAX_TOOL_ROUNDS = 3;
    public static final int MAX_TOOL_CALLS_PER_ROUND = 4;
    public static final int MAX_MEMORY_MUTATIONS = 1;

    private int memoryMutations;

    public int memoryMutations() {
        return memoryMutations;
    }

    public void requireRoundCallCount(int count) throws AiException {
        if (count <= 0) {
            return;
        }
        if (count > MAX_TOOL_CALLS_PER_ROUND) {
            throw new AiException(AiErrorType.REQUEST, "模型一次请求的工具数量过多，请换一种问法");
        }
    }

    public void recordMemoryMutation() throws AiException {
        memoryMutations++;
        if (memoryMutations > MAX_MEMORY_MUTATIONS) {
            throw new AiException(AiErrorType.REQUEST, "本轮最多允许一次长期记忆修改");
        }
    }
}
