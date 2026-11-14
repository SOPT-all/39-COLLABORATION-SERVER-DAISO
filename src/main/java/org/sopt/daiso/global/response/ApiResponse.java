package org.sopt.daiso.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiResponse<T>(
        @Schema(description = "성공 코드", example = "SUCCESS", allowableValues = "SUCCESS",
                requiredMode = Schema.RequiredMode.REQUIRED) String code,
        @Schema(description = "API별 성공 메시지", requiredMode = Schema.RequiredMode.REQUIRED) String message,
        @Schema(description = "API별 응답 데이터. 데이터가 없는 성공은 null 허용",
                requiredMode = Schema.RequiredMode.REQUIRED) T data) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }
}
