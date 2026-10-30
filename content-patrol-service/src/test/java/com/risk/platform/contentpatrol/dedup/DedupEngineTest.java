package com.risk.platform.contentpatrol.dedup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DedupEngineTest {

    private static final String BODY_A =
            "急招钢筋工30名，要求男，55岁以下，包吃包住，日结260，工资月发也可以，"
                    + "电话联系工头王师傅13900000000，工地在江北区观音桥附近";

    private JobPost job(long id, String city, String trade, String title, String body, long ts) {
        return new JobPost(id, city, trade, title, body, ts);
    }

    @Test
    @DisplayName("洗稿变体（改薪资数字/加标点空格）应被 SimHash 主通道判重")
    void washedVariantShouldBeRejected() {
        DedupEngine engine = new DedupEngine(3, 4);
        engine.check(job(1, "重庆", "钢筋工", "急招钢筋工30名包吃住", BODY_A, 1000L));

        String washed = "急招钢筋工30名，要求男，55岁以下，包吃包住，日结280，工资月发也可以，"
                + "电话联系工头王师傅 13900000000，工地在江北区观音桥附近！！";
        Verdict v = engine.check(job(2, "重庆", "钢筋工", "招钢筋工30名 包吃住", washed, 2000L));

        assertTrue(v.duplicate(), "期望判重, 实际: " + v);
        assertEquals(DedupEngine.REASON_SIMHASH_BODY, v.reason());
        assertEquals(1L, v.matchedId());
    }

    @Test
    @DisplayName("完全不同的岗位不误杀")
    void differentJobShouldPass() {
        DedupEngine engine = new DedupEngine(3, 4);
        engine.check(job(1, "重庆", "钢筋工", "急招钢筋工30名包吃住", BODY_A, 1000L));

        JobPost other = job(2, "重庆", "电工", "商场招持证电工",
                "招聘持证电工两名，负责商场配电房日常巡检维护，需低压电工证，"
                        + "两班倒月薪五千左右缴纳五险，联系人李经理13611112222", 3000L);
        Verdict v = engine.check(other);

        assertFalse(v.duplicate(), "不应误杀, 实际: " + v);
        assertEquals(DedupEngine.REASON_OK, v.reason());
    }

    @Test
    @DisplayName("跨城市同模板属合法重复，分片隔离后应放行")
    void crossCitySameTemplateShouldPass() {
        DedupEngine engine = new DedupEngine(3, 4);
        engine.check(job(1, "重庆", "钢筋工", "急招钢筋工30名包吃住", BODY_A, 1000L));

        Verdict v = engine.check(job(2, "成都", "钢筋工", "急招钢筋工30名包吃住", BODY_A, 2000L));

        assertFalse(v.duplicate(), "跨城市不应误杀, 实际: " + v);
        assertEquals(1, engine.shardSize("成都", "钢筋工"));
    }

    @Test
    @DisplayName("标题近似、正文偷换的钓鱼岗位应被标题兜底通道拦截")
    void baitJobShouldBeCaughtByTitleChannel() {
        DedupEngine engine = new DedupEngine(3, 4);
        engine.check(job(1, "重庆", "钢筋工", "急招钢筋工30名包吃住", BODY_A, 1000L));

        JobPost bait = job(2, "重庆", "钢筋工", "急招钢筋工30名包吃住！",
                "本公司另有大量普工杂工小工岗位急招，待遇优厚具体面议，当天安排上岗，"
                        + "报名先交服装费两百元，欢迎咨询联系电话13455556666李经理", 4000L);
        Verdict v = engine.check(bait);

        assertTrue(v.duplicate(), "标题通道应命中, 实际: " + v);
        assertEquals(DedupEngine.REASON_TITLE_EDIT, v.reason());
    }

    @Test
    @DisplayName("抽屉原理性质：汉明距离 <= 3 的两指纹必有一段相同，候选集不漏")
    void drawerPrincipleProperty() {
        Random rnd = new Random(42);
        for (int t = 0; t < 1000; t++) {
            long fp = rnd.nextLong();
            // 随机翻转 k 个 bit，k ∈ [0,3]
            int k = rnd.nextInt(4);
            long flipped = fp;
            for (int i = 0; i < k; i++) {
                flipped ^= (1L << rnd.nextInt(64));
            }
            assertTrue(SegmentIndex.shareSegment(fp, flipped),
                    "距离 " + k + " 的指纹必须共享至少一个段");
        }
    }

    @Test
    @DisplayName("分段索引：翻转 <= 3 bit 的指纹必能被候选集召回")
    void segmentIndexShouldRecallFlippedFingerprint() {
        Random rnd = new Random(42);
        for (int t = 0; t < 100; t++) {
            SegmentIndex index = new SegmentIndex();
            long stored = rnd.nextLong();
            index.add(1L, stored);

            long query = stored ^ (1L << rnd.nextInt(64)) ^ (1L << rnd.nextInt(64));
            List<SegmentIndex.Candidate> candidates = index.candidates(query);

            assertTrue(candidates.stream().anyMatch(c -> c.id() == 1L),
                    "距离 <= 2 的指纹必须在候选集中");
        }
    }
}
