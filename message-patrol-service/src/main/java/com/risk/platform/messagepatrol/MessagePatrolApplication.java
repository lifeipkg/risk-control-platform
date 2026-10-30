package com.risk.platform.messagepatrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 消息巡查线服务（飞单/私单治理）。
 *
 * 识别黑中介诱导求职者绕开平台私下交易的消息：
 * AC 自动机多模式匹配 + 三步变体归一化 + 角色分级处置。
 */
@SpringBootApplication
public class MessagePatrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagePatrolApplication.class, args);
    }
}
