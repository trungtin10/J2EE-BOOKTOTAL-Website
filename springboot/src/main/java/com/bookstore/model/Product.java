package com.bookstore.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Column(nullable = false)
    private String name;

    @Column(name = "name_search")
    private String nameSearch;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @Min(value = 0, message = "Giá sản phẩm không được nhỏ hơn 0")
    @Column(nullable = false)
    private Double price;

    // Optional "giá gốc" để hiển thị nhãn giảm giá (nếu có)
    @Min(value = 0, message = "Giá gốc không được nhỏ hơn 0")
    @Column(name = "original_price")
    private Double originalPrice;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    // Additional gallery images (comma / newline separated URLs or filenames)
    @Column(name = "image_gallery", columnDefinition = "TEXT")
    private String imageGallery;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được nhỏ hơn 0")
    @Column(nullable = false)
    private Integer quantity = 0;

    @Column(name = "sold_count")
    private Integer soldCount = 0;

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

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

    @Column(name = "color")
    private String color;

    @Column(name = "publisher_id")
    private Long publisherId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @NotNull(message = "Vui lòng chọn danh mục")
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

    public String getNameSearch() { return nameSearch; }
    public void setNameSearch(String nameSearch) { this.nameSearch = nameSearch; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(Double originalPrice) { this.originalPrice = originalPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageGallery() { return imageGallery; }
    public void setImageGallery(String imageGallery) { this.imageGallery = imageGallery; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }

    public Boolean getIsHidden() { return isHidden; }
    public void setIsHidden(Boolean isHidden) { this.isHidden = isHidden; }

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

    @Transient
    public List<String> getGalleryImages() {
        Set<String> urls = new LinkedHashSet<>();
        if (imageUrl != null && !imageUrl.isBlank()) {
            urls.add(imageUrl.trim());
        }
        if (imageGallery != null && !imageGallery.isBlank()) {
            String normalized = imageGallery.replace("\r", "\n");
            String[] parts = normalized.split("[,\n]+");
            for (String p : parts) {
                if (p == null) continue;
                String s = p.trim();
                if (!s.isEmpty()) urls.add(s);
            }
        }
        return new ArrayList<>(urls);
    }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    @PrePersist
    @PreUpdate
    private void syncSearchFields() {
        this.nameSearch = normalizeForSearch(this.name);
    }

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static String normalizeForSearch(String input) {
        if (input == null) return null;
        String s = input.trim().toLowerCase();
        if (s.isEmpty()) return "";
        s = Normalizer.normalize(s, Normalizer.Form.NFD);
        s = DIACRITICS.matcher(s).replaceAll("");
        // special case for Vietnamese đ/Đ
        s = s.replace('đ', 'd').replace('Đ', 'D');
        // collapse whitespace
        s = s.replaceAll("\\s+", " ");
        return s;
    }
}
