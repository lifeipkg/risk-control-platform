package com.risk.platform.contentpatrol.web;

import com.risk.platform.contentpatrol.dedup.DedupEngine;
import com.risk.platform.contentpatrol.dedup.JobPost;
import com.risk.platform.contentpatrol.dedup.Verdict;
import com.risk.platform.model.Decision;
import com.risk.platform.model.PatrolVerdict;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 内容巡查统一入口：POST /api/v1/patrol/check
 *
 * 判重决策映射（业务不对称：漏放代价 << 误杀代价，向 precision 倾斜）：
 *   - 判重命中         -> INTERCEPT（重复洗稿岗位直接拦截）
 *   - 未命中           -> PASS
 */
@RestController
@RequestMapping("/api/v1/patrol")
public class ContentPatrolController {

    public static final String DOMAIN = "content";

    private final DedupEngine dedupEngine;

    public ContentPatrolController(DedupEngine dedupEngine) {
        this.dedupEngine = dedupEngine;
    }

    @PostMapping("/check")
    public ResponseEntity<PatrolVerdict> check(@Valid @RequestBody JobCheckRequest req) {
        JobPost job = new JobPost(req.jobId(), req.city(), req.trade(),
                req.title(), req.body(), req.ts() == null ? System.currentTimeMillis() : req.ts());
        Verdict v = dedupEngine.check(job);

        Decision decision = v.duplicate() ? Decision.INTERCEPT : Decision.PASS;
        PatrolVerdict verdict = new PatrolVerdict(
                req.eventId() == null ? "job:" + req.jobId() : req.eventId(),
                decision,
                decision.baseScore(),
                v.reason(),
                Map.of(
                        "domain", DOMAIN,
                        "duplicate", v.duplicate(),
                        "matchedJobId", v.matchedId(),
                        "hammingDistance", v.hammingDistance(),
                        "titleEditDistance", v.titleEditDistance() == null ? -1 : v.titleEditDistance(),
                        "shard", job.shardKey()));
        return ResponseEntity.ok(verdict);
    }

    /** 分片观测（桶倾斜监控的最简形态）。 */
    @GetMapping("/shard")
    public Map<String, Object> shard(@RequestParam String city, @RequestParam String trade) {
        return Map.of("shard", city + "|" + trade, "size", dedupEngine.shardSize(city, trade));
    }
}
