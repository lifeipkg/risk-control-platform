package com.risk.platform.accountpatrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 账号巡查线服务（接码注册/养号治理）。
 *
 * 治理目标：接码平台注册的虚拟号段账号、设备农场批量养号。
 * 核心：号段特征 + 设备/IP 聚集度 + 行为序列，规则引擎热更新驱动。
 */
@SpringBootApplication
public class AccountPatrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountPatrolApplication.class, args);
    }
}
