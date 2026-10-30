package com.risk.platform.contentpatrol.dedup;

/**
 * 编辑距离（Levenshtein），标准 DP 实现，O(m*n)。
 *
 * 用途：岗位标题通常只有十几个字，特征太少导致 SimHash 指纹随机性强、不稳定，
 * 因此标题走编辑距离兜底通道（双通道设计）。
 */
public final class EditDistance {

    private EditDistance() {
    }

    public static int of(String a, String b) {
        if (a == null || a.isEmpty()) {
            return b == null ? 0 : b.length();
        }
        if (b == null || b.isEmpty()) {
            return a.length();
        }
        int m = a.length(), n = b.length();
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int j = 0; j <= n; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[n];
    }
}
