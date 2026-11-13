package org.sopt.daiso.product;

import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.sopt.daiso.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@ActiveProfiles("migration")
@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class ProductStoresIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ProductService service;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private final List<Long> products = new ArrayList<>();
    private final List<Long> stores = new ArrayList<>();
    private long productId;
    private long emptyProductId;
    private long availableStore;
    private long soldOutStore;

    @BeforeEach
    void seedData() {
        productId = createProduct();
        emptyProductId = createProduct();
        long otherProduct = createProduct();
        availableStore = createStore("OPEN", true, 850);
        soldOutStore = createStore("CLOSED", false, 1500);
        long unrelatedStore = createStore("OPEN", false, 250);
        jdbc.update("INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, ?, 12, FALSE)", productId, availableStore);
        jdbc.update("INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, ?, 0, TRUE)", productId, soldOutStore);
        jdbc.update("INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, ?, 99, TRUE)", otherProduct, unrelatedStore);
    }

    @AfterEach
    void cleanData() {
        for (Long id : products) {
            jdbc.update("DELETE FROM store_inventory WHERE product_id = ?", id);
            jdbc.update("DELETE FROM product WHERE id = ?", id);
        }
        for (Long id : stores) {
            jdbc.update("DELETE FROM store WHERE id = ?", id);
        }
    }

    @Test
    void defaultsToAvailableStoresAndSeparatesOtherProducts() throws Exception {
        mvc.perform(get("/api/v1/products/{id}/stores", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stores", hasSize(1)))
                .andExpect(jsonPath("$.data.stores[0].storeId").value(availableStore))
                .andExpect(jsonPath("$.data.stores[0].imageUrl").value("https://example.com/store.png"))
                .andExpect(jsonPath("$.data.stores[0].businessStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.stores[0].bookmarked").value(true))
                .andExpect(jsonPath("$.data.stores[0].distanceMeters").value(850))
                .andExpect(jsonPath("$.data.stores[0].stockQuantity").value(12))
                .andExpect(jsonPath("$.data.stores[0].pickupAvailable").value(false));
    }

    @Test
    void includesZeroStockWhenFilterIsFalse() throws Exception {
        mvc.perform(get("/api/v1/products/{id}/stores", productId).param("soldOut", "false"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stores", hasSize(2)))
                .andExpect(jsonPath("$.data.stores[*].stockQuantity", containsInAnyOrder(12, 0)))
                .andExpect(jsonPath("$.data.stores[*].businessStatus", containsInAnyOrder("OPEN", "CLOSED")));
    }

    @Test
    void returnsEmptyForNoInventoryOrOnlySoldOut() throws Exception {
        mvc.perform(get("/api/v1/products/{id}/stores", emptyProductId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stores").isEmpty());
        jdbc.update("UPDATE store_inventory SET stock_quantity = 0 WHERE product_id = ?", productId);
        mvc.perform(get("/api/v1/products/{id}/stores", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stores").isEmpty());
    }

    @Test
    void returns404EvenWithFilterDisabled() throws Exception {
        mvc.perform(get("/api/v1/products/{id}/stores", Long.MAX_VALUE).param("soldOut", "false"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void reflectsFavoriteChangesInBookmarked() throws Exception {
        mvc.perform(patch("/api/v1/stores/{id}/favorite", availableStore)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"favorite\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/products/{id}/stores", productId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stores[0].bookmarked").value(false));
    }

    @Test
    void queryCountDoesNotGrowWithStoreCount() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(service.findStores(productId, false).stores()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        for (int i = 0; i < 5; i++) {
            long store = createStore("OPEN", false, i);
            jdbc.update("INSERT INTO store_inventory (product_id, store_id, stock_quantity, pickup_available) VALUES (?, ?, 1, FALSE)", productId, store);
        }
        statistics.clear();
        assertThat(service.findStores(productId, false).stores()).hasSize(7);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    private long createProduct() {
        String number = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO product (product_number, name, price, image_url, review_count, is_liked) VALUES (?, '상품', 1000, 'https://example.com/product.png', 23, FALSE)", number);
        Long id = jdbc.queryForObject("SELECT id FROM product WHERE product_number = ?", Long.class, number);
        products.add(id);
        return id;
    }

    private long createStore(String status, boolean favorite, long distance) {
        String name = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO store (name, image_url, business_status, is_favorite, distance) VALUES (?, 'https://example.com/store.png', ?, ?, ?)", name, status, favorite, distance);
        Long id = jdbc.queryForObject("SELECT id FROM store WHERE name = ?", Long.class, name);
        stores.add(id);
        return id;
    }
}
