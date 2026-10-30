package com.risk.platform.rule;

import com.risk.platform.model.Decision;

import java.util.List;
import java.util.Map;

/**
 * 风控规则：多条条件 AND 组合，命中则给出决策。
 *
 * @param id        规则 ID
 * @param priority  优先级（数字越小越优先，RuleEngine 按此序求值）
 * @param enabled   是否启用（热更新开关）
 * @param conditions 条件列表（AND 语义）
 * @param decision  命中后的决策
 * @param reason    决策来源标识
 */
public record Rule(String id, int priority, boolean enabled,
                   List<Condition> conditions, Decision decision, String reason) {

    public Rule {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }

    public boolean match(Map<String, Object> facts) {
        if (!enabled) {
            return false;
        }
        for (Condition c : conditions) {
            if (!c.match(facts)) {
                return false;
            }
        }
        return true;
    }
}
