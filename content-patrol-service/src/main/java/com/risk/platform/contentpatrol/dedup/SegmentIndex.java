package com.risk.platform.contentpatrol.dedup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 抽屉原理分段倒排索引（本服务为内存实现，生产为 MySQL 段倒排表，见 deploy/sql/job-patrol-schema.sql）。
 *
 * 核心定理：64bit 指纹切成 4 段各 16bit，若两指纹汉明距离 d &lt;= 3，
 * 则 3 个差异 bit 至多落在 3 个段里，【必有一段完全相同】（抽屉原理）。
 * 因此只需按"段相等"建 4 张倒排表，等值查询取并集即为候选集，
 * 候选规模从全量 O(N) 降到 O(几百)，再对候选集算精确汉明距离。
 */
public class SegmentIndex {

    /** 候选项：岗位 ID + 指纹。 */
    public record Candidate(long id, long fingerprint) {
    }

    @SuppressWarnings("unchecked")
    private final Map<Integer, List<Candidate>>[] segments = new HashMap[4];

    public SegmentIndex() {
        for (int i = 0; i < 4; i++) {
            segments[i] = new HashMap<>();
        }
    }

    /** 指纹入库：写入 4 个段倒排表。 */
    public void add(long id, long fingerprint) {
        for (int i = 0; i < 4; i++) {
            segments[i].computeIfAbsent(segmentValue(i, fingerprint), k -> new ArrayList<>())
                    .add(new Candidate(id, fingerprint));
        }
    }

    /**
     * 取候选集：4 个段中"至少一段相等"的所有指纹（按岗位 ID 去重）。
     * 任何汉明距离 &lt;= 3 的已入库指纹必然出现在返回结果中。
     */
    public List<Candidate> candidates(long fingerprint) {
        LinkedHashMap<Long, Candidate> out = new LinkedHashMap<>();
        for (int i = 0; i < 4; i++) {
            List<Candidate> bucket = segments[i].get(segmentValue(i, fingerprint));
            if (bucket != null) {
                for (Candidate c : bucket) {
                    out.putIfAbsent(c.id(), c);
                }
            }
        }
        return new ArrayList<>(out.values());
    }

    /** 抽屉原理性质判断：两指纹是否至少有一个段完全相同。 */
    public static boolean shareSegment(long a, long b) {
        for (int i = 0; i < 4; i++) {
            if (segmentValue(i, a) == segmentValue(i, b)) {
                return true;
            }
        }
        return false;
    }

    /** 第 segIdx 段的 16bit 段值。 */
    static int segmentValue(int segIdx, long fingerprint) {
        return (int) ((fingerprint >>> (16 * segIdx)) & 0xFFFFL);
    }
}
