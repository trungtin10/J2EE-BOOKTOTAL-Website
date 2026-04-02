package com.bookstore.service;

import com.bookstore.model.Category;
import com.bookstore.model.Product;
import com.bookstore.model.InventoryLog;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.InventoryLogRepository;
import com.bookstore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAllVisibleOrderByIdDesc(pageable);
    }

    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchVisibleByNameOrderByIdDesc(keyword.trim(), pageable);
    }

    public Page<Product> getProductsByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findVisibleByCategoryIdOrderByIdDesc(categoryId, pageable);
    }

    public List<Product> searchProducts(Long categoryId, String keyword, String sort) {
        return searchProducts(categoryId, keyword, sort, null, null);
    }

    public List<Product> searchProducts(Long categoryId, String keyword, String sort, Double minPrice, Double maxPrice) {
        return searchShopPage(categoryId, keyword, sort, minPrice, maxPrice, Pageable.unpaged()).getContent();
    }

    public Page<Product> searchShopPage(Long categoryId, String keyword, String sort,
                                        Double minPrice, Double maxPrice, Pageable pageable) {
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();
        Double min = minPrice;
        Double max = maxPrice;
        if (min != null && max != null && min > max) {
            double t = min;
            min = max;
            max = t;
        }
        org.springframework.data.domain.Sort sortOrder = resolveShopSort(sort);
        Pageable pg = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortOrder);
        return productRepository.searchProductsForShopPage(kw, categoryId, min, max, pg);
    }

    private static org.springframework.data.domain.Sort resolveShopSort(String sort) {
        switch (sort != null ? sort : "newest") {
            case "priceAsc":
                return org.springframework.data.domain.Sort.by("price").ascending();
            case "priceDesc":
                return org.springframework.data.domain.Sort.by("price").descending();
            case "soldDesc":
                return org.springframework.data.domain.Sort.by("soldCount").descending();
            default:
                return org.springframework.data.domain.Sort.by("id").descending();
        }
    }

    public double getMaxVisibleProductPrice() {
        return productRepository.findMaxPriceAmongVisible().orElse(1_000_000.0);
    }

    public List<Product> getBestSellingProducts(int limit) {
        return productRepository.findByDeletedIsFalseOrderBySoldCountDesc(PageRequest.of(0, limit));
    }

    public List<Product> getNewArrivals(int limit) {
        return productRepository.findByDeletedIsFalseOrderByIdDesc(PageRequest.of(0, limit));
    }

    public List<Product> getBestSellers() {
        return productRepository.findByDeletedIsFalseOrderBySoldCountDesc(PageRequest.of(0, 10));
    }

    public List<Product> getNewArrivals() {
        return productRepository.findByDeletedIsFalseOrderByIdDesc(PageRequest.of(0, 10));
    }

    public List<Product> getOnSaleProducts() {
        // Match Node.js behavior: Randomized selection of 10 products
        return productRepository.findRandomProducts(10);
    }

    public List<Product> getRelatedProducts(Long productId, Long categoryId) {
        if (categoryId == null) return java.util.Collections.emptyList();
        // Match Node.js behavior: Randomized selection of 5 related products
        return productRepository.findRandomRelatedProducts(productId, categoryId, 5);
    }

    /** Admin / khôi phục: bao gồm cả sản phẩm đã xóa mềm. */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    /** Shop, giỏ, API công khai: chỉ sản phẩm chưa xóa mềm. */
    public Optional<Product> getActiveProductById(Long id) {
        return productRepository.findByIdAndDeletedIsFalse(id);
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            product.setDeleted(false);
            product.setDeletedAt(null);
        }
        // Tối ưu tìm kiếm có/không dấu
        if (product.getName() != null) {
            product.setNameNormalized(Product.normalizeVietnamese(product.getName()));
        }
        return productRepository.save(product);
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        if (Boolean.TRUE.equals(p.getDeleted())) {
            return;
        }
        p.setDeleted(true);
        p.setDeletedAt(java.time.LocalDateTime.now());
        p.setIsHidden(true);
        productRepository.save(p);
    }

    @Transactional
    public void restoreProduct(Long id) {
        Product p = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        p.setDeleted(false);
        p.setDeletedAt(null);
        productRepository.save(p);
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<Category> searchCategories(String keyword) {
        return categoryRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword);
    }

    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public void importStock(Long productId, Integer quantity, String note) {
        Product product = productRepository.findById(productId).orElseThrow();
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new RuntimeException("Sản phẩm đã xóa mềm — không nhập kho được.");
        }
        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);
        
        InventoryLog log = new InventoryLog(product, quantity, "IMPORT", note);
        inventoryLogRepository.save(log);
    }

    public void exportStock(Long productId, Integer quantity, String note) {
        Product product = productRepository.findById(productId).orElseThrow();
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new RuntimeException("Sản phẩm đã xóa mềm — không xuất kho được.");
        }
        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Số lượng tồn kho không đủ để xuất!");
        }
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);
        
        InventoryLog log = new InventoryLog(product, -quantity, "EXPORT", note);
        inventoryLogRepository.save(log);
    }

    public List<InventoryLog> getInventoryLogs(Long productId) {
        return inventoryLogRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public Page<Product> searchInventory(String keyword, Long categoryId, String stockStatus, Pageable pageable) {
        return productRepository.searchInventory(keyword, categoryId, stockStatus, pageable);
    }

    public Long getTotalStockQuantity() {
        Long total = productRepository.getTotalStockQuantity();
        return total != null ? total : 0L;
    }

    /** Số SKU có tồn kho &lt; 5 (cảnh báo nhập hàng). */
    public long countLowStockProducts() {
        return productRepository.countActiveByQuantityLessThan(5);
    }

    /** Số SKU đang hết hàng (quantity = 0). */
    public long countOutOfStockProducts() {
        return productRepository.countActiveOutOfStockProducts();
    }

    @Transactional
    public void deductStockForOrderConfirmation(Long productId, int qty, Long orderId) {
        Product product = productRepository.findById(productId).orElseThrow();
        if (Boolean.TRUE.equals(product.getDeleted())) {
            throw new RuntimeException("Sản phẩm \"" + product.getName() + "\" đã ngừng kinh doanh (xóa mềm).");
        }
        if (product.getQuantity() < qty) {
            throw new RuntimeException("Không đủ tồn kho cho \"" + product.getName()
                    + "\" (còn " + product.getQuantity() + ", đơn cần " + qty + ").");
        }
        product.setQuantity(product.getQuantity() - qty);
        int sold = product.getSoldCount() != null ? product.getSoldCount() : 0;
        product.setSoldCount(sold + qty);
        productRepository.save(product);
        inventoryLogRepository.save(new InventoryLog(product, -qty, "EXPORT",
                "Trừ tồn khi duyệt đơn #" + orderId));
    }

    @Transactional
    public void restoreStockAfterOrderRelease(Long productId, int qty, Long orderId) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setQuantity(product.getQuantity() + qty);
        int sold = product.getSoldCount() != null ? product.getSoldCount() : 0;
        product.setSoldCount(Math.max(0, sold - qty));
        productRepository.save(product);
        inventoryLogRepository.save(new InventoryLog(product, qty, "IMPORT",
                "Hoàn tồn khi hủy/hoàn tác đơn #" + orderId));
    }

    public Page<Product> searchAdminProducts(String keyword, Long categoryId, String status, Pageable pageable) {
        return productRepository.searchAdminProducts(keyword, categoryId, status, pageable);
    }

    public Page<Category> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    public Page<Category> searchCategories(String keyword, Pageable pageable) {
        return categoryRepository.findByNameContainingIgnoreCaseOrderByIdDesc(keyword, pageable);
    }
}
