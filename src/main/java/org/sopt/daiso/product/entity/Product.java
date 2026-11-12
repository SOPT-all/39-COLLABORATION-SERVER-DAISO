package org.sopt.daiso.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_number", nullable = false, length = 100)
    private String productNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String price;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "review_count", nullable = false)
    private String reviewCount;

    @Column(name = "is_liked", nullable = false)
    private boolean liked;

    protected Product() {
    }
}
