package com.risk.platform.accountpatrol.engine;

import com.risk.platform.model.PatrolVerdict;
import com.risk.platform.rule.Condition;
import com.risk.platform.rule.Rule;
import com.risk.platform.rule.RuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 账号巡查引擎（账号巡查线主检测器，规则引擎驱动）。
 *
 * 特征事实（由事件归一化产出）：
 *   virtualSegment      是否命中接码/虚拟号段（170/165/166/167 等）
 *   sameIpRegister24h   同 IP 24h 注册账号数
 *   sameDeviceAccounts  同设备关联账号数
 *   registerHour        注册时间（0-23）
 *   postCount24h        24h 发帖/触达次数（养号行为特征）
 *
 * 分级策略（决策不对称：误杀真实工人代价极高，优先打击确定性信号）：
 *   1. 虚拟号段 + 设备聚集        -> BAN（确定性设备农场）
 *   2. 设备聚集（同设备>=5账号）  -> BAN
 *   3. 同 IP 批量注册（>=10）     -> INTERCEPT
 *   4. 虚拟号段单独命中           -> EXTRA_REVIEW（人审）
 *   5. 夜间批量注册               -> EXTRA_REVIEW
 *   6. 高频发帖（养号特征）       -> LIMIT
 *   7. 默认                       -> PASS
 */
@Component
public class AccountPatrolEngine {

    public static final String DOMAIN = "account";

    private final RuleEngine ruleEngine;

    public AccountPatrolEngine(
            @Value("${patrol.account.virtual-segment-device-count:3}") int vsDeviceCount,
            @Value("${patrol.account.same-device-ban-count:5}") int sameDeviceBan,
            @Value("${patrol.account.same-ip-intercept-count:10}") int sameIpIntercept,
            @Value("${patrol.account.same-ip-night-count:5}") int sameIpNight,
            @Value("${patrol.account.post-limit-count:20}") int postLimit) {
        this.ruleEngine = new RuleEngine(List.of(
                new Rule("acc-001", 10, true,
                        List.of(
                                new Condition("virtualSegment", Condition.Op.EQ, true),
                                new Condition("sameDeviceAccounts", Condition.Op.GE, vsDeviceCount)),
                        com.risk.platform.model.Decision.BAN, "VIRTUAL_SEGMENT_DEVICE_FARM"),
                new Rule("acc-002", 20, true,
                        List.of(new Condition("sameDeviceAccounts", Condition.Op.GE, sameDeviceBan)),
                        com.risk.platform.model.Decision.BAN, "SAME_DEVICE_FARM"),
                new Rule("acc-003", 30, true,
                        List.of(new Condition("sameIpRegister24h", Condition.Op.GE, sameIpIntercept)),
                        com.risk.platform.model.Decision.INTERCEPT, "SAME_IP_BULK_REGISTER"),
                new Rule("acc-004", 40, true,
                        List.of(new Condition("virtualSegment", Condition.Op.EQ, true)),
                        com.risk.platform.model.Decision.EXTRA_REVIEW, "VIRTUAL_SEGMENT"),
                new Rule("acc-005", 50, true,
                        List.of(
                                new Condition("registerHour", Condition.Op.GE, 0),
                                new Condition("registerHour", Condition.Op.LTE, 6),
                                new Condition("sameIpRegister24h", Condition.Op.GE, sameIpNight)),
                        com.risk.platform.model.Decision.EXTRA_REVIEW, "NIGHT_BULK_REGISTER"),
                new Rule("acc-006", 60, true,
                        List.of(new Condition("postCount24h", Condition.Op.GE, postLimit)),
                        com.risk.platform.model.Decision.LIMIT, "HIGH_FREQUENCY_POST")),
                com.risk.platform.model.Decision.PASS);
    }

    /** 对账号事件事实求值。 */
    public PatrolVerdict evaluate(String eventId, Map<String, Object> facts) {
        return ruleEngine.evaluate(eventId, facts);
    }
}
