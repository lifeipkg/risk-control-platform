package com.risk.platform.fundpatrol.web;

import jakarta.validation.constraints.NotNull;

/**
 * 资金巡查检查请求（网关转发 payload 的强类型视图）。
 */
public record FundCheckRequest(
        String eventId,
        @NotNull Long userId,
        @NotNull Integer grantAmount,
        Integer accountAgeDays,
        Integer sameDeviceAccounts,
        Integer sameBankCardAccounts,
        Integer applyCount24h) {

    public String eventIdOr(String fallbackPrefix) {
        return eventId == null || eventId.isBlank() ? fallbackPrefix + ":" + userId : eventId;
    }
}
