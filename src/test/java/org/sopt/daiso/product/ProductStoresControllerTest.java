package org.sopt.daiso.product;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.global.exception.GlobalExceptionHandler;
import org.sopt.daiso.global.exception.InventoryStorageExceptionHandler;
import org.sopt.daiso.product.controller.ProductStoresController;
import org.sopt.daiso.product.dto.ProductStoreResponse;
import org.sopt.daiso.product.dto.ProductStoresResponse;
import org.sopt.daiso.product.service.ProductService;
import org.sopt.daiso.store.entity.BusinessStatus;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductStoresControllerTest {
    private ProductService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ProductService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProductStoresController(service))
                .setControllerAdvice(new InventoryStorageExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @Test
    void defaultsToExcludingSoldOutAndReturnsContractFields() throws Exception {
        when(service.findStores(1001, true)).thenReturn(new ProductStoresResponse(1001L, List.of(
                new ProductStoreResponse(201L, "강남본점", "https://example.com/store.png", BusinessStatus.OPEN, true, 850, 12, false))));
        mvc.perform(get("/api/v1/products/1001/stores"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":"SUCCESS","message":"상품의 매장별 재고가 조회되었습니다.",
                        "data":{"productId":1001,"stores":[{"storeId":201,"storeName":"강남본점",
                        "imageUrl":"https://example.com/store.png","businessStatus":"OPEN","bookmarked":true,
                        "distanceMeters":850,"distance":"850m","stockQuantity":12,"pickupAvailable":false}]}}
                        """));
        verify(service).findStores(1001, true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptsOnlyBooleanFilterAndReturnsEmptyArray(boolean soldOut) throws Exception {
        when(service.findStores(1001, soldOut)).thenReturn(new ProductStoresResponse(1001L, List.of()));
        mvc.perform(get("/api/v1/products/1001/stores").param("soldOut", String.valueOf(soldOut)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.stores").isEmpty());
        verify(service).findStores(1001, soldOut);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "TRUE", "False", "1", "0", "yes", "null", " true ", "true,false"})
    void rejectsInvalidFilter(String soldOut) throws Exception {
        mvc.perform(get("/api/v1/products/1001/stores").param("soldOut", soldOut))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EXCLUDE_SOLD_OUT"))
                .andExpect(jsonPath("$.message").value("품절 제외 여부는 true 또는 false여야 합니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsRepeatedFilter() throws Exception {
        mvc.perform(get("/api/v1/products/1001/stores").param("soldOut", "true", "false"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EXCLUDE_SOLD_OUT"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "+1", "9223372036854775808", "-9223372036854775809"})
    void rejectsInvalidProductId(String id) throws Exception {
        mvc.perform(get("/api/v1/products/{id}/stores", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 상품 ID입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void acceptsInt64Boundaries(long id) throws Exception {
        when(service.findStores(id, true)).thenReturn(new ProductStoresResponse(id, List.of()));
        mvc.perform(get("/api/v1/products/{id}/stores", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.productId").value(id));
    }

    @Test
    void returnsNotFoundForMissingProduct() throws Exception {
        when(service.findStores(1001, true)).thenThrow(new CustomException(ErrorType.PRODUCT_NOT_FOUND));
        assertFailure(404, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다.");
    }

    @Test
    void mapsStorageAndTransactionFailuresTo503() throws Exception {
        for (RuntimeException failure : List.of(new DataAccessResourceFailureException("private detail"),
                new CannotCreateTransactionException("private detail"), new QueryTimeoutException("private detail"),
                new TransientDataAccessResourceException("private detail"))) {
            doThrow(failure).when(service).findStores(1001, true);
            assertFailure(503, "STORAGE_UNAVAILABLE", "일시적으로 매장별 재고를 조회할 수 없습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    @Test
    void mapsUnexpectedFailureTo500() throws Exception {
        when(service.findStores(1001, true)).thenThrow(new IllegalStateException("private detail"));
        assertFailure(500, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");
    }

    @ParameterizedTest
    @CsvSource({"0, 0m", "850, 850m", "999, 999m", "1000, 1km", "1001, 1km",
            "1500, 1.5km", "1549, 1.5km", "1550, 1.6km", "2000, 2km", "12500, 12.5km"})
    void returnsDisplayDistanceAndPreservesMeters(long meters, String distance) throws Exception {
        when(service.findStores(1001, true)).thenReturn(new ProductStoresResponse(1001L, List.of(
                new ProductStoreResponse(201L, "매장", "store.png", BusinessStatus.OPEN, false, meters, 1, false))));
        mvc.perform(get("/api/v1/products/1001/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores[0].distanceMeters").value(meters))
                .andExpect(jsonPath("$.data.stores[0].distance").value(distance));
    }
    @Test
    void returnsClosedSoldOutAndPickupUnsupportedValues() throws Exception {
        when(service.findStores(1001, false)).thenReturn(new ProductStoresResponse(1001L, List.of(
                new ProductStoreResponse(201L, "매장", "store.png", BusinessStatus.CLOSED, false, 850, 0, false))));
        mvc.perform(get("/api/v1/products/1001/stores").param("soldOut", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stores[0].businessStatus").value("CLOSED"))
                .andExpect(jsonPath("$.data.stores[0].stockQuantity").value(0))
                .andExpect(jsonPath("$.data.stores[0].pickupAvailable").value(false));
    }
    private void assertFailure(int status, String code, String message) throws Exception {
        mvc.perform(get("/api/v1/products/1001/stores"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message)).andExpect(jsonPath("$.data").doesNotExist());
    }
}
