package com.risk.platform.contentpatrol.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 内容巡查检查请求（网关转发 payload 的强类型视图）。
 */
public record JobCheckRequest(
        String eventId,
        long jobId,
        @NotBlank String city,
        @NotBlank String trade,
        @NotBlank String title,
        @NotBlank String body,
        Long ts) {
}
