package org.sopt.daiso.product.repository;

import org.sopt.daiso.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
