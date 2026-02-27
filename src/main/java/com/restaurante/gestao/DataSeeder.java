package com.restaurante.gestao;

import com.restaurante.gestao.model.*;
import com.restaurante.gestao.repository.AppUserRepository;
import com.restaurante.gestao.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedBaseData(ProductRepository productRepository, AppUserRepository appUserRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                List<Product> baseProducts = List.of(
                        createProduct("Água sem gás", ProductCategory.BEBIDAS, "4.50"),
                        createProduct("Suco de laranja", ProductCategory.BEBIDAS, "8.00"),
                        createProduct("Refrigerante lata", ProductCategory.BEBIDAS, "6.00"),
                        createProduct("Prato feito", ProductCategory.REFEICOES, "29.90"),
                        createProduct("Executivo de frango", ProductCategory.REFEICOES, "34.90"),
                        createProduct("X-Burger", ProductCategory.LANCHES, "18.00"),
                        createProduct("Batata frita", ProductCategory.LANCHES, "16.00"),
                        createProduct("Pudim", ProductCategory.SOBREMESAS, "12.00"),
                        createProduct("Mousse de maracujá", ProductCategory.SOBREMESAS, "11.50")
                );

                productRepository.saveAll(baseProducts);
            }

            if (appUserRepository.count() == 0) {
                appUserRepository.saveAll(List.of(
                        createUser("Administrador", "admin", "admin123", UserRole.ADMIN),
                        createUser("João Garçom", "garcom", "123", UserRole.WAITER),
                        createUser("Maria Cozinha", "cozinha", "123", UserRole.KITCHEN),
                        createUser("Carlos Caixa", "caixa", "123", UserRole.CASHIER),
                        createUser("Ana Estoque", "estoque", "123", UserRole.STOCK)
                ));
            }
        };
    }

    private Product createProduct(String name, ProductCategory category, String price) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setUnitPrice(new BigDecimal(price));
        product.setActive(true);
        return product;
    }

    private AppUser createUser(String fullName, String username, String password, UserRole role) {
        AppUser user = new AppUser();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setActive(true);
        return user;
    }
}
