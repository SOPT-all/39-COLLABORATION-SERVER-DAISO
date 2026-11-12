package org.sopt.daiso.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "product")
// 좋아요만 바꿀 때 다른 트랜잭션이 갱신한 상품 정보를 덮어쓰지 않는다.
@DynamicUpdate
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_number", nullable = false, length = 100)
    private String productNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "is_liked", nullable = false)
    private boolean liked;

    protected Product() {
    }

    public Long getId() {
        return id;
    }

    public boolean isLiked() {
        return liked;
    }

    public void changeLiked(boolean liked) {
        // 토글하지 않고 요청 값을 설정해 같은 요청을 반복해도 상태를 유지한다.
        this.liked = liked;
    }
}
