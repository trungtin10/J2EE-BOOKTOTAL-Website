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
            // Lấy 12 sản phẩm đầu tiên để đảm bảo trang chủ luôn có dữ liệu
            Page<Product> productsPage = productService.getAllProducts(PageRequest.of(0, 12));
            List<Product> products = productsPage.getContent();

            // home.html render danh sách từ biến "products"
            model.addAttribute("products", products);

            // Giữ lại các biến cũ để tránh ảnh hưởng UI khác
            model.addAttribute("bestSellers", products);
            model.addAttribute("newArrivals", products);
        } catch (Exception e) {
            // Nếu có lỗi DB, truyền vào list rỗng để trang không bị crash
            model.addAttribute("products", new ArrayList<>());
            model.addAttribute("bestSellers", new ArrayList<>());
            model.addAttribute("newArrivals", new ArrayList<>());
            System.err.println("Lỗi khi tải sản phẩm cho trang chủ: " + e.getMessage());
        }
        return "home/index";
    }
}
