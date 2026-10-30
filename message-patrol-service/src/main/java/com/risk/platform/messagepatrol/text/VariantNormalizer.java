package com.risk.platform.messagepatrol.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 对抗性文本归一化器：把黑产变体消息归一到"标准词空间"，再交给 AC 自动机匹配。
 *
 * 三步归一化：
 *   1. 符号归一：全角转半角、小写、emoji/符号映射（➕→加、❤→信）；
 *   2. 干扰符剔除：去掉空格、标点、装饰符，破解"加.薇.芯"式插点绕过；
 *   3. 变体映射：短语级替换（vx / v信 / 薇芯 / 威信 → 微信），词库只需维护标准词。
 *
 * 生产化差异：映射表存 MySQL 并热更新，黑产新变体由人审结果回流补表。
 */
public final class VariantNormalizer {

    /** 单字符归一映射：'➕'→"加"，'❤'→"信" 等。 */
    private final Map<Character, String> charMap = new HashMap<>();

    /** 短语变体映射：在干扰符剔除之后执行。 */
    private final List<Map.Entry<String, String>> phraseMap = new ArrayList<>();

    public VariantNormalizer() {
        // 符号/emoji 归一
        putChar('➕', "加");
        putChar('+', "加");
        putChar('❤', "信");
        putChar('♥', "信");
        putChar('Ｖ', "v");
        putChar('ｖ', "v");
        putChar('Ｘ', "x");
        putChar('ｘ', "x");
        // 高频变体短语（词序即优先级，先长后短）
        putPhrase("vx", "微信");
        putPhrase("wx", "微信");
        putPhrase("v信", "微信");
        putPhrase("薇芯", "微信");
        putPhrase("薇信", "微信");
        putPhrase("威芯", "微信");
        putPhrase("威信", "微信");
        putPhrase("微芯", "微信");
    }

    /** 主入口：原文 -> 标准词空间的文本。 */
    public String canonicalize(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (Character.isWhitespace(c) || isPunctuation(c)) {
                continue; // 干扰符剔除
            }
            String mapped = charMap.get(c);
            sb.append(mapped != null ? mapped : c);
        }
        String t = sb.toString();
        for (Map.Entry<String, String> e : phraseMap) {
            if (t.contains(e.getKey())) {
                t = t.replace(e.getKey(), e.getValue());
            }
        }
        return t;
    }

    /** 动态补充短语变体（生产由人审回流驱动）。 */
    public void putPhrase(String variant, String canonical) {
        phraseMap.add(0, Map.entry(variant, canonical));
    }

    private void putChar(char c, String s) {
        charMap.put(c, s);
    }

    private static boolean isPunctuation(char c) {
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@')
                || (c >= '[' && c <= '`') || (c >= '{' && c <= '~')
                || c == '，' || c == '。' || c == '、' || c == '【' || c == '】'
                || c == '；' || c == '：' || c == '？' || c == '！'
                || c == '“' || c == '”' || c == '（' || c == '）'
                || c == '·' || c == '—' || c == '－' || c == '～';
    }
}
