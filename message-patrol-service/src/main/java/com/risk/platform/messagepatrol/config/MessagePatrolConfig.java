package com.risk.platform.messagepatrol.config;

import com.risk.platform.messagepatrol.engine.MessagePatrolEngine;
import com.risk.platform.messagepatrol.text.VariantNormalizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 消息巡查引擎装配：词库/白名单来自配置（生产为 MySQL + 双缓冲热更新）。
 */
@Configuration
@ConfigurationProperties(prefix = "patrol.message")
public class MessagePatrolConfig {

    /** 飞单/私单诱导关键词库（标准词空间）。 */
    private List<String> keywords = List.of(
            "加微信", "微信号", "微信", "私聊", "转账", "定金", "服装费", "中介费");

    /** 白名单：官方渠道话术，命中降级放行。 */
    private List<String> whitelist = List.of("官方微信", "平台客服");

    @Bean
    public VariantNormalizer variantNormalizer() {
        return new VariantNormalizer();
    }

    @Bean
    public MessagePatrolEngine messagePatrolEngine(VariantNormalizer normalizer) {
        return new MessagePatrolEngine(keywords, whitelist, normalizer);
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
