package com.hireconnect.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UnreadCountResponse {
    private Long userId;
    private long unreadCount;
}
