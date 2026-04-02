package com.bookstore.controller;

import com.bookstore.model.Product;
import com.bookstore.service.ProductService;
import com.bookstore.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

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
                               @RequestParam(name = "sort", defaultValue = "newest") String sort,
                               @RequestParam(name = "minPrice", required = false) Double filterMinPrice,
                               @RequestParam(name = "maxPrice", required = false) Double filterMaxPrice,
                               Model model) {
        int pageSize = 12;
        String kw = search != null ? search : "";
        Page<Product> productPage = productService.searchShopPage(
                categoryId, kw, sort, filterMinPrice, filterMaxPrice,
                PageRequest.of(Math.max(0, page - 1), pageSize));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("filterCategoryId", categoryId);
        model.addAttribute("filterSearch", kw);
        model.addAttribute("filterSort", sort);
        model.addAttribute("filterMinPrice", filterMinPrice);
        model.addAttribute("filterMaxPrice", filterMaxPrice);
        model.addAttribute("maxShopPrice", productService.getMaxVisibleProductPrice());
        return "product";
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getActiveProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviewService.getApprovedReviewsByProductId(id));
        model.addAttribute("ratingStats", reviewService.getRatingStats(id));
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id, product.getCategory() != null ? product.getCategory().getId() : null));

        return "product_detail";
    }
}
