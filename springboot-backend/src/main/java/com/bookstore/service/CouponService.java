package com.bookstore.service;

import com.bookstore.model.Coupon;
import com.bookstore.repository.CouponRepository;
import com.bookstore.repository.CouponUsageRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CouponService {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponUsageRepository couponUsageRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public List<Coupon> getAllActiveCoupons() {
        return couponRepository
                .findByIsActiveTrueAndExpirationDateGreaterThanEqualOrExpirationDateIsNullOrderByDiscountValueDesc(
                        LocalDate.now());
    }

    public Optional<Coupon> getCouponById(Long id) {
        return couponRepository.findById(id);
    }

    public Optional<Coupon> getCouponByCode(String code) {
        return couponRepository.findByCodeAndIsActiveTrue(code).filter(
                c -> (c.getExpirationDate() == null || c.getExpirationDate().isAfter(LocalDate.now().minusDays(1))) &&
                        (c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit()));
    }

    public Optional<Coupon> getCouponByCodeForUser(String code, Long userId) {
        Optional<Coupon> base = getCouponByCode(code);
        if (base.isEmpty()) return Optional.empty();
        Coupon c = base.get();
        if (userId == null) return base;

        int perUserLimit = c.getUserUsageLimit() != null ? c.getUserUsageLimit() : 1;
        if (perUserLimit <= 0) perUserLimit = 1;

        int usedByUser = couponUsageRepository
                .findByCouponIdAndUserId(c.getId(), userId)
                .map(u -> u.getUsedCount() != null ? u.getUsedCount() : 0)
                .orElse(0);

        if (usedByUser >= perUserLimit) return Optional.empty();
        return base;
    }

    public void incrementUsage(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId).orElseThrow(() -> new RuntimeException("Coupon not found"));
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    /**
     * Record coupon usage for a user (also increments global usedCount).
     * Call this when an order is successfully created/accepted.
     */
    public void recordUsage(String couponCode, Long userId) {
        if (couponCode == null || couponCode.isBlank() || userId == null) return;

        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode.trim())
                .orElse(null);
        if (coupon == null) return;

        // global usage
        coupon.setUsedCount((coupon.getUsedCount() != null ? coupon.getUsedCount() : 0) + 1);
        couponRepository.save(coupon);

        // per-user usage
        com.bookstore.model.CouponUsage usage = couponUsageRepository
                .findByCouponIdAndUserId(coupon.getId(), userId)
                .orElseGet(() -> {
                    com.bookstore.model.CouponUsage u = new com.bookstore.model.CouponUsage();
                    u.setCoupon(coupon);
                    u.setUser(userRepository.getReferenceById(userId));
                    u.setUsedCount(0);
                    return u;
                });
        usage.setUsedCount((usage.getUsedCount() != null ? usage.getUsedCount() : 0) + 1);
        couponUsageRepository.save(usage);
    }

    public Coupon saveCoupon(Coupon coupon) {
        if (coupon.getMinItemQuantity() != null && coupon.getMinItemQuantity() <= 0) {
            coupon.setMinItemQuantity(null);
        }
        if (coupon.getId() == null) {
            if (coupon.getUsedCount() == null) {
                coupon.setUsedCount(0);
            }
            return couponRepository.save(coupon);
        }
        return couponRepository.findById(coupon.getId()).map(existing -> {
            existing.setCode(coupon.getCode());
            existing.setDescription(coupon.getDescription());
            existing.setType(coupon.getType());
            existing.setDiscountType(coupon.getDiscountType());
            existing.setDiscountValue(coupon.getDiscountValue());
            if (coupon.getMaxDiscountAmount() != null) {
                existing.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
            }
            existing.setMinOrderValue(coupon.getMinOrderValue());
            existing.setExpirationDate(coupon.getExpirationDate());
            existing.setUsageLimit(coupon.getUsageLimit());
            existing.setUserUsageLimit(coupon.getUserUsageLimit());
            existing.setMinItemQuantity(coupon.getMinItemQuantity());
            existing.setIsActive(coupon.getIsActive());
            return couponRepository.save(existing);
        }).orElseGet(() -> couponRepository.save(coupon));
    }

    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }
}
