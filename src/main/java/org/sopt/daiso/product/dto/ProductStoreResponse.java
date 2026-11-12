package org.sopt.daiso.product.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ProductStoreResponse(Long storeId, String storeName, String imageUrl,
        String businessStatus, boolean bookmarked, long distanceMeters,
        int stockQuantity, boolean pickupAvailable) {
    public String getDistance() {
        if (distanceMeters < 1000) {
            return distanceMeters + "m";
        }
        return BigDecimal.valueOf(distanceMeters, 3)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "km";
    }
}
