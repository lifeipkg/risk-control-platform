package com.risk.platform.accountpatrol.engine;

import com.risk.platform.model.Decision;
import com.risk.platform.model.PatrolVerdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountPatrolEngineTest {

    private final AccountPatrolEngine engine = new AccountPatrolEngine(3, 5, 10, 5, 20);

    private Map<String, Object> facts(boolean virtual, int sameIp, int sameDevice, int hour, int posts) {
        Map<String, Object> m = new HashMap<>();
        m.put("virtualSegment", virtual);
        m.put("sameIpRegister24h", sameIp);
        m.put("sameDeviceAccounts", sameDevice);
        m.put("registerHour", hour);
        m.put("postCount24h", posts);
        return m;
    }

    @Test
    @DisplayName("虚拟号段 + 设备聚集 -> 封禁（确定性设备农场）")
    void virtualSegmentDeviceFarmShouldBan() {
        PatrolVerdict v = engine.evaluate("acc:1", facts(true, 2, 3, 14, 5));
        assertEquals(Decision.BAN, v.decision());
        assertEquals("VIRTUAL_SEGMENT_DEVICE_FARM", v.reason());
        assertEquals(100, v.score());
    }

    @Test
    @DisplayName("同设备 5 账号（无虚拟号段）-> 封禁")
    void sameDeviceFarmShouldBan() {
        PatrolVerdict v = engine.evaluate("acc:2", facts(false, 2, 5, 14, 5));
        assertEquals(Decision.BAN, v.decision());
        assertEquals("SAME_DEVICE_FARM", v.reason());
    }

    @Test
    @DisplayName("同 IP 批量注册 -> 拦截")
    void sameIpBulkRegisterShouldIntercept() {
        PatrolVerdict v = engine.evaluate("acc:3", facts(false, 12, 1, 14, 5));
        assertEquals(Decision.INTERCEPT, v.decision());
        assertEquals("SAME_IP_BULK_REGISTER", v.reason());
    }

    @Test
    @DisplayName("虚拟号段单独命中 -> 加审（避免误杀真实工人）")
    void virtualSegmentOnlyShouldExtraReview() {
        PatrolVerdict v = engine.evaluate("acc:4", facts(true, 1, 1, 14, 5));
        assertEquals(Decision.EXTRA_REVIEW, v.decision());
        assertEquals("VIRTUAL_SEGMENT", v.reason());
    }

    @Test
    @DisplayName("夜间(0-6点)批量注册 -> 加审")
    void nightBulkRegisterShouldExtraReview() {
        PatrolVerdict v = engine.evaluate("acc:5", facts(false, 6, 1, 3, 5));
        assertEquals(Decision.EXTRA_REVIEW, v.decision());
        assertEquals("NIGHT_BULK_REGISTER", v.reason());
    }

    @Test
    @DisplayName("高频发帖 -> 限流；正常账号放行")
    void highFrequencyPostShouldLimitAndNormalPass() {
        PatrolVerdict limit = engine.evaluate("acc:6", facts(false, 1, 1, 14, 25));
        assertEquals(Decision.LIMIT, limit.decision());
        assertEquals("HIGH_FREQUENCY_POST", limit.reason());

        PatrolVerdict pass = engine.evaluate("acc:7", facts(false, 1, 1, 14, 3));
        assertEquals(Decision.PASS, pass.decision());
    }
}
