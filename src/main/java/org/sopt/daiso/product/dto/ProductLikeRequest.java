package org.sopt.daiso.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import tools.jackson.databind.JsonNode;

public record ProductLikeRequest(
        @Schema(description = "저장할 공유 좋아요 상태. true 설정, false 해제", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean liked) {
    public static ProductLikeRequest from(JsonNode body) {
        // 문자열 "true"나 숫자의 자동 변환을 허용하지 않고 JSON boolean만 받는다.
        if (body == null || !body.isObject()) {
            throw new CustomException(ErrorType.INVALID_REQUEST);
        }
        JsonNode liked = body.get("liked");
        if (liked == null || !liked.isBoolean()) {
            throw new CustomException(ErrorType.INVALID_REQUEST);
        }
        return new ProductLikeRequest(liked.booleanValue());
    }
}
