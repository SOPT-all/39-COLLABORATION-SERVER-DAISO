package org.sopt.daiso;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class StoreInventoryMigrationTest {
    private String database;
    private String url;
    private String user;
    private String password;

    @BeforeEach
    void createDisposableDatabaseAtV2() throws Exception {
        database = "inventory_check_" + UUID.randomUUID().toString().replace("-", "");
        user = System.getenv("MIGRATION_DB_USER");
        password = System.getenv("MIGRATION_DB_PASSWORD");
        try (Connection connection = connection(System.getenv("MIGRATION_DB_URL"));
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4");
        }
        url = System.getenv("MIGRATION_DB_URL").replaceFirst("/[^/?]+(?=\\?|$)", "/" + database);
        Flyway.configure().dataSource(url, user, password).target("2").load().migrate();
    }

    @AfterEach
    void removeOnlyCreatedDatabase() throws Exception {
        if (database != null) {
            try (Connection connection = connection(System.getenv("MIGRATION_DB_URL"));
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + database + "`");
            }
        }
    }

    @Test
    void preservesProductsStoresAndInventory() throws Exception {
        seedV2(false, false);
        migrateV3();
        try (Connection connection = connection(url); Statement statement = connection.createStatement()) {
            try (ResultSet row = statement.executeQuery("""
                    SELECT p.price, p.review_count, p.is_liked, p.image_url, s.image_url,
                           s.is_favorite, s.business_status, s.distance, i.stock_quantity, i.pickup_available
                    FROM store_inventory i JOIN product p ON p.id = i.product_id JOIN store s ON s.id = i.store_id
                    """)) {
                assertThat(row.next()).isTrue();
                assertThat(row.getInt(1)).isEqualTo(1000);
                assertThat(row.getInt(2)).isEqualTo(23);
                assertThat(row.getBoolean(3)).isTrue();
                assertThat(row.getString(4)).isEqualTo("product.png");
                assertThat(row.getString(5)).isEqualTo("store.png");
                assertThat(row.getBoolean(6)).isTrue();
                assertThat(row.getString(7)).isEqualTo("OPEN");
                assertThat(row.getLong(8)).isEqualTo(850);
                assertThat(row.getInt(9)).isEqualTo(7);
                assertThat(row.getBoolean(10)).isFalse();
                assertThat(row.next()).isFalse();
            }
            try (ResultSet columns = statement.executeQuery("""
                    SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE()
                    AND table_name = 'product' AND column_name IN ('price', 'review_count')
                    """)) {
                int count = 0;
                while (columns.next()) {
                    assertThat(columns.getString(1)).isEqualTo("int");
                    count++;
                }
                assertThat(count).isEqualTo(2);
            }
            try (ResultSet column = statement.executeQuery("""
                    SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE()
                    AND table_name = 'store' AND column_name = 'business_status'
                    """)) {
                assertThat(column.next()).isTrue();
                assertThat(column.getString(1)).isEqualTo("varchar");
            }
            for (String sql : new String[]{"UPDATE store SET image_url = NULL",
                    "UPDATE store_inventory SET stock_quantity = NULL"}) {
                assertThatThrownBy(() -> statement.executeUpdate(sql)).isInstanceOf(java.sql.SQLException.class);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"store", "inventory"})
    void rejectsLegacyNullWithoutFillingMissingValues(String field) throws Exception {
        seedV2(field.equals("store"), field.equals("inventory"));
        assertThatThrownBy(this::migrateV3).isInstanceOf(FlywayException.class);
        String sql = field.equals("store") ? "SELECT image_url FROM store" : "SELECT stock_quantity FROM store_inventory";
        try (Connection connection = connection(url); Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(sql)) {
            assertThat(row.next()).isTrue();
            assertThat(row.getObject(1)).isNull();
        }
    }

    private void seedV2(boolean nullImage, boolean nullStock) throws Exception {
        try (Connection connection = connection(url); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO product (id, product_number, name, price, image_url, review_count, is_liked) VALUES (1, 'seed', 'product', 1000, 'product.png', 23, TRUE)");
            statement.executeUpdate("INSERT INTO store (id, name, image_url, business_status, is_favorite, distance) VALUES (1, 'store', "
                    + (nullImage ? "NULL" : "'store.png'") + ", 'OPEN', TRUE, 850)");
            statement.executeUpdate("INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (1, 1, "
                    + (nullStock ? "NULL" : "7") + ", FALSE)");
        }
    }

    private Connection connection(String databaseUrl) throws Exception {
        return DriverManager.getConnection(databaseUrl, user, password);
    }

    private void migrateV3() {
        Flyway.configure().dataSource(url, user, password).load().migrate();
    }
}
