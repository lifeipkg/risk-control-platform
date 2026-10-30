package com.risk.platform.messagepatrol.engine;

import com.risk.platform.messagepatrol.automaton.AhoCorasick;
import com.risk.platform.model.Decision;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagePatrolEngineTest {

    private MessagePatrolEngine newEngine() {
        return new MessagePatrolEngine(
                List.of("加微信", "微信号", "微信", "私聊", "转账", "定金", "服装费", "中介费"),
                List.of("官方微信", "平台客服"));
    }

    @Test
    @DisplayName("招聘侧直接发微信号应被拦截")
    void directWechatShouldIntercept() {
        PatrolResult r = newEngine().check(new ChatMessage(1, 1001L, Role.RECRUITER,
                "加我微信13800000000细聊待遇", 1000L));
        assertEquals(Decision.INTERCEPT, r.decision());
        assertEquals(MessagePatrolEngine.REASON_RECRUITER, r.reason());
    }

    @Test
    @DisplayName("emoji 变体 ➕V❤ 应被符号归一化后拦截")
    void emojiVariantShouldIntercept() {
        PatrolResult r = newEngine().check(new ChatMessage(2, 1001L, Role.RECRUITER,
                "➕V❤ 13800000000", 2000L));
        assertEquals(Decision.INTERCEPT, r.decision());
    }

    @Test
    @DisplayName("干扰符插点 加.薇.芯 应被剔除干扰符后拦截")
    void interferenceDotsShouldIntercept() {
        PatrolResult r = newEngine().check(new ChatMessage(3, 1001L, Role.RECRUITER,
                "加.薇.芯 看工地视频", 3000L));
        assertEquals(Decision.INTERCEPT, r.decision());
        assertTrue(r.hits().stream().anyMatch(h -> h.word().equals("微信")));
    }

    @Test
    @DisplayName("求职侧主动提微信只降级提示，不拦截")
    void workerSideShouldWarnNotIntercept() {
        PatrolResult r = newEngine().check(new ChatMessage(4, 2001L, Role.WORKER,
                "老板可以加微信吗？发定位方便些", 4000L));
        assertEquals(Decision.WARN, r.decision());
        assertEquals(MessagePatrolEngine.REASON_WORKER, r.reason());
    }

    @Test
    @DisplayName("白名单话术（官方微信）应放行")
    void whitelistShouldPass() {
        PatrolResult r = newEngine().check(new ChatMessage(5, 1002L, Role.RECRUITER,
                "工资问题请通过平台官方微信反馈", 5000L));
        assertEquals(Decision.PASS, r.decision());
        assertEquals(MessagePatrolEngine.REASON_WHITELIST, r.reason());
    }

    @Test
    @DisplayName("正常业务消息放行且无命中")
    void normalMessageShouldPass() {
        PatrolResult r = newEngine().check(new ChatMessage(6, 1002L, Role.RECRUITER,
                "工地急招钢筋工30名包吃住日结260", 6000L));
        assertEquals(Decision.PASS, r.decision());
        assertTrue(r.hits().isEmpty());
    }

    @Test
    @DisplayName("AC 自动机：多模式一次扫描全部命中，且无误报")
    void automatonMultiPattern() {
        AhoCorasick ac = new AhoCorasick();
        ac.add("微信");
        ac.add("加微信");
        ac.add("私聊");
        ac.build();

        List<AhoCorasick.Match> hits = ac.find("请加微信详聊，也可以私聊");
        var words = hits.stream().map(AhoCorasick.Match::word).toList();
        assertTrue(words.contains("微信"));
        assertTrue(words.contains("加微信"));
        assertTrue(words.contains("私聊"));

        assertTrue(ac.find("今天工地开工大吉").isEmpty());
    }

    @Test
    @DisplayName("AC 自动机：fail 链回退正确（重叠前缀模式）")
    void automatonFailLink() {
        AhoCorasick ac = new AhoCorasick();
        ac.add("aa");
        ac.add("aaa");
        ac.add("ababa");
        ac.build();

        List<AhoCorasick.Match> hits = ac.find("aaababa");
        var words = hits.stream().map(AhoCorasick.Match::word).distinct().toList();
        assertTrue(words.contains("aa"));
        assertTrue(words.contains("aaa"));
        assertTrue(words.contains("ababa"));
    }

    @Test
    @DisplayName("性能冒烟：万级关键词 + 千条消息亚秒级完成")
    void performanceSmoke() {
        // 构造 5000 个伪关键词
        List<String> keywords = new ArrayList<>();
        Random rnd = new Random(1);
        for (int i = 0; i < 5000; i++) {
            StringBuilder sb = new StringBuilder("词");
            for (int j = 0; j < 3; j++) {
                sb.append((char) ('a' + rnd.nextInt(26)));
            }
            keywords.add(sb.toString());
        }
        keywords.add("微信");
        MessagePatrolEngine engine = new MessagePatrolEngine(keywords, List.of());

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            engine.check(new ChatMessage(i, 1L, Role.RECRUITER,
                    "第" + i + "条正常消息，工地急招钢筋工包吃住，请联系工头", i));
        }
        long costMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(costMs < 2000, "1000 条消息耗时 " + costMs + "ms，超出预期");
    }
}
