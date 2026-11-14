package org.sopt.daiso.product;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration")
@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class ProductListIntegrationTest {
    private static final String IMAGE = "https://example.com/product.png";
    private static final String NO_IMAGE = "https://example.com/no-image.png";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    private final List<Long> productIds = new ArrayList<>();

    @BeforeEach
    void createProducts() {
        createProduct("00123", "수분 마스크팩", 1000, 9999, IMAGE, false);
        createProduct("MASK-2", "마스크팩 00123", 999, 10000, NO_IMAGE, true);
        createProduct("1023456", "100%_완성\\케이스", 0, 0, IMAGE, false);
        createProduct("TAIL-123", "plain product", 1000, 1000, NO_IMAGE, false);
        createProduct("999900123", "경계 상품", 1000000, 999, IMAGE, false);
    }

    private long createProduct(String number, String name, int price, int reviews, String image, boolean liked) {
        jdbc.update("""
                INSERT INTO product (product_number, name, price, image_url, review_count, is_liked)
                VALUES (?, ?, ?, ?, ?, ?)
                """, number, name, price, image, reviews, liked);
        Long id = jdbc.queryForObject("SELECT id FROM product WHERE product_number = ?", Long.class, number);
        productIds.add(id);
        return id;
    }

    @AfterEach
    void removeProducts() {
        for (Long id : productIds) {
            jdbc.update("DELETE FROM product WHERE id = ?", id);
        }
    }

    @Test
    void returnsAllProductsWithoutInventoryInIdOrder() throws Exception {
        ResultActions response = mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("상품 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.products[0].productNumber").value("00123"))
                .andExpect(jsonPath("$.data.products[0].name").value("수분 마스크팩"))
                .andExpect(jsonPath("$.data.products[0].price").value("1,000원"))
                .andExpect(jsonPath("$.data.products[0].reviewCount").value("9,999"))
                .andExpect(jsonPath("$.data.products[0].imageUrl").value(IMAGE))
                .andExpect(jsonPath("$.data.products[0].liked").value(false))
                .andExpect(jsonPath("$.data.products[1].imageUrl").value(NO_IMAGE))
                .andExpect(jsonPath("$.data.products[1].liked").value(true))
                .andExpect(jsonPath("$.data.products[0].rating").doesNotExist())
                .andExpect(jsonPath("$.data.products[0].new").doesNotExist());
        assertIds(response, productIds);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n", "\u2003"})
    void blankKeywordReturnsAllProducts(String keyword) throws Exception {
        assertIds(search(keyword), productIds);
    }

    @Test
    void trimsKeywordAndMatchesPartOfName() throws Exception {
        assertIds(search("  마스크팩  "), List.of(productIds.get(0), productIds.get(1)));
    }

    @Test
    void numericKeywordMatchesExactNumberOrNameWithoutDuplicatingRows() throws Exception {
        assertIds(search(" 00123 "), List.of(productIds.get(0), productIds.get(1)));
        // 품번의 앞자리 0은 보존하고, 숫자로 된 검색어도 상품명에서 검색한다.
        assertIds(search("123"), List.of(productIds.get(1)));
        search("234").andExpect(jsonPath("$.data.products").isEmpty());
        createProduct("BOTH", "BOTH", 0, 0, IMAGE, false);
        search("BOTH").andExpect(jsonPath("$.data.products.length()").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%", "_", "\\"})
    void searchesSpecialCharactersLiterally(String keyword) throws Exception {
        assertIds(search(keyword), List.of(productIds.get(2)));
    }

    @Test
    void usesDatabaseComparisonRules() throws Exception {
        List<Long> expected = jdbc.queryForList("""
                SELECT id FROM product WHERE name LIKE '%PLAIN%' OR product_number = 'PLAIN' ORDER BY id
                """, Long.class);
        assertIds(search("PLAIN"), expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"일치하지 않는 상품", "' OR 1=1 --"})
    void returnsEmptyArrayWhenNoProductMatches(String keyword) throws Exception {
        search(keyword).andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.products").isEmpty());
    }

    @Test
    void returnsEmptyArrayWhenThereAreNoProducts() throws Exception {
        removeProducts();
        mvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.products").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"0,0,0원,0", "999,999,999원,999", "1000,1000,'1,000원','1,000'",
            "9999,9999,'9,999원','9,999'", "10000,10000,'10,000원',9999+"})
    void formatsPriceAndReviewCountBoundaries(int price, int reviews, String shownPrice, String shownReviews)
            throws Exception {
        String number = UUID.randomUUID().toString();
        createProduct(number, "표시 경계", price, reviews, NO_IMAGE, false);
        search(number).andExpect(jsonPath("$.data.products.length()").value(1))
                .andExpect(jsonPath("$.data.products[0].price").isString())
                .andExpect(jsonPath("$.data.products[0].price").value(shownPrice))
                .andExpect(jsonPath("$.data.products[0].reviewCount").isString())
                .andExpect(jsonPath("$.data.products[0].reviewCount").value(shownReviews));
    }

    @Test
    void reflectsCommittedLikeChangesOnTheNextRead() throws Exception {
        for (boolean liked : new boolean[]{true, false}) {
            mvc.perform(patch("/api/v1/products/{id}/like", productIds.get(0))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"liked\":" + liked + "}"))
                    .andExpect(status().isOk());
            search("수분").andExpect(jsonPath("$.data.products[0].liked").value(liked));
            assertThat(jdbc.queryForObject("SELECT is_liked FROM product WHERE id = ?",
                    Boolean.class, productIds.get(0))).isEqualTo(liked);
        }
    }

    private void assertIds(ResultActions response, List<Long> expected) throws Exception {
        response.andExpect(jsonPath("$.data.products.length()").value(expected.size()));
        for (int index = 0; index < expected.size(); index++) {
            response.andExpect(jsonPath("$.data.products[" + index + "].productId").value(expected.get(index)));
        }
    }

    private ResultActions search(String keyword) throws Exception {
        return mvc.perform(get("/api/v1/products").param("keyword", keyword)).andExpect(status().isOk());
    }
}
