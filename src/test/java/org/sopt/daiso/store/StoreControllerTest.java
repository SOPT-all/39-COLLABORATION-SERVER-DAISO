package org.sopt.daiso.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.daiso.global.exception.GlobalExceptionHandler;
import org.sopt.daiso.global.exception.InventoryStorageExceptionHandler;
import org.sopt.daiso.store.controller.StoreController;
import org.sopt.daiso.store.dto.StoreFavoriteResponse;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.store.service.StoreService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StoreControllerTest {
    private StoreService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(StoreService.class);
        mvc = MockMvcBuilders.standaloneSetup(new StoreController(service))
                .setControllerAdvice(new InventoryStorageExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsSavedFavorite(boolean favorite) throws Exception {
        when(service.changeFavorite(201, favorite)).thenReturn(new StoreFavoriteResponse(201L, favorite));
        mvc.perform(patch("/api/v1/stores/201/favorite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":" + favorite + "}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":\"SUCCESS\",\"message\":\"매장 찜 상태를 저장했습니다.\","
                        + "\"data\":{\"storeId\":201,\"favorite\":" + favorite + "}}"));
        verify(service).changeFavorite(201, favorite);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"favorite\":null}"})
    void requiresFavorite(String body) throws Exception {
        assertError(body, "FAVORITE_REQUIRED", "찜 상태는 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"true\"", "\"false\"", "1", "0", "1.0", "[]", "{}"})
    void rejectsNonBooleanFavorite(String value) throws Exception {
        assertError("{\"favorite\":" + value + "}", "INVALID_FAVORITE_TYPE",
                "찜 상태는 true 또는 false여야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"favorite\":true", "[]", "true", "1", "\"text\"", "null",
            "{\"favorite\":true} trailing", "{\"favorite\":true} {}"})
    void rejectsInvalidJsonOrNonObject(String body) throws Exception {
        assertError(body, "INVALID_REQUEST", "요청 본문이 올바르지 않습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808", "-9223372036854775809"})
    void rejectsInvalidStoreId(String id) throws Exception {
        mvc.perform(patch("/api/v1/stores/{storeId}/favorite", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STORE_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 매장 ID입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void acceptsInt64Boundaries(long id) throws Exception {
        when(service.changeFavorite(id, true)).thenReturn(new StoreFavoriteResponse(id, true));
        mvc.perform(patch("/api/v1/stores/{storeId}/favorite", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.storeId").value(id));
    }

    @Test
    void returnsStoreNotFound() throws Exception {
        when(service.changeFavorite(201, true)).thenThrow(new CustomException(ErrorType.STORE_NOT_FOUND));
        mvc.perform(patch("/api/v1/stores/201/favorite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"code\":\"STORE_NOT_FOUND\",\"message\":\"매장을 찾을 수 없습니다.\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void hidesInternalErrorDetails() throws Exception {
        when(service.changeFavorite(201, true)).thenThrow(new IllegalStateException("private database detail"));
        mvc.perform(patch("/api/v1/stores/201/favorite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsCommonErrorsAndPreservesFrameworkHeaders() throws Exception {
        mvc.perform(patch("/api/v1/stores/201/favorite").contentType(MediaType.TEXT_PLAIN)
                        .content("favorite=true"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 Content-Type입니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/stores/201/favorite"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("PATCH")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
        verifyNoInteractions(service);
    }
    @Test
    void inventoryStorageAdviceDoesNotChangeFavoriteErrorContract() throws Exception {
        when(service.changeFavorite(201, true)).thenThrow(
                new org.springframework.transaction.CannotCreateTransactionException("private detail"));
        mvc.perform(patch("/api/v1/stores/201/favorite").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favorite\":true}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }
    private void assertError(String body, String code, String message) throws Exception {
        mvc.perform(patch("/api/v1/stores/201/favorite").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(service);
    }
}
