package org.sopt.daiso.store;

import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.sopt.daiso.store.entity.Store;
import org.sopt.daiso.store.service.StoreService;
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("migration")
@EnabledIfEnvironmentVariable(named = "MIGRATION_DB_URL", matches = ".+")
class StoreFavoriteIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StoreService service;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    private Long storeId;

    @BeforeEach
    void createStore() {
        String name = "favorite-test-" + UUID.randomUUID();
        jdbc.update("INSERT INTO store (name, image_url, business_status, is_favorite, distance) VALUES (?, ?, ?, FALSE, ?)",
                name, "https://example.com/store.png", "OPEN", 123L);
        storeId = jdbc.queryForObject("SELECT id FROM store WHERE name = ?", Long.class, name);
    }

    @AfterEach
    void removeStore() {
        if (storeId != null) {
            jdbc.update("DELETE FROM store WHERE id = ?", storeId);
        }
    }

    @Test
    void commitsSetUnsetAndRepeatedRequests() throws Exception {
        for (boolean favorite : new boolean[]{false, true, true, false, false}) {
            mvc.perform(patch("/api/v1/stores/{id}/favorite", storeId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"favorite\":" + favorite + "}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.storeId").value(storeId))
                    .andExpect(jsonPath("$.data.favorite").value(favorite));
            assertThat(savedFavorite()).isEqualTo(favorite);
        }
        assertThat(jdbc.queryForMap("SELECT image_url, business_status, distance FROM store WHERE id = ?", storeId))
                .containsEntry("image_url", "https://example.com/store.png")
                .containsEntry("business_status", "OPEN").containsEntry("distance", 123L);
    }

    @Test
    void rejectedRequestsPreserveSavedState() throws Exception {
        service.changeFavorite(storeId, true);
        for (String body : new String[]{"", "{}", "{\"favorite\":null}", "{\"favorite\":0}", "[]", "{"}) {
            mvc.perform(patch("/api/v1/stores/{id}/favorite", storeId)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
            assertThat(savedFavorite()).isTrue();
        }
        mvc.perform(patch("/api/v1/stores/{id}/favorite", Long.MAX_VALUE)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"favorite\":false}"))
                .andExpect(status().isNotFound());
        assertThat(savedFavorite()).isTrue();
    }

    @Test
    void rollsBackFlushedFavoriteWhenTransactionFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.changeFavorite(storeId, true);
            entityManager.flush();
            throw new IllegalStateException("force rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(savedFavorite()).isFalse();
    }

    @Test
    void preservesAnotherTransactionsStoreChanges() {
        TransactionTemplate outer = new TransactionTemplate(transactionManager);
        TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        outer.executeWithoutResult(status -> {
            Store store = entityManager.find(Store.class, storeId);
            concurrent.executeWithoutResult(other -> jdbc.update(
                    "UPDATE store SET business_status = 'CLOSED', distance = 456 WHERE id = ?", storeId));
            store.changeFavorite(true);
        });
        assertThat(savedFavorite()).isTrue();
        assertThat(jdbc.queryForMap("SELECT business_status, distance FROM store WHERE id = ?", storeId))
                .containsEntry("business_status", "CLOSED").containsEntry("distance", 456L);
    }

    private boolean savedFavorite() {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT is_favorite FROM store WHERE id = ?", Boolean.class, storeId));
    }
}
