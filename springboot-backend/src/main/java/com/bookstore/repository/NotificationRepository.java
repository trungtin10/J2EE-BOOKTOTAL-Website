package com.bookstore.repository;

import com.bookstore.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndTitleInOrderByCreatedAtDesc(Long userId, Collection<String> titles);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndIsReadFalseAndTitleIn(Long userId, Collection<String> titles);
}
