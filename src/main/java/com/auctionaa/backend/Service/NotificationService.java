package com.auctionaa.backend.Service;

import com.auctionaa.backend.Entity.Notification;
import com.auctionaa.backend.Entity.User;
import com.auctionaa.backend.Repository.NotificationRepository;
import com.auctionaa.backend.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, NotificationRepository repo) {
        this.notificationRepository = notificationRepository;
        this.repo = repo;
    }

    public List<Notification> getNotificationBy_OwnerId(String email) {
        User user = userRepository.findById(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByUserId(user.getId());

    }


    private final NotificationRepository repo;
    // (tuỳ chọn) đẩy realtime
    @Autowired(required = false) private org.springframework.messaging.simp.SimpMessagingTemplate broker;

    public Notification addNotification(Notification n){
        if (n.getId() == null) n.generateId();
        n.setCreatedAt(LocalDateTime.now());
        n.setNotificationTime(LocalDateTime.now());
        Notification saved = repo.save(n);
        // push realtime: /user/{userId}/queue/notifications
        if (broker != null) broker.convertAndSendToUser(n.getUserId(), "/queue/notifications", saved);
        return saved;
    }
    public List<Notification> addAll(List<Notification> items){
        items.forEach(n -> { if(n.getId()==null) n.generateId(); n.setCreatedAt(LocalDateTime.now()); n.setNotificationTime(LocalDateTime.now()); });
        List<Notification> saved = repo.saveAll(items);
        if (broker != null) saved.forEach(n -> broker.convertAndSendToUser(n.getUserId(), "/queue/notifications", n));
        return saved;
    }
}
