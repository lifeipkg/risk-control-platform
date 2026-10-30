package com.risk.platform.accountpatrol.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 账号巡查检查请求（网关转发 payload 的强类型视图）。
 */
public record AccountCheckRequest(
        String eventId,
        @NotBlank String phone,
        boolean virtualSegment,
        Integer sameIpRegister24h,
        Integer sameDeviceAccounts,
        Integer registerHour,
        Integer postCount24h) {

    /** 事件 ID 缺省时由手机号生成。 */
    public String eventIdOr(String fallbackPrefix) {
        return eventId == null || eventId.isBlank() ? fallbackPrefix + ":" + phone : eventId;
    }
}
