package org.sopt.daiso.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StoreFavoriteResponse(
        @Schema(description = "매장 ID", example = "201", requiredMode = Schema.RequiredMode.REQUIRED) Long storeId,
        @Schema(description = "저장된 공유 매장 찜 상태", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean favorite) {
}
