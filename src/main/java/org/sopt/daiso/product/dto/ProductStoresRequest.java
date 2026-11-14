package org.sopt.daiso.product.dto;

import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;

public record ProductStoresRequest(long productId, boolean soldOut) {
    public static ProductStoresRequest from(String productId, String soldOut) {
        long id;
        try {
            if (!productId.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            id = Long.parseLong(productId);
            if (id < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorType.INVALID_PRODUCT_ID);
        }
        if (soldOut == null || "true".equals(soldOut)) {
            return new ProductStoresRequest(id, true);
        }
        if ("false".equals(soldOut)) {
            return new ProductStoresRequest(id, false);
        }
        throw new CustomException(ErrorType.INVALID_EXCLUDE_SOLD_OUT);
    }
}
