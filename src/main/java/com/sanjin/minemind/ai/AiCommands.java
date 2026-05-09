package com.sanjin.minemind.ai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class AiCommands {
    private AiCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(literal("ai")
                .executes(AiCommands::help)
                .then(literal("help").executes(AiCommands::help))
                .then(literal("on").executes(AiCommands::on))
                .then(literal("off").executes(AiCommands::off))
                .then(literal("ask")
                        .then(argument("prompt", StringArgumentType.greedyString())
                                .executes(AiCommands::ask)))
                .then(literal("model")
                        .executes(AiCommands::model)
                        .then(argument("provider", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                .then(argument("model", StringArgumentType.greedyString())
                                        .executes(AiCommands::setModel))))
                .then(literal("key")
                        .then(literal("remove")
                                .then(argument("provider", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                        .executes(AiCommands::removeKey)))
                        .then(argument("provider", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                .then(argument("key", StringArgumentType.greedyString())
                                        .executes(AiCommands::setKey))))
                .then(literal("base")
                        .then(argument("provider", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                .then(argument("url", StringArgumentType.greedyString())
                                        .executes(AiCommands::setBaseUrl))))
                .then(literal("status").executes(AiCommands::status))
                .then(literal("clear").executes(AiCommands::clear)));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        AiChat.info("/ai on - 开启 AI 对话模式");
        AiChat.info("/ai off - 关闭 AI 对话模式");
        AiChat.info("/ai ask <内容> - 单次向 AI 提问");
        AiChat.info("/ai model - 查看当前模型");
        AiChat.info("/ai model <provider> <id> - 切换模型");
        AiChat.info("/ai key <provider> <key> - 设置 API Key");
        AiChat.info("/ai key remove <provider> - 删除 API Key");
        AiChat.info("/ai base <provider> <url> - 设置 API Base URL");
        AiChat.info("/ai status - 查看连接状态");
        AiChat.info("/ai clear - 清空当前对话上下文");
        return 1;
    }

    private static int on(CommandContext<CommandSourceStack> context) {
        return AiController.enableAiMode() ? 1 : 0;
    }

    private static int off(CommandContext<CommandSourceStack> context) {
        return AiController.disableAiMode() ? 1 : 0;
    }

    private static int ask(CommandContext<CommandSourceStack> context) {
        AiController.ask(StringArgumentType.getString(context, "prompt"));
        return 1;
    }

    private static int model(CommandContext<CommandSourceStack> context) {
        AiProviderSettings settings = AiConfigStore.currentSettings();
        AiChat.info("当前模型：" + settings.displayName() + " / " + settings.model());
        return 1;
    }

    private static int setModel(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        String model = StringArgumentType.getString(context, "model");
        try {
            AiConfigStore.setProviderModel(provider, model);
            AiProviderSettings settings = AiConfigStore.currentSettings();
            AiChat.info("当前模型已切换为：" + settings.displayName() + " / " + settings.model());
            return 1;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return 0;
        }
    }

    private static int setKey(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        String key = StringArgumentType.getString(context, "key");
        try {
            AiConfigStore.setApiKey(provider, key);
            AiChat.info(provider + ": 已配置 Key，尾号 " + AiConfigStore.mask(key));
            return 1;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return 0;
        }
    }

    private static int removeKey(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        try {
            AiConfigStore.removeApiKey(provider);
            AiChat.info(provider + ": 已删除 Key");
            return 1;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return 0;
        }
    }

    private static int setBaseUrl(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        String url = StringArgumentType.getString(context, "url");
        try {
            AiConfigStore.setBaseUrl(provider, url);
            AiChat.info(provider + ": API Base URL 已更新");
            return 1;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return 0;
        }
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        for (String line : AiConfigStore.statusLines()) {
            AiChat.info(line);
        }
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        AiController.clearHistory();
        return 1;
    }
}
