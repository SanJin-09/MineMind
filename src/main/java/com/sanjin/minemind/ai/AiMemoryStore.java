package com.sanjin.minemind.ai;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiMemoryStore {
    private static final String HEADER = "# MineMind Long-term Memory";
    private static final int MAX_MEMORY_ENTRIES = 20;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern ENTRY_PREFIX_DATE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\b.*");
    private static final Pattern CJK_SEQUENCE = Pattern.compile("[\\p{IsHan}]{2,}");

    private AiMemoryStore() {
    }

    public static MemoryWriteResult remember(String playerName, String content) {
        return remember(path(), playerName, content, LocalDateTime.now());
    }

    public static MemoryWriteResult remember(String playerName, String content, LocalDateTime time) {
        return remember(path(), playerName, content, time);
    }

    static MemoryWriteResult remember(Path path, String playerName, String content, LocalDateTime time) {
        String cleaned = requireText(content, "记忆内容不能为空");
        String date = DATE_FORMAT.format(time);
        String entry = TIME_FORMAT.format(time) + " | 玩家: " + singleLine(playerName, "玩家") + " | " + cleaned;
        List<MemoryEntry> entries = new ArrayList<>(readEntries(path));
        entries.add(new MemoryEntry(date, entry));
        writeEntries(path, entries);
        return new MemoryWriteResult(date, cleaned);
    }

    public static int forget(String keyword) {
        return forget(path(), keyword);
    }

    static int forget(Path path, String keyword) {
        String cleaned = requireText(keyword, "遗忘关键词不能为空");
        List<MemoryEntry> entries = readEntries(path);
        if (entries.isEmpty()) {
            return 0;
        }

        String normalizedKeyword = cleaned.toLowerCase(Locale.ROOT);
        List<MemoryEntry> kept = new ArrayList<>();
        int removed = 0;
        for (MemoryEntry entry : entries) {
            if (entry.text().toLowerCase(Locale.ROOT).contains(normalizedKeyword)) {
                removed++;
            } else {
                kept.add(entry);
            }
        }
        if (removed > 0) {
            writeEntries(path, kept);
        }
        return removed;
    }

    public static List<MemoryDeletionCandidate> deletionCandidates(String query) {
        return deletionCandidates(path(), query, MAX_MEMORY_ENTRIES);
    }

    static List<MemoryDeletionCandidate> deletionCandidates(Path path, String query, int maxEntries) {
        List<IndexedMemoryEntry> entries = indexedEntries(path);
        if (entries.isEmpty()) {
            return List.of();
        }

        int limit = Math.max(1, maxEntries);
        List<IndexedMemoryEntry> recentFirst = new ArrayList<>(entries);
        recentFirst.sort(Comparator.comparingInt(IndexedMemoryEntry::id).reversed());

        List<String> keywords = keywords(query);
        List<IndexedMemoryEntry> selected = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (IndexedMemoryEntry entry : recentFirst) {
            if (selected.size() >= limit) {
                break;
            }
            if (matches(entry.entry(), keywords) && seen.add(entry.id())) {
                selected.add(entry);
            }
        }
        for (IndexedMemoryEntry entry : recentFirst) {
            if (selected.size() >= limit) {
                break;
            }
            if (seen.add(entry.id())) {
                selected.add(entry);
            }
        }

        return selected.stream()
                .map(entry -> new MemoryDeletionCandidate(entry.id(), entry.entry().text()))
                .toList();
    }

    public static MemoryDeleteResult forgetByIds(List<Integer> ids) {
        return forgetByIds(path(), ids);
    }

    static MemoryDeleteResult forgetByIds(Path path, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return new MemoryDeleteResult(0, List.of());
        }
        Set<Integer> deleteIds = new LinkedHashSet<>(ids);
        List<MemoryEntry> entries = readEntries(path);
        if (entries.isEmpty()) {
            return new MemoryDeleteResult(0, List.of());
        }

        List<MemoryEntry> kept = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            MemoryEntry entry = entries.get(index);
            if (deleteIds.contains(index + 1)) {
                removed.add(entry.text());
            } else {
                kept.add(entry);
            }
        }
        if (!removed.isEmpty()) {
            writeEntries(path, kept);
        }
        return new MemoryDeleteResult(removed.size(), removed);
    }

    public static AiToolResult readContextResult(String query) {
        return new AiToolResult("tool.memory.read", "@memory", "长期记忆", readRelevant(path(), query), false);
    }

    static String readRelevant(Path path, String query) {
        return readRelevant(path, query, MAX_MEMORY_ENTRIES);
    }

    static String readRelevant(Path path, String query, int maxEntries) {
        List<MemoryEntry> entries = readEntries(path);
        if (entries.isEmpty()) {
            return "长期记忆为空。";
        }

        int limit = Math.max(1, maxEntries);
        List<MemoryEntry> recentFirst = new ArrayList<>(entries);
        recentFirst.sort(Comparator.comparingInt(entries::indexOf).reversed());

        List<String> keywords = keywords(query);
        List<MemoryEntry> selected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (MemoryEntry entry : recentFirst) {
            if (selected.size() >= limit) {
                break;
            }
            if (matches(entry, keywords) && seen.add(entry.text())) {
                selected.add(entry);
            }
        }
        for (MemoryEntry entry : recentFirst) {
            if (selected.size() >= limit) {
                break;
            }
            if (seen.add(entry.text())) {
                selected.add(entry);
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("长期记忆（已选 ").append(selected.size()).append(" 条，优先关键词匹配，其次最近记录）：\n");
        for (MemoryEntry entry : selected) {
            builder.append("- ").append(entry.text()).append('\n');
        }
        return builder.toString();
    }

    public static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("minemind-memory.md");
    }

    private static List<MemoryEntry> readEntries(Path path) {
        if (path == null || !Files.exists(path)) {
            return List.of();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new MemoryException("长期记忆文件读写失败，请重试");
        }

        List<MemoryEntry> entries = new ArrayList<>();
        String currentDate = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("## ")) {
                currentDate = trimmed.substring(3).trim();
                continue;
            }
            if (!trimmed.startsWith("- ")) {
                continue;
            }
            String text = trimmed.substring(2).trim();
            if (!text.isBlank()) {
                String date = currentDate.isBlank() ? dateFromEntry(text) : currentDate;
                entries.add(new MemoryEntry(date, text));
            }
        }
        return entries;
    }

    private static List<IndexedMemoryEntry> indexedEntries(Path path) {
        List<MemoryEntry> entries = readEntries(path);
        List<IndexedMemoryEntry> indexed = new ArrayList<>();
        for (int index = 0; index < entries.size(); index++) {
            indexed.add(new IndexedMemoryEntry(index + 1, entries.get(index)));
        }
        return indexed;
    }

    private static void writeEntries(Path path, List<MemoryEntry> entries) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        String currentDate = "";
        for (MemoryEntry entry : entries) {
            if (!entry.date().equals(currentDate)) {
                lines.add("");
                lines.add("## " + entry.date());
                currentDate = entry.date();
            }
            lines.add("- " + entry.text());
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new MemoryException("长期记忆文件读写失败，请重试");
        }
    }

    private static boolean matches(MemoryEntry entry, List<String> keywords) {
        if (keywords.isEmpty()) {
            return false;
        }
        String text = entry.text().toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> keywords(String query) {
        String cleaned = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();
        if (cleaned.isBlank()) {
            return List.of();
        }

        Set<String> values = new LinkedHashSet<>();
        for (String token : cleaned.split("[^\\p{IsHan}\\p{L}\\p{N}]+")) {
            if (token.length() >= 2) {
                values.add(token);
            }
        }

        Matcher matcher = CJK_SEQUENCE.matcher(cleaned);
        while (matcher.find()) {
            String value = matcher.group();
            for (int length = 4; length >= 2; length--) {
                for (int index = 0; index + length <= value.length(); index++) {
                    values.add(value.substring(index, index + length));
                    if (values.size() >= 32) {
                        return List.copyOf(values);
                    }
                }
            }
        }
        return List.copyOf(values);
    }

    private static String dateFromEntry(String entry) {
        Matcher matcher = ENTRY_PREFIX_DATE.matcher(entry);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "未分组";
    }

    private static String requireText(String value, String error) {
        String cleaned = singleLine(value, "");
        if (cleaned.isBlank()) {
            throw new MemoryException(error);
        }
        return cleaned;
    }

    private static String singleLine(String value, String fallback) {
        String cleaned = value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isBlank() ? fallback : cleaned;
    }

    record MemoryWriteResult(String date, String content) {
    }

    record MemoryDeletionCandidate(int id, String text) {
    }

    record MemoryDeleteResult(int count, List<String> contents) {
        String summary() {
            if (contents == null || contents.isEmpty()) {
                return "无";
            }
            return String.join("；", contents);
        }
    }

    private record IndexedMemoryEntry(int id, MemoryEntry entry) {
    }

    private record MemoryEntry(String date, String text) {
    }

    public static final class MemoryException extends RuntimeException {
        public MemoryException(String message) {
            super(message);
        }
    }
}
