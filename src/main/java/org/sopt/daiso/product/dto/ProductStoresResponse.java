package org.sopt.daiso.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductStoresResponse(
        @Schema(description = "조회 상품 ID. product.productId와 같은 값", example = "1001",
                requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(description = "상단 상품 정보. 매장 결과가 없어도 반환", requiredMode = Schema.RequiredMode.REQUIRED)
        ProductListItemResponse product,
        @Schema(description = "해당 상품과 재고 관계가 있는 매장. 조건에 맞는 결과가 없으면 []",
                requiredMode = Schema.RequiredMode.REQUIRED) List<ProductStoreResponse> stores) {
}
