package com.medilink.notification.repository;

import com.medilink.notification.entity.Notification;
import com.medilink.notification.entity.NotificationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserId(Long userId);

    List<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status);

    List<Notification> findByStatus(NotificationStatus status);

    List<Notification> findByType(String type);

    long countByUserIdAndStatus(Long userId, NotificationStatus status);
}
