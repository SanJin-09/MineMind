package com.sanjin.minemind.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiPrompt {
    static final String MINECRAFT_CHAT_SYSTEM_PROMPT = """
            你正在通过 Minecraft Java 版游戏内聊天栏与玩家对话。所有用户输入都发生在 Minecraft 游戏聊天栏中，回复也会显示在游戏聊天区域。请默认以 Minecraft 玩家正在游戏内交流的场景理解问题，回答应简洁、清晰，并适合在聊天栏中分段阅读。
            
            MineMind 可以在玩家显式使用快捷标记时附加本地工具结果。支持的文本工具标记包括 @hotbar、@inventory、@here、@nearby、@target 和 @memory；@image 会附加当前游戏画面截图。工具结果会以 Minecraft Tool Context 形式出现在本轮上下文中，图片会作为本轮用户消息的图片输入出现。只有看到这些工具结果或图片输入时，才可以声称已读取快捷栏、背包、坐标、生态群系、附近生物、准星目标、长期记忆或当前画面；没有对应输入时，不要假装已经读取这些本地信息。

            @remember 会由 MineMind 单独触发长期记忆整理写入流程，@forget 会由 MineMind 单独触发模型辅助的长期记忆删除流程；普通对话中你不能声称自己已经写入、删除或读取长期记忆，除非本轮上下文中明确附加了对应的长期记忆结果。
            """.trim();

    private AiPrompt() {
    }

    public static List<AiMessage> withSystemPrompt(List<AiMessage> messages) {
        return withSystemPrompt(AiToolContext.empty(), messages);
    }

    public static List<AiMessage> withSystemPrompt(AiToolContext toolContext, List<AiMessage> messages) {
        List<AiMessage> result = new ArrayList<>();
        result.add(new AiMessage("system", MINECRAFT_CHAT_SYSTEM_PROMPT));
        if (toolContext != null && toolContext.hasResults()) {
            result.add(new AiMessage("system", toolContext.toPromptText()));
        }
        if (messages != null) {
            result.addAll(messages);
        }
        return List.copyOf(result);
    }
}
