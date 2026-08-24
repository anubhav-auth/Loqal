package com.loqal.communication.repository;

import com.loqal.communication.entity.Notification;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface NotificationRepository extends R2dbcRepository<Notification, UUID> {
}
