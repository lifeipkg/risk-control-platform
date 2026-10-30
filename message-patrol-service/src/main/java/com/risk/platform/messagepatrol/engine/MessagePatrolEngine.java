package com.risk.platform.messagepatrol.engine;

import com.risk.platform.messagepatrol.automaton.AhoCorasick;
import com.risk.platform.messagepatrol.text.VariantNormalizer;
import com.risk.platform.model.Decision;

import java.util.List;

/**
 * 飞单/私单消息巡查引擎（消息巡查线主检测器）。
 *
 * 判定流程：
 *   1. 变体归一化（符号归一 -> 干扰符剔除 -> 短语变体映射）；
 *   2. AC 自动机一次扫描命中全部关键词；
 *   3. 白名单降级（官方渠道话术避免误杀）；
 *   4. 角色分级处置：招聘侧命中 = 拦截（飞单诱导主体），求职侧命中 = 仅提示。
 *
 * 生产化差异（见 README / deploy/sql/message-patrol-schema.sql）：
 *   - 关键词库/变体映射/白名单存 MySQL，双缓冲热更新（重建 AC 自动机后原子切换引用）；
 *   - Kafka 异步消费 IM 消息事件，不阻塞发送链路；
 *   - 命中记录 + 人审结果回流，持续补充变体映射。
 */
public class MessagePatrolEngine {

    public static final String REASON_OK = "OK";
    public static final String REASON_RECRUITER = "KEYWORD_HIT_RECRUITER";
    public static final String REASON_WORKER = "KEYWORD_HIT_WORKER";
    public static final String REASON_WHITELIST = "WHITELIST";

    private final VariantNormalizer normalizer;
    private final AhoCorasick keywordAutomaton;
    private final List<String> whitelist;

    public MessagePatrolEngine(List<String> keywords, List<String> whitelist) {
        this(keywords, whitelist, new VariantNormalizer());
    }

    public MessagePatrolEngine(List<String> keywords, List<String> whitelist, VariantNormalizer normalizer) {
        this.normalizer = normalizer;
        this.keywordAutomaton = new AhoCorasick();
        for (String k : keywords) {
            String canonical = normalizer.canonicalize(k);
            if (!canonical.isEmpty()) {
                keywordAutomaton.add(canonical);
            }
        }
        keywordAutomaton.build();
        this.whitelist = whitelist == null ? List.of() : whitelist;
    }

    public PatrolResult check(ChatMessage msg) {
        String normalized = normalizer.canonicalize(msg.content());

        List<AhoCorasick.Match> hits = keywordAutomaton.find(normalized);
        if (hits.isEmpty()) {
            return new PatrolResult(msg.id(), Decision.PASS, REASON_OK, hits);
        }

        // 白名单降级：官方渠道话术（如"平台官方微信客服"）不拦截，避免误杀
        for (String w : whitelist) {
            if (normalized.contains(normalizer.canonicalize(w))) {
                return new PatrolResult(msg.id(), Decision.PASS, REASON_WHITELIST, hits);
            }
        }

        // 角色分级处置
        return switch (msg.senderRole()) {
            case RECRUITER -> new PatrolResult(msg.id(), Decision.INTERCEPT, REASON_RECRUITER, hits);
            case WORKER -> new PatrolResult(msg.id(), Decision.WARN, REASON_WORKER, hits);
        };
    }
}
