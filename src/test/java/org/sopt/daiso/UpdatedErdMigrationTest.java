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
class UpdatedErdMigrationTest {
    private String database;
    private String url;
    private String user;
    private String password;

    @BeforeEach
    void createDisposableDatabaseAtV1() throws Exception {
        database = "erd_check_" + UUID.randomUUID().toString().replace("-", "");
        user = System.getenv("MIGRATION_DB_USER");
        password = System.getenv("MIGRATION_DB_PASSWORD");
        String baseUrl = System.getenv("MIGRATION_DB_URL");
        try (Connection connection = DriverManager.getConnection(baseUrl, user, password);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + database + "` CHARACTER SET utf8mb4");
        }
        url = baseUrl.replaceFirst("/[^/?]+(?=\\?|$)", "/" + database);
        Flyway.configure().dataSource(url, user, password).target("1").load().migrate();
    }

    @AfterEach
    void removeOnlyCreatedDatabase() throws Exception {
        if (url != null) {
            try (Connection connection = DriverManager.getConnection(System.getenv("MIGRATION_DB_URL"), user, password);
                 Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE `" + database + "`");
            }
        }
    }

    @Test
    void preservesExistingValuesAndRelationshipsAndEnforcesNotNull() throws Exception {
        seedV1(false, false, false);
        migrateV2();
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            try (ResultSet row = statement.executeQuery("""
                    SELECT p.price, p.review_count, p.image_url, s.image_url, i.stock_quantity,
                           s.is_favorite, i.pickup_available
                    FROM store_inventory i JOIN product p ON p.id = i.product_id JOIN store s ON s.id = i.store_id
                    """)) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).isEqualTo("1000");
                assertThat(row.getString(2)).isEqualTo("23");
                assertThat(row.getString(3)).isEqualTo("product.png");
                assertThat(row.getString(4)).isEqualTo("store.png");
                assertThat(row.getInt(5)).isEqualTo(7);
                assertThat(row.getBoolean(6)).isTrue();
                assertThat(row.getBoolean(7)).isTrue();
            }
            for (String sql : new String[]{"UPDATE product SET image_url = NULL", "UPDATE store SET image_url = NULL",
                    "UPDATE store_inventory SET stock_quantity = NULL"}) {
                assertThatThrownBy(() -> statement.executeUpdate(sql)).isInstanceOf(java.sql.SQLException.class);
            }
            statement.executeUpdate("UPDATE product SET price = '1,000원', review_count = '4.8'");
            try (ResultSet row = statement.executeQuery("SELECT price, review_count FROM product")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).isEqualTo("1,000원");
                assertThat(row.getString(2)).isEqualTo("4.8");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "store", "inventory"})
    void rejectsLegacyNullBeforePersistentDdl(String field) throws Exception {
        seedV1(field.equals("product"), field.equals("store"), field.equals("inventory"));
        assertThatThrownBy(this::migrateV2).isInstanceOf(FlywayException.class);
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT DATA_TYPE FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'product' AND column_name = 'price'")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getString(1)).isEqualTo("int");
        }
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery("SELECT COUNT(*) FROM store_inventory WHERE stock_quantity "
                     + (field.equals("inventory") ? "IS NULL" : "= 7"))) {
            assertThat(row.next()).isTrue();
            assertThat(row.getInt(1)).isEqualTo(1);
        }
    }

    private void seedV1(boolean nullProductImage, boolean nullStoreImage, boolean nullStock) throws Exception {
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO product (id, product_number, name, price, image_url, review_count, is_liked) VALUES (1, 'seed', '상품', 1000, "
                    + (nullProductImage ? "NULL" : "'product.png'") + ", 23, FALSE)");
            statement.executeUpdate("INSERT INTO store (id, name, image_url, business_status, is_favorite, distance) VALUES (1, '매장', "
                    + (nullStoreImage ? "NULL" : "'store.png'") + ", 'OPEN', TRUE, 850)");
            statement.executeUpdate("INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (1, 1, "
                    + (nullStock ? "NULL" : "7") + ", TRUE)");
        }
    }

    private void migrateV2() {
        Flyway.configure().dataSource(url, user, password).load().migrate();
    }
}
