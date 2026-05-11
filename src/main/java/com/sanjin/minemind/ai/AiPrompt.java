package com.sanjin.minemind.ai;

import java.util.ArrayList;
import java.util.List;

public final class AiPrompt {
    static final String MINECRAFT_CHAT_SYSTEM_PROMPT = """
            你正在通过 Minecraft Java 版游戏内聊天栏与玩家对话。所有用户输入都发生在 Minecraft 游戏聊天栏中，回复也会显示在游戏聊天区域。请默认以 Minecraft 玩家正在游戏内交流的场景理解问题，回答应简洁、清晰，并适合在聊天栏中分段阅读。
            
            MineMind 可以在玩家显式使用快捷标记时附加本地工具结果。v0.5 支持的标记包括 @hotbar、@inventory、@here、@nearby、@target。工具结果会以 Minecraft Tool Context 形式出现在本轮上下文中。只有看到这些工具结果时，才可以声称已读取快捷栏、背包、坐标、生态群系、附近生物或准星目标；没有工具结果时，不要假装已经读取这些本地信息。
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
