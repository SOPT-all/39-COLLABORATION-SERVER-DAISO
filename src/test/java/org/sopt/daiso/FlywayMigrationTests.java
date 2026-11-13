package org.sopt.daiso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("migration")
@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class FlywayMigrationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void initialMigrationIsAppliedSuccessfully() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '2' AND success = 1",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '3' AND success = 1",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name IN ('product', 'store', 'store_inventory')
                """, String.class)).containsExactlyInAnyOrder("product", "store", "store_inventory");
    }

    @Test
    @Transactional
    void generatedIdsAndInventoryConstraintsWork() {
        jdbc.update("""
                INSERT INTO product (product_number, name, price, image_url, review_count, is_liked)
                VALUES ('migration-check', '검증 상품', 1000, 'https://example.com/product.png', 0, FALSE)
                """);
        Long productId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO store (name, image_url, business_status, is_favorite, distance)
                VALUES ('검증 매장', 'https://example.com/store.png', 'OPEN', FALSE, 100)
                """);
        Long storeId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertThat(productId).isPositive();
        assertThat(storeId).isPositive();

        jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available)
                VALUES (?, ?, 7, TRUE)
                """, productId, storeId);
        assertThat(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).isPositive();
        assertThat(jdbc.queryForObject("""
                SELECT stock_quantity FROM store_inventory WHERE product_id = ? AND store_id = ?
                """, Integer.class, productId, storeId)).isEqualTo(7);

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, ?, 1, FALSE)
                """, productId, storeId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (-1, ?, 1, FALSE)
                """, storeId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, -1, 1, FALSE)
                """, productId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE store SET image_url = NULL WHERE id = ?", storeId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE store_inventory SET stock_quantity = NULL WHERE product_id = ?", productId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional
    void productImageIsRequired() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO product (product_number, name, price, image_url, review_count, is_liked)
                VALUES ('missing-image-check', '이미지 누락', 1000, NULL, 0, FALSE)
                """)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
