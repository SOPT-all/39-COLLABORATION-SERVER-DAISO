package org.sopt.daiso.product.repository;

import java.util.List;
import org.sopt.daiso.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByOrderByIdAsc();

    // Containing은 LIKE 특수문자를 이스케이프하며 품번에는 정확 일치를 적용한다.
    List<Product> findByNameContainingOrProductNumberOrderByIdAsc(String name, String productNumber);
}
