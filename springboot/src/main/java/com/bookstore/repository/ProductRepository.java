package com.bookstore.repository;

import com.bookstore.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByDeletedAtIsNull(Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndDeletedAtIsNullOrderByIdDesc(String keyword, Pageable pageable);

    // Sửa lại: JOIN với p.category thay vì p.categories
    Page<Product> findByDeletedAtIsNullAndCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "p.deletedAt IS NULL AND " +
           "(:keyword IS NULL OR p.name LIKE %:keyword%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    List<Product> searchProducts(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, Sort sort);

    @Query("SELECT p FROM Product p WHERE " +
            "p.deletedAt IS NULL AND " +
            "(:search IS NULL OR p.nameSearch LIKE CONCAT('%', :search, '%')) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<Product> filterProducts(@Param("search") String search,
                                 @Param("categoryId") Long categoryId,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 Pageable pageable);

    List<Product> findTop10ByOrderBySoldCountDesc();

    List<Product> findTop10ByOrderByCreatedAtDesc();

    @Query(value = "SELECT * FROM products WHERE deleted_at IS NULL ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Product> findRandomProducts(@Param("limit") int limit);

    @Query(value = "SELECT p.* FROM products p " +
                   "WHERE p.deleted_at IS NULL AND p.id != :productId AND p.category_id = :categoryId " +
                   "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Product> findRandomRelatedProducts(@Param("productId") Long productId, @Param("categoryId") Long categoryId, @Param("limit") int limit);

    @Query("SELECT p FROM Product p WHERE " +
           "p.deletedAt IS NULL AND " +
           "(:keyword IS NULL OR p.name LIKE %:keyword%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:stockStatus = 'all' OR " +
           "(:stockStatus = 'ok' AND p.quantity >= 10) OR " +
           "(:stockStatus = 'low' AND p.quantity > 0 AND p.quantity < 10) OR " +
           "(:stockStatus = 'out' AND p.quantity = 0))")
    Page<Product> searchInventory(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, @Param("stockStatus") String stockStatus, Pageable pageable);

    @Query("SELECT SUM(p.quantity) FROM Product p WHERE p.deletedAt IS NULL")
    Long getTotalStockQuantity();

    long countByQuantityLessThan(int stockLevel);

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:status = 'all' OR " +
           "(:status = 'hidden' AND p.isHidden = true AND p.deletedAt IS NULL) OR " +
           "(:status = 'visible' AND p.isHidden = false AND p.deletedAt IS NULL) OR " +
           "(:status = 'deleted' AND p.deletedAt IS NOT NULL))")
    Page<Product> searchAdminProducts(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, @Param("status") String status, Pageable pageable);
}
