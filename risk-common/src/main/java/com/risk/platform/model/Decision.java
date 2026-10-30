package com.risk.platform.model;

/**
 * 统一风控决策：四条巡查线共用，处置层按决策分级执行。
 */
public enum Decision {
    /** 放行 */
    PASS,
    /** 提示（不拦截，如求职侧命中诱导词仅做风险提示） */
    WARN,
    /** 限流（如当日触达次数受限） */
    LIMIT,
    /** 加审（转人工/二次审核后才可分发） */
    EXTRA_REVIEW,
    /** 拦截 */
    INTERCEPT,
    /** 封禁（针对账号级处置） */
    BAN;

    /** 风险分映射（决策 -> 基准分）。 */
    public int baseScore() {
        return switch (this) {
            case PASS -> 0;
            case WARN -> 20;
            case LIMIT -> 40;
            case EXTRA_REVIEW -> 60;
            case INTERCEPT -> 80;
            case BAN -> 100;
        };
    }
}
