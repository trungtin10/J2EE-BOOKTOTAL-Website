package com.bookstore.controller.admin;

import com.bookstore.model.Product;
import com.bookstore.service.ProductImageStorageService;
import com.bookstore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductImageStorageService productImageStorageService;

    @GetMapping
    public String listProducts(@RequestParam(name = "page", defaultValue = "0") int page,
                               @RequestParam(name = "keyword", required = false) String keyword,
                               @RequestParam(name = "categoryId", required = false) Long categoryId,
                               @RequestParam(name = "status", defaultValue = "all") String status,
                               Model model) {

        Page<Product> productPage = productService.searchAdminProducts(keyword, categoryId, status, PageRequest.of(page, 10));

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());

        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("status", status);

        return "admin/products/product_list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", productService.getAllCategories());
        return "admin/products/product_add";
    }

    @PostMapping("/add")
    public String addProduct(@Valid @ModelAttribute Product product,
                             BindingResult bindingResult,
                             @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                             @RequestParam(name = "galleryFiles", required = false) MultipartFile[] galleryFiles,
                             @RequestParam(name = "externalImageUrl", required = false) String externalImageUrl,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productService.getAllCategories());
            return "admin/products/product_add";
        }
        product.setDeleted(false);
        product.setDeletedAt(null);
        applyProductImage(product, imageFile, externalImageUrl, null);
        applyProductGallery(product, galleryFiles, null, null);
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "Thêm sản phẩm thành công");
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable(name = "id") Long id, Model model) {
        Product product = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product id: " + id));
        model.addAttribute("product", product);
        model.addAttribute("categories", productService.getAllCategories());
        return "admin/products/product_edit";
    }

    @PostMapping("/edit/{id}")
    public String editProduct(@PathVariable(name = "id") Long id,
                              @Valid @ModelAttribute Product product,
                              BindingResult bindingResult,
                              @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                              @RequestParam(name = "galleryFiles", required = false) MultipartFile[] galleryFiles,
                              @RequestParam(name = "galleryRemove", required = false) String galleryRemove,
                              @RequestParam(name = "externalImageUrl", required = false) String externalImageUrl,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", productService.getAllCategories());
            return "admin/products/product_edit";
        }
        Product existingProduct = productService.getProductById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product id: " + id));

        product.setSoldCount(existingProduct.getSoldCount());
        product.setDeleted(existingProduct.getDeleted());
        product.setDeletedAt(existingProduct.getDeletedAt());

        applyProductImage(product, imageFile, externalImageUrl, existingProduct);
        applyProductGallery(product, galleryFiles, existingProduct, galleryRemove);

        product.setId(id);
        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật sản phẩm thành công");
        return "redirect:/admin/products";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable(name = "id") Long id, RedirectAttributes redirectAttributes) {
        productService.softDeleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa mềm sản phẩm (ẩn khỏi shop, giữ lịch sử đơn hàng).");
        return "redirect:/admin/products";
    }

    @PostMapping("/restore/{id}")
    public String restoreProduct(@PathVariable(name = "id") Long id, RedirectAttributes redirectAttributes) {
        productService.restoreProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã khôi phục sản phẩm.");
        return "redirect:/admin/products?status=deleted";
    }

    private void applyProductImage(Product product, MultipartFile imageFile, String externalImageUrl, Product previous) {
        if (StringUtils.hasText(externalImageUrl)) {
            product.setImageUrl(externalImageUrl.trim());
        } else if (imageFile != null && !imageFile.isEmpty()) {
            product.setImageUrl(productImageStorageService.storeProductImage(imageFile));
        } else if (previous != null) {
            product.setImageUrl(previous.getImageUrl());
        }
    }

    private void applyProductGallery(Product product, MultipartFile[] galleryFiles, Product previous, String galleryRemove) {
        java.util.Set<String> removeSet = new java.util.HashSet<>();
        if (StringUtils.hasText(galleryRemove)) {
            for (String part : galleryRemove.split("[,;\\n]+")) {
                String p = part != null ? part.trim() : "";
                if (!p.isEmpty()) removeSet.add(p);
            }
        }

        java.util.List<String> kept = new java.util.ArrayList<>();
        if (previous != null && StringUtils.hasText(previous.getGalleryImages())) {
            for (String part : previous.getGalleryImages().split("[,;\\n]+")) {
                String p = part != null ? part.trim() : "";
                if (p.isEmpty()) continue;
                if (!removeSet.contains(p)) kept.add(p);
            }
        }

        if (galleryFiles != null) {
            for (MultipartFile f : galleryFiles) {
                if (f == null || f.isEmpty()) continue;
                String url = productImageStorageService.storeProductImage(f);
                if (StringUtils.hasText(url)) kept.add(url.trim());
            }
        }

        product.setGalleryImages(String.join("\n", kept));
    }
}
