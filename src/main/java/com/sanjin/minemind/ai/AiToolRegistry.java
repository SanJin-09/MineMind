package com.sanjin.minemind.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AiToolRegistry {
    public static final String HOTBAR = "tool.hotbar.read";
    public static final String INVENTORY = "tool.inventory.read";
    public static final String LOCATION = "tool.location.read";
    public static final String NEARBY = "tool.entities.nearby";
    public static final String TARGET = "tool.target.read";
    public static final String MEMORY_READ = "tool.memory.read";
    public static final String MEMORY_WRITE = "tool.memory.write";
    public static final String MEMORY_DELETE = "tool.memory.delete";

    private static final Map<String, AiToolSpec> TOOLS = new LinkedHashMap<>();

    static {
        register(readOnly(HOTBAR, "快捷栏", "读取玩家 9 格快捷栏中的物品、数量、耐久、组件和关键标签。"));
        register(readOnly(INVENTORY, "背包", "读取玩家背包、护甲、副手和快捷栏物品摘要。"));
        register(readOnly(LOCATION, "位置", "读取玩家当前维度、坐标、朝向、生态群系和脚下方块。"));
        register(readOnly(NEARBY, "附近生物", "读取玩家附近客户端可见实体的类型、数量、距离和敌对信息。"));
        register(readOnly(TARGET, "准星目标", "读取玩家准星当前指向的方块或实体信息。"));
        register(memoryRead());
        register(memoryWrite());
        register(memoryDelete());
    }

    private AiToolRegistry() {
    }

    public static List<AiToolSpec> specs() {
        return List.copyOf(TOOLS.values());
    }

    public static List<AiToolSpec> specs(AiToolPermissions permissions) {
        if (permissions == null || !permissions.autonomousTools()) {
            return List.of();
        }
        return TOOLS.values().stream()
                .filter(permissions::isAllowed)
                .toList();
    }

    public static Optional<AiToolSpec> spec(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(TOOLS.get(name.trim()));
    }

    public static boolean contains(String name) {
        return spec(name).isPresent();
    }

    private static void register(AiToolSpec spec) {
        TOOLS.put(spec.name(), spec);
    }

    private static AiToolSpec readOnly(String name, String label, String description) {
        return new AiToolSpec(name, label, description, emptyObjectSchema(), true);
    }

    private static AiToolSpec memoryRead() {
        JsonObject properties = new JsonObject();
        properties.add("query", stringSchema("用于匹配长期记忆的玩家问题或关键词，可以为空。"));
        return new AiToolSpec(
                MEMORY_READ,
                "长期记忆",
                "读取与当前问题相关的本地长期记忆条目。",
                objectSchema(properties),
                true
        );
    }

    private static AiToolSpec memoryWrite() {
        JsonObject properties = new JsonObject();
        properties.add("content", stringSchema("需要写入长期记忆的简短中文内容。"));
        JsonObject schema = objectSchema(properties);
        schema.add("required", required("content"));
        return new AiToolSpec(
                MEMORY_WRITE,
                "长期记忆",
                "写入一条本地长期记忆。只在玩家明确表达需要记住信息时调用。",
                schema,
                false
        );
    }

    private static AiToolSpec memoryDelete() {
        JsonObject properties = new JsonObject();
        properties.add("keyword", stringSchema("用于删除长期记忆的关键词。"));
        JsonObject schema = objectSchema(properties);
        schema.add("required", required("keyword"));
        return new AiToolSpec(
                MEMORY_DELETE,
                "长期记忆",
                "按关键词删除本地长期记忆条目。只在玩家明确表达需要遗忘或删除记忆时调用。",
                schema,
                false
        );
    }

    private static JsonObject emptyObjectSchema() {
        return objectSchema(new JsonObject());
    }

    private static JsonObject objectSchema(JsonObject properties) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", new JsonArray());
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    private static JsonObject stringSchema(String description) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.addProperty("description", description);
        return schema;
    }

    private static JsonArray required(String name) {
        JsonArray required = new JsonArray();
        required.add(name);
        return required;
    }
}
