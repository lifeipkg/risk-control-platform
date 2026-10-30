package com.risk.platform.fundpatrol.web;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FundPatrolControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String url() {
        return "http://localhost:" + port + "/api/v1/patrol/check";
    }

    @Test
    @DisplayName("REST 全链路：正常补贴放行 -> 银行卡团伙封禁")
    void checkEndpointEndToEnd() {
        FundCheckRequest normal = new FundCheckRequest(
                "fund:ok", 8001L, 100, 30, 1, 1, 1);
        ResponseEntity<Map> r1 = rest.postForEntity(url(), normal, Map.class);
        assertEquals(HttpStatus.OK, r1.getStatusCode());
        assertEquals("PASS", r1.getBody().get("decision"));

        FundCheckRequest gang = new FundCheckRequest(
                "fund:bad", 8002L, 600, 2, 3, 3, 2);
        ResponseEntity<Map> r2 = rest.postForEntity(url(), gang, Map.class);
        assertEquals("BAN", r2.getBody().get("decision"));
        assertEquals("FUND_GANG_BANKCARD", r2.getBody().get("reason"));

        Map<String, Object> detail = (Map<String, Object>) r2.getBody().get("detail");
        assertEquals("fund", detail.get("domain"));
    }
}
