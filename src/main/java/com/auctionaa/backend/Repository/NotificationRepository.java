package com.auctionaa.backend.Repository;

import com.auctionaa.backend.Entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserId(String userId);

    @Query("{ $or: [ " +
            "{ '_id': { $regex: ?0, $options: 'i' } }, " +
            "{ 'notificationContent': { $regex: ?0, $options: 'i' } }, " +
            "{ 'title': { $regex: ?0, $options: 'i' } }, " +
            "{ 'link': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Notification> searchNotifications(String searchTerm);

    long countByNotificationStatus(int status);

    long countByNotificationStatusIn(Collection<Integer> statuses);
}