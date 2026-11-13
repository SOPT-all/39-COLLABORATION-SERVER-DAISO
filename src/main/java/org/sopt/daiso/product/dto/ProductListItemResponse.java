package org.sopt.daiso.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Locale;
import org.sopt.daiso.product.entity.Product;

public record ProductListItemResponse(
        @Schema(description = "상품 ID", example = "1001", requiredMode = Schema.RequiredMode.REQUIRED) Long productId,
        @Schema(description = "표시용 품번. 앞자리 0을 유지하는 문자열", example = "0012345",
                requiredMode = Schema.RequiredMode.REQUIRED) String productNumber,
        @Schema(description = "상품명", example = "수분 마스크팩", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "천 단위 쉼표와 원을 포함한 가격", example = "1,000원",
                requiredMode = Schema.RequiredMode.REQUIRED) String price,
        @Schema(description = "실제 또는 이미지 없음 안내 이미지의 URL", example = "https://example.com/product.png",
                requiredMode = Schema.RequiredMode.REQUIRED) String imageUrl,
        @Schema(description = "천 단위 쉼표를 적용한 리뷰 수. 9,999 초과는 9999+", example = "9999+",
                requiredMode = Schema.RequiredMode.REQUIRED) String reviewCount,
        @Schema(description = "저장된 공유 좋아요 상태", example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED) boolean liked) {

    public static ProductListItemResponse from(Product product) {
        return new ProductListItemResponse(product.getId(), product.getProductNumber(), product.getName(),
                String.format(Locale.KOREA, "%,d원", product.getPrice()), product.getImageUrl(),
                product.getReviewCount() > 9999 ? "9999+"
                        : String.format(Locale.KOREA, "%,d", product.getReviewCount()),
                product.isLiked());
    }
}
