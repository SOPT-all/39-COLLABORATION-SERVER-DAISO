package org.sopt.daiso.product.dto;

import java.util.List;

public record ProductStoresResponse(Long productId, ProductListItemResponse product, List<ProductStoreResponse> stores) {
}
