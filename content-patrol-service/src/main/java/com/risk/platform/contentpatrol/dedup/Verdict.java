package com.risk.platform.contentpatrol.dedup;

/**
 * 判重内部结论（引擎层，控制器负责映射为统一 PatrolVerdict）。
 *
 * @param jobId             当前岗位 ID
 * @param duplicate         是否判重命中
 * @param matchedId         命中的既有岗位 ID（未命中为 -1）
 * @param hammingDistance   SimHash 主通道汉明距离（未计算为 -1）
 * @param titleEditDistance 标题兜底通道编辑距离（未计算为 null）
 * @param reason            判定来源
 */
public record Verdict(long jobId, boolean duplicate, long matchedId,
                      int hammingDistance, Integer titleEditDistance, String reason) {
}
