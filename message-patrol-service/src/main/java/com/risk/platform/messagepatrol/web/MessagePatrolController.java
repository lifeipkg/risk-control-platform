package com.risk.platform.messagepatrol.web;

import com.risk.platform.messagepatrol.engine.ChatMessage;
import com.risk.platform.messagepatrol.engine.MessagePatrolEngine;
import com.risk.platform.messagepatrol.engine.PatrolResult;
import com.risk.platform.messagepatrol.engine.Role;
import com.risk.platform.model.Decision;
import com.risk.platform.model.PatrolVerdict;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 消息巡查统一入口：POST /api/v1/patrol/check
 *
 * 角色分级处置（误杀治理的核心）：
 *   - 招聘侧命中 -> INTERCEPT（飞单诱导主体）
 *   - 求职侧命中 -> WARN（被动方，仅提示不拦截）
 *   - 白名单命中 -> PASS（官方话术降级）
 */
@RestController
@RequestMapping("/api/v1/patrol")
public class MessagePatrolController {

    public static final String DOMAIN = "message";

    private final MessagePatrolEngine engine;

    public MessagePatrolController(MessagePatrolEngine engine) {
        this.engine = engine;
    }

    @PostMapping("/check")
    public ResponseEntity<PatrolVerdict> check(@Valid @RequestBody MessageCheckRequest req) {
        Role role;
        try {
            role = Role.valueOf(req.senderRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        ChatMessage msg = new ChatMessage(req.messageId(), req.senderId(), role,
                req.content(), req.ts() == null ? System.currentTimeMillis() : req.ts());
        PatrolResult r = engine.check(msg);

        List<String> hitWords = r.hits().stream().map(h -> h.word()).distinct().toList();
        PatrolVerdict verdict = new PatrolVerdict(
                req.eventId() == null ? "msg:" + req.messageId() : req.eventId(),
                r.decision(),
                r.decision().baseScore(),
                r.reason(),
                Map.of(
                        "domain", DOMAIN,
                        "hitWords", hitWords,
                        "senderRole", role.name()));
        return ResponseEntity.status(HttpStatus.OK).body(verdict);
    }
}
