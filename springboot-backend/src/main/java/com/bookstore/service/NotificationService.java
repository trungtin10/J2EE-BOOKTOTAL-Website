package com.bookstore.service;

import com.bookstore.model.Notification;
import com.bookstore.model.User;
import com.bookstore.repository.NotificationRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class NotificationService {

    /** Chỉ các tiêu đề hiển thị cho khách (đặt hàng, thanh toán, giao hàng). */
    public static final Set<String> CUSTOMER_VISIBLE_TITLES = Set.of(
            "Đặt hàng thành công",
            "Thanh toán thành công",
            "Đang giao hàng");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndTitleInOrderByCreatedAtDesc(userId, CUSTOMER_VISIBLE_TITLES);
    }

    public List<Notification> getRecentNotifications(Long userId, int limit) {
        List<Notification> all = notificationRepository.findByUserIdAndTitleInOrderByCreatedAtDesc(userId, CUSTOMER_VISIBLE_TITLES);
        return all.size() <= limit ? all : all.subList(0, limit);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndTitleIn(userId, CUSTOMER_VISIBLE_TITLES);
    }

    public void createNotification(Long userId, String title, String message) {
        createNotification(userId, title, message, "info");
    }

    public void createNotification(Long userId, String title, String message, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(Long userId) {
        notificationRepository.findByUserIdAndTitleInOrderByCreatedAtDesc(userId, CUSTOMER_VISIBLE_TITLES)
                .stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .forEach(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
    }
}
