package com.sanjin.minemind.ai;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class AiMinecraftTools {
    private static final int NEARBY_RADIUS = 16;
    private static final int TARGET_MAX_DISTANCE = 20;
    private static final int MAX_INVENTORY_LINES = 48;
    private static final int MAX_ENTITY_TYPES = 20;
    private static final int MAX_TAGS = 8;

    private AiMinecraftTools() {
    }

    public static AiToolContext collect(AiToolRequest request) {
        if (request == null || !request.hasTools()) {
            return AiToolContext.empty();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            throw new AiToolException();
        }

        List<AiToolResult> results = new ArrayList<>();
        for (AiToolRequest.Tool tool : request.tools()) {
            results.add(switch (tool) {
                case HOTBAR -> result(tool, readHotbar(minecraft.player));
                case INVENTORY -> result(tool, readInventory(minecraft.player));
                case HERE -> result(tool, readLocation(minecraft.player, minecraft.level));
                case NEARBY -> result(tool, readNearbyEntities(minecraft.player, minecraft.level));
                case TARGET -> result(tool, readTarget(minecraft));
            });
        }
        return AiToolContext.of(results);
    }

    private static AiToolResult result(AiToolRequest.Tool tool, String content) {
        return new AiToolResult(tool.toolId(), tool.marker(), tool.label(), content, false);
    }

    private static String readHotbar(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        StringBuilder builder = new StringBuilder("快捷栏 9 格：\n");
        boolean hasItem = false;
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                hasItem = true;
                builder.append("- 槽位 ").append(slot + 1).append(": ").append(itemSummary(stack, true)).append('\n');
            }
        }
        if (!hasItem) {
            builder.append("- 空\n");
        }
        return builder.toString();
    }

    private static String readInventory(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        StringBuilder builder = new StringBuilder("玩家携带物品摘要：\n");
        appendStackLines(builder, "快捷栏", collectStacks(inventory, 0, Inventory.getSelectionSize(), true));
        appendStackLines(builder, "主背包", collectStacks(inventory, Inventory.getSelectionSize(), 36, false));
        appendStackLines(builder, "护甲", collectEquipment(player));
        appendStackLines(builder, "副手", collectSingle(player.getItemBySlot(EquipmentSlot.OFFHAND)));
        return builder.toString();
    }

    private static String readLocation(LocalPlayer player, ClientLevel level) {
        BlockPos pos = player.blockPosition();
        BlockPos onPos = player.getOnPos();
        BlockState foot = level.getBlockState(onPos);
        String biome = level.getBiome(pos)
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse(level.getBiome(pos).getRegisteredName());
        return """
                位置：
                - 维度: %s
                - 坐标: x=%d, y=%d, z=%d
                - 朝向: %s, yaw=%.1f, pitch=%.1f
                - 生态群系: %s
                - 脚下方块: %s
                """.formatted(
                level.dimension().identifier(),
                pos.getX(), pos.getY(), pos.getZ(),
                player.getDirection().getName(), player.getYRot(), player.getXRot(),
                biome,
                blockSummary(foot, onPos, false)
        ).trim();
    }

    private static String readNearbyEntities(LocalPlayer player, ClientLevel level) {
        AABB box = player.getBoundingBox().inflate(NEARBY_RADIUS);
        List<Entity> entities = level.getEntities(player, box, entity -> entity != player && entity.isAlive());
        if (entities.isEmpty()) {
            return "附近 " + NEARBY_RADIUS + " 格内没有可见实体。";
        }

        Map<String, EntityGroup> groups = new LinkedHashMap<>();
        for (Entity entity : entities) {
            String id = entityTypeId(entity);
            EntityGroup group = groups.computeIfAbsent(id, key -> new EntityGroup(key, entity.getType().getCategory()));
            group.accept(entity, player.distanceTo(entity), isHostile(entity));
        }

        List<EntityGroup> sorted = groups.values().stream()
                .sorted(Comparator.comparingDouble(EntityGroup::nearestDistance))
                .limit(MAX_ENTITY_TYPES)
                .toList();

        StringBuilder builder = new StringBuilder("附近 ").append(NEARBY_RADIUS).append(" 格内实体：\n");
        for (EntityGroup group : sorted) {
            builder.append("- ").append(group.id())
                    .append(": 数量 ").append(group.count())
                    .append(", 最近 ").append(formatDistance(group.nearestDistance())).append(" 格")
                    .append(", 类别 ").append(group.category().getName())
                    .append(", 敌对 ").append(group.hostile() ? "是" : "否")
                    .append('\n');
        }
        if (groups.size() > MAX_ENTITY_TYPES) {
            builder.append("- 其余 ").append(groups.size() - MAX_ENTITY_TYPES).append(" 类已省略\n");
        }
        return builder.toString();
    }

    private static String readTarget(Minecraft minecraft) {
        HitResult hitResult = minecraft.hitResult;
        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS || minecraft.player == null || minecraft.level == null) {
            return "当前没有准星目标。";
        }
        if (minecraft.player.distanceToSqr(hitResult.getLocation()) > TARGET_MAX_DISTANCE * TARGET_MAX_DISTANCE) {
            return "当前准星目标超过 " + TARGET_MAX_DISTANCE + " 格，未附加详细信息。";
        }
        if (hitResult instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            return "准星目标方块：\n- " + blockSummary(state, pos, true);
        }
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            return "准星目标实体：\n- " + entitySummary(entity, minecraft.player.distanceTo(entity));
        }
        return "当前没有可识别的准星目标。";
    }

    private static List<String> collectStacks(Inventory inventory, int startInclusive, int endExclusive, boolean keepSlots) {
        Map<String, StackGroup> groups = new LinkedHashMap<>();
        int limit = Math.min(endExclusive, inventory.getContainerSize());
        for (int slot = startInclusive; slot < limit; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                String key = stackKey(stack);
                StackGroup group = groups.computeIfAbsent(key, ignored -> new StackGroup(stack.copy()));
                group.add(stack.getCount(), keepSlots ? String.valueOf(slot + 1) : null);
            }
        }
        return groups.values().stream()
                .map(StackGroup::line)
                .limit(MAX_INVENTORY_LINES)
                .toList();
    }

    private static List<String> collectEquipment(LocalPlayer player) {
        List<String> lines = new ArrayList<>();
        addEquipment(lines, "头盔", player.getItemBySlot(EquipmentSlot.HEAD));
        addEquipment(lines, "胸甲", player.getItemBySlot(EquipmentSlot.CHEST));
        addEquipment(lines, "护腿", player.getItemBySlot(EquipmentSlot.LEGS));
        addEquipment(lines, "靴子", player.getItemBySlot(EquipmentSlot.FEET));
        return lines;
    }

    private static List<String> collectSingle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        return List.of(itemSummary(stack, false));
    }

    private static void addEquipment(List<String> lines, String label, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            lines.add(label + ": " + itemSummary(stack, false));
        }
    }

    private static void appendStackLines(StringBuilder builder, String title, List<String> lines) {
        builder.append(title).append("：\n");
        if (lines.isEmpty()) {
            builder.append("- 空\n");
            return;
        }
        for (String line : lines) {
            builder.append("- ").append(line).append('\n');
        }
    }

    private static String itemSummary(ItemStack stack, boolean includeTags) {
        StringBuilder builder = new StringBuilder();
        builder.append(itemId(stack))
                .append(" (").append(stack.getHoverName().getString()).append(")")
                .append(" x").append(stack.getCount());
        if (stack.isDamageableItem()) {
            builder.append(", 耐久 ").append(stack.getMaxDamage() - stack.getDamageValue()).append('/').append(stack.getMaxDamage());
        }
        if (stack.isEnchanted()) {
            builder.append(", 已附魔");
        }
        String components = stack.getComponentsPatch().toString();
        if (!"{}".equals(components) && !"[]".equals(components) && !components.isBlank()) {
            builder.append(", 组件 ").append(shorten(components, 120));
        }
        if (includeTags) {
            List<String> tags = stack.getTags()
                    .limit(MAX_TAGS)
                    .map(TagKey::location)
                    .map(Object::toString)
                    .toList();
            if (!tags.isEmpty()) {
                builder.append(", tags ").append(String.join(", ", tags));
            }
        }
        return builder.toString();
    }

    private static String blockSummary(BlockState state, BlockPos pos, boolean includeTags) {
        StringBuilder builder = new StringBuilder();
        builder.append(blockId(state)).append(" @ ")
                .append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ());
        if (!state.getValues().isEmpty()) {
            builder.append(", 状态 ").append(formatProperties(state));
        }
        if (includeTags) {
            List<String> tags = state.getTags()
                    .limit(MAX_TAGS)
                    .map(TagKey::location)
                    .map(Object::toString)
                    .toList();
            if (!tags.isEmpty()) {
                builder.append(", tags ").append(String.join(", ", tags));
            }
        }
        return builder.toString();
    }

    private static String entitySummary(Entity entity, double distance) {
        StringBuilder builder = new StringBuilder();
        builder.append(entityTypeId(entity))
                .append(" (").append(entity.getName().getString()).append(")")
                .append(", 距离 ").append(formatDistance(distance)).append(" 格")
                .append(", 类别 ").append(entity.getType().getCategory().getName())
                .append(", 敌对 ").append(isHostile(entity) ? "是" : "否");
        if (entity instanceof LivingEntity living) {
            builder.append(", 生命 ").append(formatFloat(living.getHealth())).append('/').append(formatFloat(living.getMaxHealth()));
        }
        return builder.toString();
    }

    private static String formatProperties(BlockState state) {
        return state.getValues().entrySet().stream()
                .limit(8)
                .map(entry -> propertyName(entry.getKey()) + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static String propertyName(Property<?> property) {
        return property.getName();
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private static String entityTypeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String stackKey(ItemStack stack) {
        return itemId(stack) + "|" + stack.getHoverName().getString() + "|" + stack.getDamageValue() + "|" + stack.getComponentsPatch();
    }

    private static boolean isHostile(Entity entity) {
        return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.ROOT, "%.1f", distance);
    }

    private static String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static final class StackGroup {
        private final ItemStack sample;
        private final List<String> slots = new ArrayList<>();
        private int count;

        private StackGroup(ItemStack sample) {
            this.sample = sample;
        }

        private void add(int count, String slot) {
            this.count += count;
            if (slot != null) {
                slots.add(slot);
            }
        }

        private String line() {
            String value = itemSummary(sample.copyWithCount(count), false);
            if (!slots.isEmpty()) {
                value += ", 槽位 " + String.join("/", slots);
            }
            return value;
        }
    }

    private static final class EntityGroup {
        private final String id;
        private final MobCategory category;
        private int count;
        private double nearestDistance = Double.MAX_VALUE;
        private boolean hostile;

        private EntityGroup(String id, MobCategory category) {
            this.id = id;
            this.category = category;
        }

        private void accept(Entity entity, double distance, boolean hostile) {
            count++;
            nearestDistance = Math.min(nearestDistance, distance);
            this.hostile |= hostile;
        }

        private String id() {
            return id;
        }

        private MobCategory category() {
            return category;
        }

        private int count() {
            return count;
        }

        private double nearestDistance() {
            return nearestDistance;
        }

        private boolean hostile() {
            return hostile;
        }
    }

    public static final class AiToolException extends RuntimeException {
    }
}
