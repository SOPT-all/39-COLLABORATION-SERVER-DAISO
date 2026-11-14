package org.sopt.daiso.product;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.sopt.daiso.product.entity.Product;
import org.sopt.daiso.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration")
@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class ProductLikeIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProductService service;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    private Long productId;
    private Long otherProductId;
    private String productNumber;

    @BeforeEach
    void createProducts() {
        productNumber = UUID.randomUUID().toString();
        productId = createProduct(productNumber);
        otherProductId = createProduct(UUID.randomUUID().toString());
    }

    private Long createProduct(String number) {
        jdbc.update("""
                INSERT INTO product (product_number, name, price, image_url, review_count, is_liked)
                VALUES (?, '검증 상품', 1500, 'https://example.com/original.png', 12, FALSE)
                """, number);
        return jdbc.queryForObject("SELECT id FROM product WHERE product_number = ?", Long.class, number);
    }

    @AfterEach
    void removeProducts() {
        if (productId != null) {
            jdbc.update("DELETE FROM product WHERE id = ?", productId);
        }
        if (otherProductId != null) {
            jdbc.update("DELETE FROM product WHERE id = ?", otherProductId);
        }
    }

    @Test
    void commitsSetUnsetAndRepeatedRequestsWithoutChangingOtherData() throws Exception {
        for (boolean liked : new boolean[]{false, true, true, false, false}) {
            mvc.perform(patch("/api/v1/products/{id}/like", productId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"liked\":" + liked + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.productId").value(productId))
                    .andExpect(jsonPath("$.data.liked").value(liked));
            assertThat(savedLiked(productId)).isEqualTo(liked);
        }
        assertThat(jdbc.queryForMap("""
                SELECT product_number, name, price, image_url, review_count FROM product WHERE id = ?
                """, productId))
                .containsEntry("product_number", productNumber)
                .containsEntry("name", "검증 상품")
                .containsEntry("price", 1500)
                .containsEntry("image_url", "https://example.com/original.png")
                .containsEntry("review_count", 12);
        assertThat(savedLiked(otherProductId)).isFalse();
    }

    @Test
    void rejectedRequestsPreserveSavedState() throws Exception {
        service.changeLiked(productId, true);
        for (String body : new String[]{"", "{}", "{\"liked\":null}", "{\"liked\":0}", "[]", "{"}) {
            mvc.perform(patch("/api/v1/products/{id}/like", productId)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
            assertThat(savedLiked(productId)).isTrue();
        }
        mvc.perform(patch("/api/v1/products/{id}/like", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"liked\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
        assertThat(savedLiked(productId)).isTrue();
    }

    @Test
    void rollsBackFlushedLikeWhenTransactionFails() {
        // SQL을 DB에 보낸 뒤 실패해도 커밋되지 않은 좋아요 변경은 롤백되어야 한다.
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.changeLiked(productId, true);
            entityManager.flush();
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(savedLiked(productId)).isFalse();
    }

    @Test
    void preservesAnotherTransactionsProductChanges() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        outer.executeWithoutResult(status -> {
            Product product = entityManager.find(Product.class, productId);
            // 기존 엔티티가 읽힌 이후 다른 트랜잭션이 상품 정보를 수정하는 상황을 만든다.
            concurrent.executeWithoutResult(other -> jdbc.update("""
                    UPDATE product SET name = '변경 상품', price = 3000,
                    image_url = 'https://example.com/product.png', review_count = 25 WHERE id = ?
                    """, productId));
            product.changeLiked(true);
        });
        assertThat(savedLiked(productId)).isTrue();
        assertThat(jdbc.queryForMap("SELECT name, price, image_url, review_count FROM product WHERE id = ?", productId))
                .containsEntry("name", "변경 상품")
                .containsEntry("price", 3000)
                .containsEntry("image_url", "https://example.com/product.png")
                .containsEntry("review_count", 25);
        assertThat(savedLiked(otherProductId)).isFalse();
    }

    private boolean savedLiked(Long id) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT is_liked FROM product WHERE id = ?", Boolean.class, id));
    }
}
