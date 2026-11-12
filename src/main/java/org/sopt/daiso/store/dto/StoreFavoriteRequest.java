package org.sopt.daiso.store.dto;

import org.sopt.daiso.global.exception.CommonErrorCode;
import org.sopt.daiso.global.exception.StoreErrorCode;
import org.sopt.daiso.global.exception.StoreException;
import tools.jackson.databind.JsonNode;

public record StoreFavoriteRequest(boolean favorite) {
    public static StoreFavoriteRequest from(JsonNode body) {
        if (body == null) {
            throw new StoreException(StoreErrorCode.FAVORITE_REQUIRED);
        }
        if (!body.isObject()) {
            throw new StoreException(CommonErrorCode.INVALID_REQUEST);
        }
        JsonNode favorite = body.get("favorite");
        if (favorite == null || favorite.isNull()) {
            throw new StoreException(StoreErrorCode.FAVORITE_REQUIRED);
        }
        if (!favorite.isBoolean()) {
            throw new StoreException(StoreErrorCode.INVALID_FAVORITE_TYPE);
        }
        return new StoreFavoriteRequest(favorite.booleanValue());
    }
}
