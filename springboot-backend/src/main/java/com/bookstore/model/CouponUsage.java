package com.bookstore.model;

import jakarta.persistence.*;

@Entity
@Table(name = "coupon_usages",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupon_user", columnNames = {"coupon_id", "user_id"}))
public class CouponUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    public CouponUsage() {}

    public CouponUsage(Coupon coupon, User user) {
        this.coupon = coupon;
        this.user = user;
        this.usedCount = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Coupon getCoupon() { return coupon; }
    public void setCoupon(Coupon coupon) { this.coupon = coupon; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
}

