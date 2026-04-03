package com.bookstore.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.dir:uploads/}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Nhiều vị trí: upload.dir (vd ../uploads/) + uploads cạnh working dir (vd springboot-backend/uploads/)
        Set<Path> roots = new LinkedHashSet<>();
        roots.add(Paths.get(uploadDir).toAbsolutePath().normalize());
        String userDir = System.getProperty("user.dir", ".");
        roots.add(Paths.get(userDir, "uploads").toAbsolutePath().normalize());
        Path parent = Paths.get(userDir).toAbsolutePath().getParent();
        if (parent != null) {
            roots.add(parent.resolve("uploads").normalize());
        }

        List<String> locations = new ArrayList<>(roots.size());
        for (Path p : roots) {
            String uri = p.toUri().toString();
            locations.add(uri.endsWith("/") ? uri : uri + "/");
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(locations.toArray(String[]::new));

        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
