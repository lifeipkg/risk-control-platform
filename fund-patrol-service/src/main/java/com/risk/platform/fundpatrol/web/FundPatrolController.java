package com.risk.platform.fundpatrol.web;

import com.risk.platform.fundpatrol.engine.FundPatrolEngine;
import com.risk.platform.model.PatrolVerdict;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 资金巡查统一入口：POST /api/v1/patrol/check
 */
@RestController
@RequestMapping("/api/v1/patrol")
public class FundPatrolController {

    private final FundPatrolEngine engine;

    public FundPatrolController(FundPatrolEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/check")
    public PatrolVerdict check(@Valid @RequestBody FundCheckRequest req) {
        Map<String, Object> facts = new HashMap<>();
        facts.put("grantAmount", req.grantAmount());
        facts.put("accountAgeDays", req.accountAgeDays() == null ? 30 : req.accountAgeDays());
        facts.put("sameDeviceAccounts", req.sameDeviceAccounts() == null ? 1 : req.sameDeviceAccounts());
        facts.put("sameBankCardAccounts", req.sameBankCardAccounts() == null ? 1 : req.sameBankCardAccounts());
        facts.put("applyCount24h", req.applyCount24h() == null ? 1 : req.applyCount24h());

        PatrolVerdict verdict = engine.evaluate(req.eventIdOr("fund"), facts);
        return new PatrolVerdict(verdict.eventId(), verdict.decision(), verdict.score(),
                verdict.reason(),
                withDomain(verdict.detail()));
    }

    private Map<String, Object> withDomain(Map<String, Object> detail) {
        Map<String, Object> out = new HashMap<>(detail);
        out.put("domain", FundPatrolEngine.DOMAIN);
        return out;
    }
}
