package com.risk.platform;

import com.risk.platform.model.Decision;
import com.risk.platform.model.PatrolVerdict;
import com.risk.platform.pipeline.AbstractPatrolPipeline;
import com.risk.platform.rule.Condition;
import com.risk.platform.rule.Rule;
import com.risk.platform.rule.RuleEngine;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformSkeletonTest {

    /** 规则引擎：优先级短路——数字小的规则先求值，命中即返回。 */
    @Test
    void ruleEnginePriorityShortCircuit() {
        RuleEngine engine = new RuleEngine(List.of(
                new Rule("r2", 20, true,
                        List.of(new Condition("score", Condition.Op.GE, 60)), Decision.INTERCEPT, "HIGH_SCORE"),
                new Rule("r1", 10, true,
                        List.of(new Condition("score", Condition.Op.GE, 80)), Decision.BAN, "CRITICAL_SCORE")));

        PatrolVerdict v = engine.evaluate("evt:1", Map.of("score", 85));
        assertEquals(Decision.BAN, v.decision());
        assertEquals("CRITICAL_SCORE", v.reason());
        assertEquals(100, v.score());
    }

    /** 规则引擎：禁用规则跳过；无命中回默认放行。 */
    @Test
    void ruleEngineDisabledAndDefaultPass() {
        RuleEngine engine = new RuleEngine(List.of(
                new Rule("r1", 10, false,
                        List.of(new Condition("score", Condition.Op.GE, 50)), Decision.INTERCEPT, "SHOULD_NOT_HIT")));

        assertEquals(Decision.PASS, engine.evaluate("evt:2", Map.of("score", 90)).decision());
        PatrolVerdict pass = engine.evaluate("evt:3", Map.of("score", 10));
        assertEquals(RuleEngine.REASON_DEFAULT, pass.reason());
    }

    /** Pipeline 四阶段骨架：子类只实现钩子，骨架顺序固定。 */
    @Test
    void pipelineSkeletonReusedAcrossDomains() {
        var pipeline = new AbstractPatrolPipeline<String>() {
            @Override
            public String domain() {
                return "content";
            }

            @Override
            protected String normalize(String event) {
                return event.trim().toLowerCase();
            }

            @Override
            protected Map<String, Object> extract(String normalized, String event) {
                return Map.of("len", normalized.length());
            }

            @Override
            protected Detection detect(Map<String, Object> features, String event) {
                int len = (int) features.get("len");
                return len > 3
                        ? new Detection(Decision.INTERCEPT, 80, "TOO_LONG")
                        : new Detection(Decision.PASS, 0, "OK");
            }

            @Override
            protected String eventIdOf(String event) {
                return "evt:" + event;
            }
        };

        assertEquals("content", pipeline.domain());
        assertEquals(Decision.INTERCEPT, pipeline.patrol("  ABCD ").decision());
        assertEquals("evt:AB", pipeline.patrol("AB").eventId());
        assertTrue(pipeline.patrol("ab").detail().isEmpty());
    }
}
