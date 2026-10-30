package com.risk.platform.fundpatrol.engine;

import com.risk.platform.model.Decision;
import com.risk.platform.model.PatrolVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FundPatrolEngineTest {

    private final FundPatrolEngine engine = new FundPatrolEngine(3, 3, 500, 3, 200, 5);

    private Map<String, Object> facts(int amount, int ageDays, int device, int bankcard, int applies) {
        Map<String, Object> m = new HashMap<>();
        m.put("grantAmount", amount);
        m.put("accountAgeDays", ageDays);
        m.put("sameDeviceAccounts", device);
        m.put("sameBankCardAccounts", bankcard);
        m.put("applyCount24h", applies);
        return m;
    }

    @Test
    @DisplayName("同银行卡 3 账号 -> 封禁（团伙最强信号）")
    void bankcardGangShouldBan() {
        PatrolVerdict v = engine.evaluate("fund:1", facts(300, 30, 1, 3, 1));
        assertEquals(Decision.BAN, v.decision());
        assertEquals("FUND_GANG_BANKCARD", v.reason());
        assertEquals(100, v.score());
    }

    @Test
    @DisplayName("同设备聚集 + 大额 -> 拦截")
    void deviceGangLargeAmountShouldIntercept() {
        PatrolVerdict v = engine.evaluate("fund:2", facts(600, 30, 3, 1, 1));
        assertEquals(Decision.INTERCEPT, v.decision());
        assertEquals("FUND_GANG_DEVICE", v.reason());
    }

    @Test
    @DisplayName("新号高额 -> 加审（放款前分层）")
    void newAccountHighGrantShouldExtraReview() {
        PatrolVerdict v = engine.evaluate("fund:3", facts(300, 1, 1, 1, 1));
        assertEquals(Decision.EXTRA_REVIEW, v.decision());
        assertEquals("NEW_ACCOUNT_HIGH_GRANT", v.reason());
    }

    @Test
    @DisplayName("高频申请 -> 限流；正常老号小额放行")
    void highFrequencyApplyAndNormalPass() {
        PatrolVerdict limit = engine.evaluate("fund:4", facts(100, 30, 1, 1, 6));
        assertEquals(Decision.LIMIT, limit.decision());
        assertEquals("HIGH_FREQUENCY_APPLY", limit.reason());

        PatrolVerdict pass = engine.evaluate("fund:5", facts(100, 30, 1, 1, 1));
        assertEquals(Decision.PASS, pass.decision());
    }
}
