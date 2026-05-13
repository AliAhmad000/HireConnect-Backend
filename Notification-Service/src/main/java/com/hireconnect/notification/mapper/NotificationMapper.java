package com.hireconnect.notification.mapper;

import com.hireconnect.notification.dto.response.NotificationResponse;
import com.hireconnect.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toResponse(Notification notification);
}
