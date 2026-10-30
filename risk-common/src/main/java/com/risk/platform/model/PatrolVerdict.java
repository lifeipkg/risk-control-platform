package com.risk.platform.model;

import java.util.Map;

/**
 * 巡查结论（各巡查线统一返回结构，也是巡查线服务对网关的统一响应契约）。
 *
 * @param eventId 事件 ID（岗位ID/消息ID/账号ID/资金单ID）
 * @param decision 决策
 * @param score    风险分（0-100，规则引擎/检测器融合分）
 * @param reason   判定来源标识
 * @param detail   附加信息（命中词、相似岗位、团伙ID 等）
 */
public record PatrolVerdict(String eventId, Decision decision, int score, String reason,
                            Map<String, Object> detail) {

    public PatrolVerdict {
        detail = detail == null ? Map.of() : Map.copyOf(detail);
    }
}
