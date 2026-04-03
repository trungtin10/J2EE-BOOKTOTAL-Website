package com.bookstore.repository;

import com.bookstore.model.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    Optional<CouponUsage> findByCouponIdAndUserId(Long couponId, Long userId);
}

