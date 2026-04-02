package com.bookstore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Lưu ảnh sản phẩm: Cloudinary (khi đủ cloud-name, api-key, api-secret) hoặc thư mục upload cục bộ.
 */
@Service
public class ProductImageStorageService {

    private final FileStorageService fileStorageService;
    private final Optional<Cloudinary> cloudinary;

    @Autowired
    public ProductImageStorageService(
            FileStorageService fileStorageService,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.fileStorageService = fileStorageService;
        if (StringUtils.hasText(cloudName) && StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
            this.cloudinary = Optional.of(new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName.trim(),
                    "api_key", apiKey.trim(),
                    "api_secret", apiSecret.trim())));
        } else {
            this.cloudinary = Optional.empty();
        }
    }

    public String storeProductImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File rỗng");
        }
        if (cloudinary.isPresent()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = cloudinary.get().uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("folder", "booktotal/products", "resource_type", "image"));
                Object url = result.get("secure_url");
                if (url == null) {
                    url = result.get("url");
                }
                if (url == null) {
                    throw new IOException("Cloudinary không trả về URL");
                }
                return url.toString();
            } catch (IOException e) {
                throw new RuntimeException("Không upload được lên Cloudinary: " + e.getMessage(), e);
            }
        }
        return fileStorageService.storeFile(file);
    }
}
