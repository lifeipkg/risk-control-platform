package com.risk.platform.contentpatrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 内容巡查线服务（岗位真实性巡查）。
 *
 * 识别黑中介洗稿岗位：换地标、改薪资数字、同义改写后批量发布。
 * 核心：SimHash + 字符级 3-gram + 抽屉原理分段倒排索引，双通道判重。
 */
@SpringBootApplication
public class ContentPatrolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentPatrolApplication.class, args);
    }
}
