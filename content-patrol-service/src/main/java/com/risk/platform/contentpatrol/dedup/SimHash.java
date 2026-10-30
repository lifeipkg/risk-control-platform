package com.risk.platform.contentpatrol.dedup;

/**
 * SimHash 指纹生成。
 *
 * 设计要点（与生产方案一致）：
 * 1. 特征不用分词器，用字符级 3-gram —— 蓝领岗位文本口语化、极短、高度模板化，
 *    通用分词器不稳定；n-gram 对口语化短文本更鲁棒。
 * 2. 权重用 TF（生产上可扩展 tf-idf）。
 * 3. 哈希用 FNV-1a 64bit，无第三方依赖，分布均匀性满足投票需求。
 */
public final class SimHash {

    private static final int FINGERPRINT_BITS = 64;
    private static final int NGRAM = 3;
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private SimHash() {
    }

    /** 对原文生成 64bit SimHash 指纹。 */
    public static long of(String rawText) {
        java.util.Map<String, Integer> tf = new java.util.HashMap<>();
        for (String gram : ngrams(normalize(rawText))) {
            tf.merge(gram, 1, Integer::sum);
        }

        int[] votes = new int[FINGERPRINT_BITS];
        for (java.util.Map.Entry<String, Integer> e : tf.entrySet()) {
            long h = fnv1a64(e.getKey());
            int weight = e.getValue();
            for (int i = 0; i < FINGERPRINT_BITS; i++) {
                votes[i] += ((h >>> i) & 1L) == 1L ? weight : -weight;
            }
        }

        long fingerprint = 0L;
        for (int i = 0; i < FINGERPRINT_BITS; i++) {
            if (votes[i] > 0) {
                fingerprint |= (1L << i);
            }
        }
        return fingerprint;
    }

    /** 两个指纹的汉明距离（位运算，O(1)）。 */
    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    /** 归一化：去空白/标点，转小写。 */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c) || isPunctuation(c)) {
                continue;
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    private static boolean isPunctuation(char c) {
        return (c >= '!' && c <= '/') || (c >= ':' && c <= '@')
                || (c >= '[' && c <= '`') || (c >= '{' && c <= '~')
                || c == '，' || c == '。' || c == '、' || c == '【' || c == '】'
                || c == '；' || c == '：' || c == '？' || c == '！'
                || c == '“' || c == '”' || c == '（' || c == '）';
    }

    private static java.util.List<String> ngrams(String s) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (s.isEmpty()) {
            return out;
        }
        if (s.length() < NGRAM) {
            out.add(s);
            return out;
        }
        for (int i = 0; i <= s.length() - NGRAM; i++) {
            out.add(s.substring(i, i + NGRAM));
        }
        return out;
    }

    /** FNV-1a 64bit 字符串哈希。 */
    static long fnv1a64(String s) {
        long h = FNV_OFFSET_BASIS;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= FNV_PRIME;
        }
        return h;
    }
}
