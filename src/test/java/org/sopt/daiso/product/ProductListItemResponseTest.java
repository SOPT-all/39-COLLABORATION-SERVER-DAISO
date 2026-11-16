package org.sopt.daiso.product;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sopt.daiso.product.dto.ProductListItemResponse;
import org.sopt.daiso.product.entity.Product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductListItemResponseTest {
    @ParameterizedTest
    @CsvSource({"0,0,0,0", "999,999,999,999", "1000,1000,'1,000','1,000'",
            "2000,9999,'2,000','9,999'", "10000,10000,'10,000','9,999+'",
            "1000000,2147483647,'1,000,000','9,999+'"})
    void formatsPriceWithoutUnitAndReviewCountWithCommas(
            int price, int reviews, String shownPrice, String shownReviews) {
        Product product = mock(Product.class);
        when(product.getPrice()).thenReturn(price);
        when(product.getReviewCount()).thenReturn(reviews);

        ProductListItemResponse response = ProductListItemResponse.from(product);

        assertThat(response.price()).isEqualTo(shownPrice);
        assertThat(response.reviewCount()).isEqualTo(shownReviews);
    }
}
