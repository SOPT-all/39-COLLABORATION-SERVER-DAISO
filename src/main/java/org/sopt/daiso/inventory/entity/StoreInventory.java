package org.sopt.daiso.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.sopt.daiso.product.entity.Product;
import org.sopt.daiso.store.entity.Store;

@Entity
@Table(name = "store_inventory",
        uniqueConstraints = @UniqueConstraint(name = "uk_store_inventory_product_store", columnNames = {"product_id", "store_id"}),
        indexes = @Index(name = "idx_store_inventory_store_id", columnList = "store_id"))
public class StoreInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "pickup_available", nullable = false)
    private boolean pickupAvailable;

    protected StoreInventory() {
    }
}
