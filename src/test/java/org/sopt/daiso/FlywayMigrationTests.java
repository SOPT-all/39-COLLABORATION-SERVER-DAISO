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
    void migrationsAreAppliedSuccessfully() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version IN ('1', '2') AND success = 1",
                Integer.class)).isEqualTo(2);
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
                VALUES ('migration-check', '검증 상품', '1000', 'https://example.com/product.png', '0', FALSE)
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
                VALUES (?, ?, 0, TRUE)
                """, productId, storeId);
        assertThat(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).isPositive();
        assertThat(jdbc.queryForObject("""
                SELECT stock_quantity FROM store_inventory WHERE product_id = ? AND store_id = ?
                """, Integer.class, productId, storeId)).isZero();

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, ?, 0, FALSE)
                """, productId, storeId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (-1, ?, 0, FALSE)
                """, storeId)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, -1, 0, FALSE)
                """, productId)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
