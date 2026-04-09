package com.bookstore.service;

import com.bookstore.model.Province;
import com.bookstore.repository.ProvinceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShippingService {

    @Autowired
    private ProvinceRepository provinceRepository;

    private static final double FREE_SHIPPING_THRESHOLD = 1000000.0;

    public double calculateShippingFee(String provinceCode, double subtotal) {
        if (subtotal >= FREE_SHIPPING_THRESHOLD) {
            return 0.0;
        }

        Optional<Province> provinceOpt = provinceRepository.findById(provinceCode);
        if (provinceOpt.isEmpty()) {
            return 30000.0; // Default fee if province not found (North/Default)
        }

        Province province = provinceOpt.get();
        String region = province.getRegion();
        
        // Fee calculation based on region:
        // Bắc: 30,000 VND
        // Trung: 25,000 VND
        // Nam: 20,000 VND
        if (region == null) {
            return 30000.0;
        }

        String lowerRegion = region.toLowerCase();
        if (lowerRegion.contains("bắc")) {
            return 30000.0;
        } else if (lowerRegion.contains("trung")) {
            return 25000.0;
        } else if (lowerRegion.contains("nam")) {
            return 20000.0;
        }

        return 30000.0; // Default North/Inter-region fee
    }
}
