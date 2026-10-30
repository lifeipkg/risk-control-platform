package com.risk.platform.accountpatrol.web;

import com.risk.platform.accountpatrol.engine.AccountPatrolEngine;
import com.risk.platform.model.PatrolVerdict;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 账号巡查统一入口：POST /api/v1/patrol/check
 */
@RestController
@RequestMapping("/api/v1/patrol")
public class AccountPatrolController {

    private final AccountPatrolEngine engine;

    public AccountPatrolController(AccountPatrolEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/check")
    public PatrolVerdict check(@Valid @RequestBody AccountCheckRequest req) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("virtualSegment", req.virtualSegment());
        facts.put("sameIpRegister24h", req.sameIpRegister24h() == null ? 1 : req.sameIpRegister24h());
        facts.put("sameDeviceAccounts", req.sameDeviceAccounts() == null ? 1 : req.sameDeviceAccounts());
        facts.put("registerHour", req.registerHour() == null ? 12 : req.registerHour());
        facts.put("postCount24h", req.postCount24h() == null ? 0 : req.postCount24h());

        PatrolVerdict verdict = engine.evaluate(req.eventIdOr("acc"), facts);
        return new PatrolVerdict(verdict.eventId(), verdict.decision(), verdict.score(),
                verdict.reason(),
                withDomain(verdict.detail()));
    }

    private Map<String, Object> withDomain(Map<String, Object> detail) {
        Map<String, Object> out = new HashMap<>(detail);
        out.put("domain", AccountPatrolEngine.DOMAIN);
        return out;
    }
}
