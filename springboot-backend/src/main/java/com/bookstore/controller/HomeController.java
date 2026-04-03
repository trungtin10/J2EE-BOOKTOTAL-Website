package com.bookstore.controller;

import com.bookstore.model.Product;
import com.bookstore.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @GetMapping("/")
    public String index(Model model) {
        try {
            int stripNewSize = 10;
            Page<Product> newPage = productService.searchShopPage(
                    null, "", "newest", null, null, PageRequest.of(0, stripNewSize));
            List<Product> newList = newPage.getContent();
            List<Product> bests = productService.getHomeBestsellerProducts(5);

            model.addAttribute("homeDeals", productService.getHomeDealProducts(5));
            model.addAttribute("homeBestsellers", bests);
            model.addAttribute("homeNewProducts", newList);
            model.addAttribute("homeNewHasMore", newPage.hasNext());
            model.addAttribute("homeNewPageSize", stripNewSize);
            model.addAttribute("homeNewNextPage", 1);

            model.addAttribute("products", newList);
            model.addAttribute("homeProductsHasMore", newPage.hasNext());
            model.addAttribute("homeProductsPageSize", stripNewSize);
            model.addAttribute("homeProductsNextPage", 1);

            model.addAttribute("bestSellers", bests);
            model.addAttribute("newArrivals", newList);
        } catch (Exception e) {
            model.addAttribute("homeDeals", new ArrayList<>());
            model.addAttribute("homeBestsellers", new ArrayList<>());
            model.addAttribute("homeNewProducts", new ArrayList<>());
            model.addAttribute("homeNewHasMore", false);
            model.addAttribute("homeNewPageSize", 10);
            model.addAttribute("homeNewNextPage", 1);
            model.addAttribute("products", new ArrayList<>());
            model.addAttribute("bestSellers", new ArrayList<>());
            model.addAttribute("newArrivals", new ArrayList<>());
            model.addAttribute("homeProductsHasMore", false);
            model.addAttribute("homeProductsPageSize", 10);
            model.addAttribute("homeProductsNextPage", 1);
            System.err.println("Lỗi khi tải sản phẩm cho trang chủ: " + e.getMessage());
        }
        return "home";
    }
}
