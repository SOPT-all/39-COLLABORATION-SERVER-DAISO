package org.sopt.daiso.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import tools.jackson.databind.JsonNode;

public record StoreFavoriteRequest(
        @Schema(description = "저장할 공유 매장 찜 상태. true 설정, false 해제", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean favorite) {
    public static StoreFavoriteRequest from(JsonNode body) {
        if (body == null) {
            throw new CustomException(ErrorType.FAVORITE_REQUIRED);
        }
        if (!body.isObject()) {
            throw new CustomException(ErrorType.INVALID_REQUEST);
        }
        JsonNode favorite = body.get("favorite");
        if (favorite == null || favorite.isNull()) {
            throw new CustomException(ErrorType.FAVORITE_REQUIRED);
        }
        if (!favorite.isBoolean()) {
            throw new CustomException(ErrorType.INVALID_FAVORITE_TYPE);
        }
        return new StoreFavoriteRequest(favorite.booleanValue());
    }
}
