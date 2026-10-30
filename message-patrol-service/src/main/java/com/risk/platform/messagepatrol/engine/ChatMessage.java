package com.risk.platform.messagepatrol.engine;

/**
 * IM 聊天消息事件。
 *
 * @param id         消息 ID
 * @param senderId   发送方账号 ID
 * @param senderRole 发送方角色（角色分级处置依据）
 * @param content    消息原文
 * @param ts         发送时间戳
 */
public record ChatMessage(long id, long senderId, Role senderRole, String content, long ts) {
}
