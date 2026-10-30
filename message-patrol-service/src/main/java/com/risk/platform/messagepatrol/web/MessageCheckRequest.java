package com.risk.platform.messagepatrol.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 消息巡查检查请求（网关转发 payload 的强类型视图）。
 */
public record MessageCheckRequest(
        String eventId,
        long messageId,
        long senderId,
        @NotBlank String senderRole,
        @NotBlank String content,
        Long ts) {
}
