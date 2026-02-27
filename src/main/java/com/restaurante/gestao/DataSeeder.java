package com.restaurante.gestao;

import com.restaurante.gestao.model.Product;
import com.restaurante.gestao.model.ProductCategory;
import com.restaurante.gestao.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            List<Product> baseProducts = List.of(
                    create("Água sem gás", ProductCategory.BEBIDAS, "4.50"),
                    create("Suco de laranja", ProductCategory.BEBIDAS, "8.00"),
                    create("Refrigerante lata", ProductCategory.BEBIDAS, "6.00"),
                    create("Prato feito", ProductCategory.REFEICOES, "29.90"),
                    create("Executivo de frango", ProductCategory.REFEICOES, "34.90"),
                    create("X-Burger", ProductCategory.LANCHES, "18.00"),
                    create("Batata frita", ProductCategory.LANCHES, "16.00"),
                    create("Pudim", ProductCategory.SOBREMESAS, "12.00"),
                    create("Mousse de maracujá", ProductCategory.SOBREMESAS, "11.50")
            );

            productRepository.saveAll(baseProducts);
        };
    }

    private Product create(String name, ProductCategory category, String price) {
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        product.setUnitPrice(new BigDecimal(price));
        product.setActive(true);
        return product;
    }
}
