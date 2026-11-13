package org.sopt.daiso.inventory.repository;

import java.util.List;
import org.sopt.daiso.inventory.entity.StoreInventory;
import org.sopt.daiso.product.dto.ProductStoreResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
    @Query("""
            SELECT new org.sopt.daiso.product.dto.ProductStoreResponse(
                s.id, s.name, s.imageUrl, s.businessStatus, s.favorite, s.distance,
                inventory.stockQuantity, inventory.pickupAvailable)
            FROM StoreInventory inventory
            JOIN inventory.store s
            WHERE inventory.product.id = :productId
              AND (:soldOut = false OR inventory.stockQuantity >= 1)
            """)
    List<ProductStoreResponse> findStores(@Param("productId") long productId, @Param("soldOut") boolean soldOut);
}
