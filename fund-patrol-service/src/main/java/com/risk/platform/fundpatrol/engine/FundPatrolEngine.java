package com.risk.platform.fundpatrol.engine;

import com.risk.platform.model.PatrolVerdict;
import com.risk.platform.rule.Condition;
import com.risk.platform.rule.Rule;
import com.risk.platform.rule.RuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 资金巡查引擎（资金巡查线主检测器，规则引擎驱动）。
 *
 * 特征事实（由补贴发放事件归一化产出）：
 *   grantAmount          本次补贴金额（元）
 *   accountAgeDays       领补贴时账号年龄（天）——"放款前后分层"
 *   sameDeviceAccounts   同设备关联账号数
 *   sameBankCardAccounts 同银行卡关联账号数（团伙最强信号）
 *   applyCount24h        24h 补贴申请次数
 *
 * 分层策略（资金损失不可逆，处置从严；团伙信号优先于单点信号）：
 *   1. 同银行卡 >=3 账号               -> BAN（团伙）
 *   2. 同设备 >=3 账号 + 金额 >=500    -> INTERCEPT
 *   3. 新号（<=3 天）+ 高额（>=200）   -> EXTRA_REVIEW
 *   4. 24h 申请 >=5 次                 -> LIMIT
 *   5. 默认                            -> PASS
 */
@Component
public class FundPatrolEngine {

    public static final String DOMAIN = "fund";

    private final RuleEngine ruleEngine;

    public FundPatrolEngine(
            @Value("${patrol.fund.bankcard-gang-count:3}") int bankcardGang,
            @Value("${patrol.fund.device-gang-count:3}") int deviceGang,
            @Value("${patrol.fund.device-gang-amount:500}") int deviceGangAmount,
            @Value("${patrol.fund.new-account-days:3}") int newAccountDays,
            @Value("${patrol.fund.new-account-amount:200}") int newAccountAmount,
            @Value("${patrol.fund.apply-limit-count:5}") int applyLimit) {
        this.ruleEngine = new RuleEngine(List.of(
                new Rule("fund-001", 10, true,
                        List.of(new Condition("sameBankCardAccounts", Condition.Op.GE, bankcardGang)),
                        com.risk.platform.model.Decision.BAN, "FUND_GANG_BANKCARD"),
                new Rule("fund-002", 20, true,
                        List.of(
                                new Condition("sameDeviceAccounts", Condition.Op.GE, deviceGang),
                                new Condition("grantAmount", Condition.Op.GE, deviceGangAmount)),
                        com.risk.platform.model.Decision.INTERCEPT, "FUND_GANG_DEVICE"),
                new Rule("fund-003", 30, true,
                        List.of(
                                new Condition("accountAgeDays", Condition.Op.LTE, newAccountDays),
                                new Condition("grantAmount", Condition.Op.GE, newAccountAmount)),
                        com.risk.platform.model.Decision.EXTRA_REVIEW, "NEW_ACCOUNT_HIGH_GRANT"),
                new Rule("fund-004", 40, true,
                        List.of(new Condition("applyCount24h", Condition.Op.GE, applyLimit)),
                        com.risk.platform.model.Decision.LIMIT, "HIGH_FREQUENCY_APPLY")),
                com.risk.platform.model.Decision.PASS);
    }

    /** 对补贴发放事件事实求值。 */
    public PatrolVerdict evaluate(String eventId, Map<String, Object> facts) {
        return ruleEngine.evaluate(eventId, facts);
    }
}
