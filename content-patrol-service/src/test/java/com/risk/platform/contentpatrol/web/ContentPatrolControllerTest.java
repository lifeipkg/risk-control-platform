package com.risk.platform.contentpatrol.web;

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
class ContentPatrolControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private static final String BODY_A =
            "急招钢筋工30名，要求男，55岁以下，包吃包住，日结260，工资月发也可以，"
                    + "电话联系工头王师傅13900000000，工地在江北区观音桥附近";

    private String url() {
        return "http://localhost:" + port + "/api/v1/patrol/check";
    }

    @Test
    @DisplayName("REST 全链路：首发布放行 -> 洗稿变体拦截")
    void checkEndpointEndToEnd() {
        JobCheckRequest first = new JobCheckRequest("job:1", 1, "重庆", "钢筋工",
                "急招钢筋工30名包吃住", BODY_A, 1000L);
        ResponseEntity<Map> r1 = rest.postForEntity(url(), first, Map.class);
        assertEquals(HttpStatus.OK, r1.getStatusCode());
        assertEquals("PASS", r1.getBody().get("decision"));

        JobCheckRequest washed = new JobCheckRequest("job:2", 2, "重庆", "钢筋工",
                "招钢筋工30名 包吃住",
                "急招钢筋工30名，要求男，55岁以下，包吃包住，日结280，工资月发也可以，"
                        + "电话联系工头王师傅 13900000000，工地在江北区观音桥附近！！", 2000L);
        ResponseEntity<Map> r2 = rest.postForEntity(url(), washed, Map.class);
        assertEquals("INTERCEPT", r2.getBody().get("decision"));
        assertEquals("SIMHASH_BODY", r2.getBody().get("reason"));

        Map<String, Object> detail = (Map<String, Object>) r2.getBody().get("detail");
        assertEquals(Boolean.TRUE, detail.get("duplicate"));
        assertEquals(1, ((Number) detail.get("matchedJobId")).intValue());
        assertEquals("重庆|钢筋工", detail.get("shard"));
    }
}
