package com.risk.platform.contentpatrol.dedup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 岗位判重引擎（内容巡查线主检测器）。
 *
 * 判重主流程：
 *   1. 按「城市 + 工种」分片 —— 业务语义分层：跨城市同模板属合法重复，直接隔离；
 *   2. 正文走 SimHash 主通道：先经分段倒排索引取候选集，再算精确汉明距离；
 *   3. 标题走编辑距离兜底通道（短文本 SimHash 不稳定）；
 *   4. 双通道任一命中即判重，返回判定来源。
 *
 * 生产化差异（本服务未包含，设计见 README / deploy/sql/job-patrol-schema.sql）：
 *   - 指纹库为 MySQL 段倒排表，30 天滑动窗口过期清理；
 *   - 发布事件经 Kafka 异步消费，端到端延迟 &lt; 2s；
 *   - 消费幂等：岗位 ID + 版本号，状态机只允许单调前进。
 */
@Component
public class DedupEngine {

    public static final int DEFAULT_HAMMING_THRESHOLD = 3;
    public static final int DEFAULT_TITLE_EDIT_THRESHOLD = 4;

    public static final String REASON_SIMHASH_BODY = "SIMHASH_BODY";
    public static final String REASON_TITLE_EDIT = "TITLE_EDIT_DISTANCE";
    public static final String REASON_OK = "OK";

    private final int hammingThreshold;
    private final int titleEditThreshold;

    private final Map<String, SegmentIndex> shardIndexes = new HashMap<>();
    private final Map<String, List<JobPost>> shardJobs = new HashMap<>();

    public DedupEngine(@Value("${patrol.content.hamming-threshold:3}") int hammingThreshold,
                       @Value("${patrol.content.title-edit-threshold:4}") int titleEditThreshold) {
        this.hammingThreshold = hammingThreshold;
        this.titleEditThreshold = titleEditThreshold;
    }

    /** 对新岗位做判重，并把指纹入库（"发布即巡查"）。 */
    public synchronized Verdict check(JobPost job) {
        String shard = job.shardKey();
        SegmentIndex index = shardIndexes.computeIfAbsent(shard, k -> new SegmentIndex());
        long fp = SimHash.of(job.body());

        // ---- 主通道：正文 SimHash ----
        int bestDistance = Integer.MAX_VALUE;
        long bestId = -1;
        for (SegmentIndex.Candidate c : index.candidates(fp)) {
            int d = SimHash.hammingDistance(fp, c.fingerprint());
            if (d < bestDistance) {
                bestDistance = d;
                bestId = c.id();
            }
        }
        // 正确性说明：若真实最小距离 <= 阈值，由抽屉原理该指纹必在候选集中，不会漏判。
        boolean duplicate = bestDistance <= hammingThreshold;

        Integer titleDistance = null;
        String reason;
        if (duplicate) {
            reason = REASON_SIMHASH_BODY;
        } else {
            // ---- 兜底通道：标题编辑距离（短标题 SimHash 不稳定） ----
            for (JobPost other : shardJobs.getOrDefault(shard, List.of())) {
                int d = EditDistance.of(job.title(), other.title());
                if (d <= titleEditThreshold && (titleDistance == null || d < titleDistance)) {
                    titleDistance = d;
                    bestId = other.id();
                }
            }
            if (titleDistance != null) {
                duplicate = true;
                reason = REASON_TITLE_EDIT;
            } else {
                reason = REASON_OK;
            }
        }

        // 入库（生产为异步写 MySQL 段倒排 + 30 天滑动窗口）
        index.add(job.id(), fp);
        shardJobs.computeIfAbsent(shard, k -> new ArrayList<>()).add(job);

        return new Verdict(job.id(), duplicate,
                duplicate ? bestId : -1,
                bestDistance == Integer.MAX_VALUE ? -1 : bestDistance,
                titleDistance, reason);
    }

    /** 查看某分片已入库岗位数（观测用）。 */
    public synchronized int shardSize(String city, String trade) {
        return shardJobs.getOrDefault(city + "|" + trade, List.of()).size();
    }
}
