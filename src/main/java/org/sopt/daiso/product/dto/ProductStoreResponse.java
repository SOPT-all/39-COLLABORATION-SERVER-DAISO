package org.sopt.daiso.product.dto;

public record ProductStoreResponse(Long storeId, String storeName, String imageUrl,
        String businessStatus, boolean bookmarked, long distanceMeters,
        int stockQuantity, boolean pickupAvailable) {
}
