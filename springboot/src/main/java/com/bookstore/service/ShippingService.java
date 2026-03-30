package com.bookstore.service;

import com.bookstore.model.Province;
import com.bookstore.repository.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShippingService {

    /** Miễn phí vận chuyển từ mức này (đồng bộ với chính sách trên site). */
    private static final double FREE_SHIPPING_THRESHOLD = 500_000.0;

    /** Phí tối đa mọi khu vực (đ). */
    public static final double MAX_SHIPPING_FEE = 30_000.0;

    private static final double FEE_BAC = 30_000.0;
    private static final double FEE_TRUNG = 24_000.0;
    private static final double FEE_NAM = 18_000.0;
    private static final double FEE_UNKNOWN = 22_000.0;

    @Autowired
    private ProvinceRepository provinceRepository;

    /**
     * Phí ship cố định theo miền (Bắc / Trung / Nam), không cộng thêm km — luôn ≤ 30.000đ.
     */
    public double calculateShippingFee(String provinceCode, double subtotal) {
        if (subtotal >= FREE_SHIPPING_THRESHOLD) {
            return 0.0;
        }
        if (provinceCode == null || provinceCode.isBlank()) {
            return Math.min(FEE_UNKNOWN, MAX_SHIPPING_FEE);
        }

        Optional<Province> provinceOpt = provinceRepository.findById(provinceCode.trim());
        if (provinceOpt.isEmpty()) {
            return Math.min(FEE_UNKNOWN, MAX_SHIPPING_FEE);
        }

        String region = provinceOpt.get().getRegion();
        double fee = feeForRegion(region);
        return Math.min(fee, MAX_SHIPPING_FEE);
    }

    private double feeForRegion(String region) {
        if (region == null || region.isBlank()) {
            return FEE_UNKNOWN;
        }
        return switch (region) {
            case "Bắc" -> FEE_BAC;
            case "Trung" -> FEE_TRUNG;
            case "Nam" -> FEE_NAM;
            default -> FEE_UNKNOWN;
        };
    }

    /** Nhãn miền để hiển thị (API). */
    public String regionLabel(String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) {
            return "Chưa xác định";
        }
        return provinceRepository.findById(provinceCode.trim())
                .map(Province::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .orElse("Chưa xác định");
    }
}
