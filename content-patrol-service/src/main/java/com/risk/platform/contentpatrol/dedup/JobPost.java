package com.risk.platform.contentpatrol.dedup;

/**
 * 岗位发布记录（内容巡查线的事件载体）。
 *
 * @param id     岗位 ID（幂等键）
 * @param city   城市（分片键：跨城市同模板属合法重复）
 * @param trade  工种（分片键）
 * @param title  标题（兜底通道输入）
 * @param body   正文（主通道输入）
 * @param ts     发布时间戳
 */
public record JobPost(long id, String city, String trade, String title, String body, long ts) {

    /** 业务语义分层分片键：城市 + 工种。 */
    public String shardKey() {
        return city + "|" + trade;
    }
}
