package org.sopt.daiso.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ProductListResponse(
        @Schema(description = "조건에 맞는 전체 상품, ID 오름차순. 결과가 없으면 []",
                requiredMode = Schema.RequiredMode.REQUIRED) List<ProductListItemResponse> products) {
}
