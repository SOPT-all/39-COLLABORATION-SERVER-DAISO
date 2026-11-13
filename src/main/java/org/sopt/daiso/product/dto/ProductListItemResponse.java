package org.sopt.daiso.product.dto;

import java.util.Locale;
import org.sopt.daiso.product.entity.Product;

public record ProductListItemResponse(Long productId, String productNumber, String name,
        String price, String imageUrl, String reviewCount, boolean liked) {

    public static ProductListItemResponse from(Product product) {
        return new ProductListItemResponse(product.getId(), product.getProductNumber(), product.getName(),
                String.format(Locale.KOREA, "%,d원", product.getPrice()), product.getImageUrl(),
                product.getReviewCount() > 9999 ? "9999+"
                        : String.format(Locale.KOREA, "%,d", product.getReviewCount()),
                product.isLiked());
    }
}
