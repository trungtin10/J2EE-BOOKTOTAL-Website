package com.bookstore.controller.api;

import com.bookstore.model.Product;
import com.bookstore.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    @Autowired
    private ProductService productService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllProducts(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {

        int safeSize = Math.min(Math.max(size, 1), 48);
        int safePage = Math.max(0, page);
        String kw = keyword != null ? keyword : "";
        Page<Product> p = productService.searchShopPage(
                categoryId, kw, sort, minPrice, maxPrice, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(Map.of(
                "content", p.getContent(),
                "totalPages", p.getTotalPages(),
                "totalElements", p.getTotalElements(),
                "number", p.getNumber(),
                "size", p.getSize(),
                "first", p.isFirst(),
                "last", p.isLast()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable(name = "id") Long id) {
        return productService.getActiveProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedProducts() {
        // Simple logic for featured products, for example, top 8 sold products
        List<Product> products = productService.getBestSellingProducts(8);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestProducts() {
        List<Product> products = productService.getNewArrivals(8);
        return ResponseEntity.ok(products);
    }
}
