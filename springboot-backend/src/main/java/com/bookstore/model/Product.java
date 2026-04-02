package com.bookstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(nullable = false)
    private String name;

    /** Tên không dấu (phục vụ tìm kiếm nhanh có/không dấu). */
    @Column(name = "name_normalized")
    private String nameNormalized;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 0, message = "Giá sản phẩm không được nhỏ hơn 0")
    @Column(nullable = false)
    private Double price;

    /** Giá gốc (niêm yết). Nếu lớn hơn {@link #price} thì hiển thị nhãn giảm giá. */
    @DecimalMin(value = "0.0", inclusive = true, message = "Giá gốc không được âm")
    @Column(name = "original_price")
    private Double originalPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0")
    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "sold_count")
    private Integer soldCount = 0;

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    @Column(nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "publication_year")
    private Integer publicationYear;

    private Integer pages;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "publisher_name")
    private String publisherName;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "cover_type")
    private String coverType;

    private String language;
    private String dimensions;

    /** Màu bìa / chủ đạo (hiển thị cho khách). */
    private String color;

    /** Thêm ảnh phụ: mỗi dòng hoặc phân tách bằng dấu phẩy (đường dẫn uploads/ hoặc URL đầy đủ). */
    @Column(name = "gallery_images", columnDefinition = "TEXT")
    private String galleryImages;

    @Column(name = "publisher_id")
    private Long publisherId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Product() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNameNormalized() { return nameNormalized; }
    public void setNameNormalized(String nameNormalized) { this.nameNormalized = nameNormalized; }

    public static String normalizeVietnamese(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return "";
        // tách dấu -> bỏ combining marks
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        // xử lý riêng đ/Đ
        s = s.replace('đ', 'd').replace('Đ', 'd');
        // gom whitespace
        return s.replaceAll("\\s+", " ").trim();
    }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }

    /** % giảm so với giá gốc, hoặc null nếu không áp dụng (không map DB). */
    public Integer getDiscountPercent() {
        if (originalPrice == null || originalPrice <= 0 || price == null || price >= originalPrice) {
            return null;
        }
        return (int) Math.round((1.0 - price / originalPrice) * 100.0);
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }

    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }

    public Boolean getDeleted() { return deleted; }
    public void setDeleted(Boolean deleted) { this.deleted = deleted != null && deleted; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getCoverType() { return coverType; }
    public void setCoverType(String coverType) { this.coverType = coverType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getGalleryImages() { return galleryImages; }
    public void setGalleryImages(String galleryImages) { this.galleryImages = galleryImages; }

    /** Ảnh bìa trước, sau đó ảnh phụ (không trùng). Dùng cho gallery khách hàng. */
    public List<String> getResolvedGalleryUrls() {
        Set<String> uniq = new LinkedHashSet<>();
        if (imageUrl != null && !imageUrl.isBlank()) {
            uniq.add(imageUrl.trim());
        }
        if (galleryImages != null && !galleryImages.isBlank()) {
            for (String part : galleryImages.split("[,;\\n]+")) {
                String p = part.trim();
                if (!p.isEmpty()) {
                    uniq.add(p);
                }
            }
        }
        return new ArrayList<>(uniq);
    }

    /** Chỉ ảnh phụ (gallery) — KHÔNG bao gồm ảnh gốc {@link #imageUrl}. Dùng cho trang quản trị. */
    public List<String> getResolvedGalleryOnlyUrls() {
        Set<String> uniq = new LinkedHashSet<>();
        String main = imageUrl != null ? imageUrl.trim() : "";
        if (galleryImages != null && !galleryImages.isBlank()) {
            for (String part : galleryImages.split("[,;\\n]+")) {
                String p = part != null ? part.trim() : "";
                if (p.isEmpty()) continue;
                if (!main.isEmpty() && p.equals(main)) continue;
                uniq.add(p);
            }
        }
        return new ArrayList<>(uniq);
    }

    /** Chuẩn hóa đường dẫn ảnh cho thẻ &lt;img src&gt; */
    public String resolveImageSrc(String path) {
        if (path == null || path.isBlank()) {
            return "/img/placeholder.svg";
        }
        String p = path.trim();
        if (p.startsWith("http://") || p.startsWith("https://") || p.startsWith("/uploads/")) {
            return p;
        }
        return "/uploads/" + p;
    }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
