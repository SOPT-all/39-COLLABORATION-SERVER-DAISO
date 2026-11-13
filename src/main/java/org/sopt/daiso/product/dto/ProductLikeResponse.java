package org.sopt.daiso.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductLikeResponse(
        @Schema(description = "상품 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(description = "저장된 공유 좋아요 상태", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean liked) {
}
