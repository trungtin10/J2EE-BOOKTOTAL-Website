package com.bookstore.config;

import com.bookstore.model.Product;
import com.bookstore.model.User;
import com.bookstore.repository.ProductRepository;
import com.bookstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class DbBackfillRunner implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    @Override
    @Transactional
    public void run(String... args) {
        backfillUsersEnabled();
        backfillProductsNameSearch();
    }

    private void backfillUsersEnabled() {
        List<User> users = userRepository.findAll();
        boolean changed = false;
        for (User u : users) {
            if (u.getEnabled() == null) {
                u.setEnabled(true);
                changed = true;
            }
        }
        if (changed) {
            userRepository.saveAll(users);
        }
    }

    private void backfillProductsNameSearch() {
        List<Product> products = productRepository.findAll();
        List<Product> dirty = new ArrayList<>();

        for (Product p : products) {
            String existing = p.getNameSearch();
            if (existing == null || existing.isBlank()) {
                String computed = normalizeForSearch(p.getName());
                p.setNameSearch(computed);
                dirty.add(p);
            }
        }

        if (!dirty.isEmpty()) {
            productRepository.saveAll(dirty);
        }
    }

    private static String normalizeForSearch(String input) {
        if (input == null) return null;
        String norm = Normalizer.normalize(input, Normalizer.Form.NFD);
        norm = DIACRITICS.matcher(norm).replaceAll("");
        norm = norm.replace('đ', 'd').replace('Đ', 'D');
        norm = norm.toLowerCase(Locale.ROOT).trim();
        norm = norm.replaceAll("\\s+", " ");
        return norm;
    }
}

