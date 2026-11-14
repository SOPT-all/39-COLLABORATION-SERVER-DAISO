package org.sopt.daiso.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.sopt.daiso.store.entity.BusinessStatus;

public record ProductStoreResponse(
        @Schema(description = "매장 ID", example = "201", requiredMode = Schema.RequiredMode.REQUIRED) Long storeId,
        @Schema(description = "매장명", example = "강남본점", requiredMode = Schema.RequiredMode.REQUIRED) String storeName,
        @Schema(description = "매장 이미지 URL", example = "https://example.com/store.png",
                requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
        @Schema(description = "저장된 영업 상태. OPEN: 영업중, CLOSED: 영업 전", example = "OPEN",
                requiredMode = Schema.RequiredMode.REQUIRED) BusinessStatus businessStatus,
        @Schema(description = "공유 매장 찜 상태. 매장 찜 API의 favorite와 같은 값", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean bookmarked,
        @Schema(description = "원본 거리, 미터 단위", example = "850",
                requiredMode = Schema.RequiredMode.REQUIRED) long distanceMeters,
        @Schema(description = "재고 수량. 0은 품절", example = "12",
                requiredMode = Schema.RequiredMode.REQUIRED) int stockQuantity,
        @Schema(description = "픽업 지원 여부. 재고·영업 상태와 독립", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean pickupAvailable) {
    @Schema(description = "표시 거리. 1000m 미만은 m, 이상은 km로 소수 첫째 자리 반올림 후 불필요한 0 제거",
            example = "850m", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.REQUIRED)
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
