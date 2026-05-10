package com.sanjin.minemind.ai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                                .executes(AiCommands::listModels)
                                .then(argument("model", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                AiModelCatalog.cachedModelIds(StringArgumentType.getString(context, "provider")),
                                                builder
                                        ))
                                        .executes(AiCommands::setModel))))
                .then(literal("key")
                        .then(literal("list").executes(AiCommands::listKeys))
                        .then(literal("remove")
                                .then(argument("provider", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                        .executes(AiCommands::removeKey)))
                        .then(argument("provider", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                .then(argument("key", StringArgumentType.greedyString())
                                        .executes(AiCommands::setKey))))
                .then(literal("base")
                        .executes(AiCommands::base)
                        .then(argument("provider", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(AiConfigStore.providerIds(), builder))
                                .then(argument("url", StringArgumentType.greedyString())
                                        .executes(AiCommands::setBaseUrl))))
                .then(literal("timeout")
                        .executes(AiCommands::timeout)
                        .then(argument("seconds", IntegerArgumentType.integer())
                                .executes(AiCommands::setTimeout)))
                .then(literal("max-history")
                        .executes(AiCommands::maxHistory)
                        .then(argument("count", IntegerArgumentType.integer())
                                .executes(AiCommands::setMaxHistory)))
                .then(literal("status").executes(AiCommands::status))
                .then(literal("clear").executes(AiCommands::clear)));
    }

    private static int help(CommandContext<CommandSourceStack> context) {
        AiChat.infoHighlight("============ MineMind AI 命令帮助 =============");
        AiChat.info("/ai on - 开启 AI 对话模式");
        AiChat.info("/ai off - 关闭 AI 对话模式");
        AiChat.info("/ai ask <内容> - 单次向 AI 提问");
        AiChat.info("/ai model - 查看当前模型");
        AiChat.info("/ai model <provider> - 获取可选型号列表");
        AiChat.info("/ai model <provider> <id> - 从可选列表切换模型");
        AiChat.info("/ai key <provider> <key> - 设置 API Key");
        AiChat.info("/ai key list - 查看 API Key 状态");
        AiChat.info("/ai key remove <provider> - 删除 API Key");
        AiChat.info("/ai base - 查看当前 API Base URL");
        AiChat.info("/ai base <provider> <url> - 设置 API Base URL");
        AiChat.info("/ai timeout [秒] - 查看或设置请求超时");
        AiChat.info("/ai max-history [数量] - 查看或设置上下文条数");
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
        String model = settings.model() == null || settings.model().isBlank() ? "未设置" : settings.model();
        AiChat.info("当前模型：" + settings.displayName() + " / " + model);
        return 1;
    }

    private static int listModels(CommandContext<CommandSourceStack> context) {
        AiController.listModels(StringArgumentType.getString(context, "provider"));
        return 1;
    }

    private static int setModel(CommandContext<CommandSourceStack> context) {
        String provider = StringArgumentType.getString(context, "provider");
        String model = StringArgumentType.getString(context, "model");
        AiController.selectModel(provider, model);
        return 1;
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

    private static int listKeys(CommandContext<CommandSourceStack> context) {
        for (String line : AiConfigStore.keyStatusLines()) {
            AiChat.info(line);
        }
        return 1;
    }

    private static int base(CommandContext<CommandSourceStack> context) {
        AiProviderSettings settings = AiConfigStore.currentSettings();
        String baseUrl = settings.baseUrl() == null || settings.baseUrl().isBlank() ? "未设置" : settings.baseUrl();
        AiChat.info("当前 Base URL：" + baseUrl);
        return 1;
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

    private static int timeout(CommandContext<CommandSourceStack> context) {
        AiChat.info("当前请求超时：" + AiConfigStore.timeoutSeconds() + " 秒");
        return 1;
    }

    private static int setTimeout(CommandContext<CommandSourceStack> context) {
        int seconds = IntegerArgumentType.getInteger(context, "seconds");
        try {
            AiConfigStore.setTimeoutSeconds(seconds);
            AiChat.info("请求超时已设置为：" + seconds + " 秒");
            return 1;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return 0;
        }
    }

    private static int maxHistory(CommandContext<CommandSourceStack> context) {
        AiChat.info("当前最大上下文：" + AiConfigStore.maxHistoryMessages() + " 条");
        return 1;
    }

    private static int setMaxHistory(CommandContext<CommandSourceStack> context) {
        int count = IntegerArgumentType.getInteger(context, "count");
        try {
            AiConfigStore.setMaxHistoryMessages(count);
            AiController.trimHistoryToMax();
            AiChat.info("最大上下文已设置为：" + count + " 条");
            return 1;
        } catch (AiConfigStore.ConfigException exception) {
            AiChat.error(exception.getMessage());
            return 0;
        }
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        for (String line : AiController.statusLines()) {
            AiChat.info(line);
        }
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        AiController.clearHistory();
        return 1;
    }
}
