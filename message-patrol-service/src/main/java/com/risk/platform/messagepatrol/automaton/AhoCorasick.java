package com.risk.platform.messagepatrol.automaton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick 多模式匹配自动机（无第三方依赖实现）。
 *
 * 选型理由：
 *   - 消息风控关键词量级数千到数万，逐条 contains 是 O(词库大小 x 文本长度)；
 *   - AC 自动机对文本一次扫描 O(len) 完成全部关键词匹配，单条消息亚毫秒；
 *   - 构建 O(词库总长)，词库热更新时重建成本可接受（生产上双缓冲切换）。
 *
 * 实现要点：build 时把 fail 链上的输出合并到当前节点（LinkedHashSet 去重），
 * 匹配阶段无需再回溯 fail 链，严格 O(文本长度)。
 */
public final class AhoCorasick {

    private static final int ROOT = 0;

    /** 命中结果。 */
    public record Match(String word, int start, int end) {
    }

    private final List<Map<Character, Integer>> next = new ArrayList<>();
    private final List<LinkedHashSet<String>> patterns = new ArrayList<>();
    private final List<Integer> fail = new ArrayList<>();
    private boolean built;

    public AhoCorasick() {
        newNode();
    }

    /** 加入一个模式词（build 之后不允许再 add）。 */
    public void add(String word) {
        if (built) {
            throw new IllegalStateException("build() 之后不可再 add");
        }
        if (word == null || word.isEmpty()) {
            return;
        }
        int cur = ROOT;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            Integer t = next.get(cur).get(c);
            if (t == null) {
                t = newNode();
                next.get(cur).put(c, t);
            }
            cur = t;
        }
        patterns.get(cur).add(word);
    }

    /** BFS 构建 fail 指针，并合并 fail 链输出。 */
    public void build() {
        Queue<Integer> queue = new ArrayDeque<>();
        for (int child : next.get(ROOT).values()) {
            fail.set(child, ROOT);
            queue.add(child);
        }
        while (!queue.isEmpty()) {
            int u = queue.poll();
            patterns.get(u).addAll(patterns.get(fail.get(u)));
            for (Map.Entry<Character, Integer> e : next.get(u).entrySet()) {
                char c = e.getKey();
                int v = e.getValue();
                int f = fail.get(u);
                while (f != ROOT && !next.get(f).containsKey(c)) {
                    f = fail.get(f);
                }
                Integer t = next.get(f).get(c);
                fail.set(v, (t != null && t != v) ? t : ROOT);
                queue.add(v);
            }
        }
        built = true;
    }

    /** 一次扫描返回全部命中（含位置）。 */
    public List<Match> find(String text) {
        if (!built) {
            throw new IllegalStateException("请先调用 build()");
        }
        List<Match> out = new ArrayList<>();
        int cur = ROOT;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            while (cur != ROOT && !next.get(cur).containsKey(c)) {
                cur = fail.get(cur);
            }
            Integer t = next.get(cur).get(c);
            cur = (t == null) ? ROOT : t;
            for (String p : patterns.get(cur)) {
                out.add(new Match(p, i - p.length() + 1, i + 1));
            }
        }
        return out;
    }

    private int newNode() {
        next.add(new HashMap<>());
        patterns.add(new LinkedHashSet<>());
        fail.add(ROOT);
        return next.size() - 1;
    }
}
