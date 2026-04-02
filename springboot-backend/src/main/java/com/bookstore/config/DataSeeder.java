package com.bookstore.config;

import com.bookstore.model.Category;
import com.bookstore.model.Product;
import com.bookstore.model.User;
import com.bookstore.repository.CategoryRepository;
import com.bookstore.repository.ProductRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            seedUsers();
        }
        // Seed categories and products independently.
        // Tránh trường hợp DB đã có categories (count>0) nhưng products rỗng => shop không hiển thị gì.
        if (categoryRepository.count() == 0) {
            seedCategories();
        }
        if (productRepository.count() == 0) {
            seedProductsUsingExistingCategories();
        }
        if (couponRepository.count() == 0) {
            seedCoupons();
        }
    }

    private void seedUsers() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@bookstore.com");
        admin.setFullName("System Admin");
        admin.setRole("admin");
        userRepository.save(admin);

        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("user123"));
        user.setEmail("user@example.com");
        user.setFullName("Regular User");
        user.setRole("user");
        userRepository.save(user);
    }

    private void seedCategories() {
        Category c1 = new Category(); c1.setName("Văn học");
        Category c2 = new Category(); c2.setName("Kinh tế");
        Category c3 = new Category(); c3.setName("Kỹ năng sống");
        Category c4 = new Category(); c4.setName("Thiếu nhi");
        Category c5 = new Category(); c5.setName("Ngoại ngữ");
        categoryRepository.saveAll(Arrays.asList(c1, c2, c3, c4, c5));
    }

    private void seedProductsUsingExistingCategories() {
        List<Category> cats = categoryRepository.findAll();
        if (cats.isEmpty()) {
            seedCategories();
            cats = categoryRepository.findAll();
        }

        // fallback theo thứ tự đã seed (nếu tên khác thì lấy đại theo index)
        Category vanHoc = pickCategory(cats, "Văn học", 0);
        Category kinhTe = pickCategory(cats, "Kinh tế", 1);
        Category kyNang = pickCategory(cats, "Kỹ năng sống", 2);
        Category thieuNhi = pickCategory(cats, "Thiếu nhi", 3);

        Product p1 = new Product();
        p1.setName("Đắc Nhân Tâm");
        p1.setOriginalPrice(95000.0);
        p1.setPrice(76000.0);
        p1.setQuantity(100);
        p1.setAuthorName("Dale Carnegie");
        p1.setPublisherName("NXB Tổng hợp TPHCM");
        p1.setSupplierName("First News");
        p1.setDescription("Cuốn sách nổi tiếng nhất thế giới về nghệ thuật giao tiếp.");
        p1.setCategory(kyNang);
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setName("Nhà Giả Kim");
        p2.setPrice(63000.0);
        p2.setQuantity(50);
        p2.setAuthorName("Paulo Coelho");
        p2.setPublisherName("NXB Hội Nhà Văn");
        p2.setSupplierName("Nhã Nam");
        p2.setDescription("Hành trình theo đuổi vận mệnh của chàng chăn cừu Santiago.");
        p2.setCategory(vanHoc);
        productRepository.save(p2);
        
        Product p3 = new Product();
        p3.setName("Cha Giàu Cha Nghèo");
        p3.setOriginalPrice(150000.0);
        p3.setPrice(120000.0);
        p3.setQuantity(30);
        p3.setAuthorName("Robert T. Kiyosaki");
        p3.setPublisherName("NXB Trẻ");
        p3.setSupplierName("NXB Trẻ");
        p3.setDescription("Bí mật về tư duy tài chính để trở nên giàu có.");
        p3.setCategory(kinhTe);
        productRepository.save(p3);

        Product p4 = new Product();
        p4.setName("Dế Mèn Phiêu Lưu Ký");
        p4.setPrice(45000.0);
        p4.setQuantity(80);
        p4.setAuthorName("Tô Hoài");
        p4.setPublisherName("NXB Kim Đồng");
        p4.setSupplierName("Kim Đồng");
        p4.setCategory(thieuNhi);
        productRepository.save(p4);

        Product p5 = new Product();
        p5.setName("Harry Potter và Hòn Đá Phù Thủy");
        p5.setOriginalPrice(220000.0);
        p5.setPrice(185000.0);
        p5.setQuantity(25);
        p5.setAuthorName("J.K. Rowling");
        p5.setPublisherName("NXB Trẻ");
        p5.setSupplierName("Trẻ");
        p5.setCategory(vanHoc);
        productRepository.save(p5);
    }

    private static Category pickCategory(List<Category> cats, String preferName, int fallbackIndex) {
        if (cats == null || cats.isEmpty()) return null;
        if (preferName != null && !preferName.isBlank()) {
            for (Category c : cats) {
                if (c != null && preferName.equalsIgnoreCase(c.getName())) {
                    return c;
                }
            }
        }
        int idx = Math.min(Math.max(0, fallbackIndex), cats.size() - 1);
        return cats.get(idx);
    }

    @Autowired
    private com.bookstore.repository.CouponRepository couponRepository;

    private void seedCoupons() {
        com.bookstore.model.Coupon cp1 = new com.bookstore.model.Coupon();
        cp1.setCode("GIAM20K");
        cp1.setDiscountType("FIXED");
        cp1.setDiscountValue(20000.0);
        cp1.setMinOrderValue(100000.0);
        cp1.setIsActive(true);
        
        com.bookstore.model.Coupon cp2 = new com.bookstore.model.Coupon();
        cp2.setCode("HE2024");
        cp2.setDiscountType("PERCENT");
        cp2.setDiscountValue(10.0);
        cp2.setMaxDiscountAmount(50000.0);
        cp2.setMinOrderValue(200000.0);
        cp2.setIsActive(true);

        couponRepository.saveAll(Arrays.asList(cp1, cp2));
    }
}
