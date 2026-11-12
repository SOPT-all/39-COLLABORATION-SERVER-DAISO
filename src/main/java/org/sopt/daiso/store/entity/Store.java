package org.sopt.daiso.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "store")
@DynamicUpdate
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(name = "business_status", nullable = false, length = 30)
    private String businessStatus;

    @Column(name = "is_favorite", nullable = false)
    private boolean favorite;

    @Column(nullable = false)
    private long distance;

    protected Store() {
    }

    public Long getId() { return id; }
    public boolean isFavorite() { return favorite; }

    public void changeFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
