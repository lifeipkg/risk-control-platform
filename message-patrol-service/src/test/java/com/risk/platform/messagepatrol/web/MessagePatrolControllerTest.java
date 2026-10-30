package com.risk.platform.messagepatrol.web;

import com.risk.platform.model.Decision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessagePatrolControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String url() {
        return "http://localhost:" + port + "/api/v1/patrol/check";
    }

    @Test
    @DisplayName("REST 全链路：招聘侧变体话术拦截 / 求职侧提示 / 非法角色 400")
    void checkEndpointEndToEnd() {
        // 招聘侧 emoji 变体 -> INTERCEPT
        MessageCheckRequest recruiter = new MessageCheckRequest(
                "msg:1", 1, 1001L, "RECRUITER", "➕V❤ 13800000000", 1000L);
        ResponseEntity<Map> r1 = rest.postForEntity(url(), recruiter, Map.class);
        assertEquals(HttpStatus.OK, r1.getStatusCode());
        assertEquals("INTERCEPT", r1.getBody().get("decision"));
        assertEquals("KEYWORD_HIT_RECRUITER", r1.getBody().get("reason"));

        // 求职侧 -> WARN
        MessageCheckRequest worker = new MessageCheckRequest(
                "msg:2", 2, 2001L, "WORKER", "老板可以加微信吗？发定位方便些", 2000L);
        ResponseEntity<Map> r2 = rest.postForEntity(url(), worker, Map.class);
        assertEquals("WARN", r2.getBody().get("decision"));

        // 非法角色 -> 400
        MessageCheckRequest bad = new MessageCheckRequest(
                "msg:3", 3, 3001L, "ROBOT", "hello", 3000L);
        ResponseEntity<Map> r3 = rest.postForEntity(url(), bad, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, r3.getStatusCode());
    }

    @Test
    @DisplayName("detail 携带命中词与角色（供网关处置与人审回流）")
    void verdictDetailCarriesHits() {
        MessageCheckRequest req = new MessageCheckRequest(
                "msg:4", 4, 1001L, "RECRUITER", "加.薇.芯 看工地视频", 4000L);
        ResponseEntity<Map> r = rest.postForEntity(url(), req, Map.class);
        Map<String, Object> detail = (Map<String, Object>) r.getBody().get("detail");
        assertEquals(Decision.INTERCEPT.name(), r.getBody().get("decision"));
        assertTrue(((java.util.List<?>) detail.get("hitWords")).contains("微信"));
        assertEquals("RECRUITER", detail.get("senderRole"));
    }
}
