package org.sopt.daiso.product.dto;

import java.util.List;

public record ProductListResponse(List<ProductListItemResponse> products) {
}
