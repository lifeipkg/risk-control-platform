package com.risk.platform.messagepatrol.engine;

import com.risk.platform.messagepatrol.automaton.AhoCorasick;

import java.util.List;

/**
 * 引擎内部巡查结果（控制器负责映射为统一 PatrolVerdict）。
 *
 * @param messageId 消息 ID
 * @param decision  统一决策
 * @param reason    判定来源标识
 * @param hits      关键词命中明细
 */
public record PatrolResult(long messageId, com.risk.platform.model.Decision decision,
                           String reason, List<AhoCorasick.Match> hits) {
}
