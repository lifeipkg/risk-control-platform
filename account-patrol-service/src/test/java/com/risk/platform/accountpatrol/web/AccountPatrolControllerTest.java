package com.risk.platform.accountpatrol.web;

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
class AccountPatrolControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String url() {
        return "http://localhost:" + port + "/api/v1/patrol/check";
    }

    @Test
    @DisplayName("REST 全链路：正常注册放行 -> 虚拟号段设备农场封禁")
    void checkEndpointEndToEnd() {
        AccountCheckRequest normal = new AccountCheckRequest(
                "acc:ok", "13800001234", false, 1, 1, 14, 3);
        ResponseEntity<Map> r1 = rest.postForEntity(url(), normal, Map.class);
        assertEquals(HttpStatus.OK, r1.getStatusCode());
        assertEquals("PASS", r1.getBody().get("decision"));

        AccountCheckRequest farm = new AccountCheckRequest(
                "acc:bad", "16500001234", true, 2, 4, 3, 30);
        ResponseEntity<Map> r2 = rest.postForEntity(url(), farm, Map.class);
        assertEquals("BAN", r2.getBody().get("decision"));
        assertEquals("VIRTUAL_SEGMENT_DEVICE_FARM", r2.getBody().get("reason"));

        Map<String, Object> detail = (Map<String, Object>) r2.getBody().get("detail");
        assertEquals("account", detail.get("domain"));
    }
}
