package com.risk.platform.fundpatrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 资金巡查线服务（补贴薅取治理）。
 *
 * 治理目标：黑产批量注册小号薅取新人补贴、招聘补贴。
 * 核心：领补贴前后分层（账号年龄）+ 设备/银行卡聚集度（团伙信号），规则引擎热更新驱动。
 */
@SpringBootApplication
public class FundPatrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundPatrolApplication.class, args);
    }
}
