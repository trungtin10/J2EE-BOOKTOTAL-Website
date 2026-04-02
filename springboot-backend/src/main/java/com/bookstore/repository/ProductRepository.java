package com.bookstore.repository;

import com.bookstore.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByNameContainingIgnoreCaseOrderByIdDesc(String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE COALESCE(p.isHidden, false) = false AND COALESCE(p.deleted, false) = false ORDER BY p.id DESC")
    Page<Product> findAllVisibleOrderByIdDesc(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE COALESCE(p.isHidden, false) = false AND COALESCE(p.deleted, false) = false AND p.category.id = :categoryId ORDER BY p.id DESC")
    Page<Product> findVisibleByCategoryIdOrderByIdDesc(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "COALESCE(p.isHidden, false) = false AND COALESCE(p.deleted, false) = false AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(p.nameNormalized, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           ") ORDER BY p.id DESC")
    Page<Product> searchVisibleByNameOrderByIdDesc(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(COALESCE(:keyword, '') = '' OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(COALESCE(p.nameNormalized, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(COALESCE(p.isHidden, false) = false) AND (COALESCE(p.deleted, false) = false)")
    Page<Product> searchProductsForShopPage(@Param("keyword") String keyword,
                                          @Param("categoryId") Long categoryId,
                                          @Param("minPrice") Double minPrice,
                                          @Param("maxPrice") Double maxPrice,
                                          Pageable pageable);

    @Query("SELECT MAX(p.price) FROM Product p WHERE COALESCE(p.isHidden, false) = false AND COALESCE(p.deleted, false) = false")
    Optional<Double> findMaxPriceAmongVisible();

    List<Product> findByDeletedIsFalseOrderBySoldCountDesc(Pageable pageable);

    List<Product> findByDeletedIsFalseOrderByIdDesc(Pageable pageable);

    @Query(value = "SELECT * FROM products WHERE (deleted IS NULL OR deleted = 0) AND (is_hidden IS NULL OR is_hidden = 0) ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Product> findRandomProducts(@Param("limit") int limit);

    @Query(value = "SELECT p.* FROM products p " +
                   "WHERE p.id != :productId AND p.category_id = :categoryId " +
                   "AND (p.deleted IS NULL OR p.deleted = 0) AND (p.is_hidden IS NULL OR p.is_hidden = 0) " +
                   "ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Product> findRandomRelatedProducts(@Param("productId") Long productId, @Param("categoryId") Long categoryId, @Param("limit") int limit);

    @Query("SELECT p FROM Product p WHERE " +
           "COALESCE(p.deleted, false) = false AND " +
           "(:keyword IS NULL OR p.name LIKE %:keyword%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:stockStatus = 'all' OR " +
           "(:stockStatus = 'ok' AND p.quantity >= 5) OR " +
           "(:stockStatus = 'low' AND p.quantity > 0 AND p.quantity < 5) OR " +
           "(:stockStatus = 'out' AND p.quantity = 0) OR " +
           "(:stockStatus = 'below5' AND p.quantity < 5))")
    Page<Product> searchInventory(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, @Param("stockStatus") String stockStatus, Pageable pageable);

    @Query("SELECT SUM(p.quantity) FROM Product p WHERE COALESCE(p.deleted, false) = false")
    Long getTotalStockQuantity();

    @Query("SELECT COUNT(p) FROM Product p WHERE COALESCE(p.deleted, false) = false AND p.quantity < :maxExclusive")
    long countActiveByQuantityLessThan(@Param("maxExclusive") int maxExclusive);

    @Query("SELECT COUNT(p) FROM Product p WHERE COALESCE(p.deleted, false) = false AND p.quantity = 0")
    long countActiveOutOfStockProducts();

    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR p.name LIKE %:keyword%) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:status = 'all' AND COALESCE(p.deleted, false) = false OR " +
           " (:status = 'hidden' AND COALESCE(p.deleted, false) = false AND p.isHidden = true) OR " +
           " (:status = 'visible' AND COALESCE(p.deleted, false) = false AND COALESCE(p.isHidden, false) = false) OR " +
           " (:status = 'deleted' AND COALESCE(p.deleted, false) = true))")
    Page<Product> searchAdminProducts(@Param("keyword") String keyword, @Param("categoryId") Long categoryId, @Param("status") String status, Pageable pageable);

    Optional<Product> findByIdAndDeletedIsFalse(Long id);
}
