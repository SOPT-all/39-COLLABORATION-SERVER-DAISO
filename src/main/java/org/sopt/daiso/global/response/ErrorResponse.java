package org.sopt.daiso.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(description = "API별 오류 코드", requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(description = "오류 설명", requiredMode = Schema.RequiredMode.REQUIRED) String message) {
}
