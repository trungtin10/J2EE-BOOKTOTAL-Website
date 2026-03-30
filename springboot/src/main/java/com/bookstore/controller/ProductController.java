package com.bookstore.controller;

import com.bookstore.model.Product;
import com.bookstore.service.ProductService;
import com.bookstore.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/products")
    public String listProducts(@RequestParam(name = "page", defaultValue = "1") int page,
                               @RequestParam(name = "categoryId", required = false) Long categoryId,
                               @RequestParam(name = "search", required = false) String search,
                               @RequestParam(name = "keyword", required = false) String keyword,
                               @RequestParam(name = "minPrice", required = false) Double minPrice,
                               @RequestParam(name = "maxPrice", required = false) Double maxPrice,
                               @RequestParam(name = "sort", required = false) String sort,
                               Model model) {
        Page<Product> productPage;
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "id");
        if ("priceAsc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("priceDesc".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
        }
        PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), 9, sortOrder);

        // Unified filtering (category + price + search)
        String q = (search == null || search.isBlank()) ? keyword : search;
        productPage = productService.filterProducts(q, categoryId, minPrice, maxPrice, pageable);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("search", q);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        return "products/index";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (product.getDeletedAt() != null) {
            return "redirect:/products";
        }

        List<String> galleryImages = product.getGalleryImages().stream()
                .map(this::normalizeImageUrl)
                .collect(Collectors.toList());
        if (galleryImages.isEmpty()) {
            galleryImages = List.of("/uploads/default.jpg");
        }
        
        model.addAttribute("product", product);
        model.addAttribute("galleryImages", galleryImages);
        model.addAttribute("reviews", reviewService.getApprovedReviewsByProductId(id));
        model.addAttribute("ratingStats", reviewService.getRatingStats(id));
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id, product.getCategory() != null ? product.getCategory().getId() : null));

        return "products/detail";
    }

    private String normalizeImageUrl(String raw) {
        if (raw == null) return "/uploads/default.jpg";
        String s = raw.trim();
        if (s.isEmpty()) return "/uploads/default.jpg";
        if (s.startsWith("http") || s.startsWith("/uploads/")) return s;
        return "/uploads/" + s;
    }
}
