package org.sopt.daiso.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.daiso.global.exception.GlobalExceptionHandler;
import org.sopt.daiso.global.exception.InventoryStorageExceptionHandler;
import org.sopt.daiso.product.controller.ProductController;
import org.sopt.daiso.product.dto.ProductLikeResponse;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest {
    private ProductService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ProductService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(service))
                .setControllerAdvice(new InventoryStorageExceptionHandler(), new GlobalExceptionHandler()).build();
    }

    @Test
    void inventoryStorageAdvicePreservesProductErrorContracts() throws Exception {
        when(service.getProducts(null)).thenThrow(new org.springframework.dao.QueryTimeoutException("private detail"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        when(service.changeLiked(201, true)).thenThrow(
                new org.springframework.transaction.CannotCreateTransactionException("private detail"));
        mvc.perform(patch("/api/v1/products/201/like").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"liked\":true}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsSavedLiked(boolean liked) throws Exception {
        when(service.changeLiked(201, liked)).thenReturn(new ProductLikeResponse(201L, liked));
        mvc.perform(patch("/api/v1/products/201/like").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"liked\":" + liked + "}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":\"SUCCESS\",\"message\":\"상품 좋아요 상태를 저장했습니다.\","
                        + "\"data\":{\"productId\":201,\"liked\":" + liked + "}}"));
        verify(service).changeLiked(201, liked);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "{\"liked\":null}"})
    void requiresLiked(String body) throws Exception {
        assertError(body, "INVALID_REQUEST", "요청 본문이 올바르지 않습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"true\"", "\"false\"", "1", "0", "1.0", "[]", "{}"})
    void rejectsNonBooleanLiked(String value) throws Exception {
        assertError("{\"liked\":" + value + "}", "INVALID_REQUEST",
                "요청 본문이 올바르지 않습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"liked\":true", "[]", "true", "1", "\"text\"", "null",
            "{\"liked\":true} trailing", "{\"liked\":true} {}"})
    void rejectsInvalidJsonOrNonObject(String body) throws Exception {
        assertError(body, "INVALID_REQUEST", "요청 본문이 올바르지 않습니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "1.5", "9223372036854775808", "-9223372036854775809"})
    void rejectsInvalidProductId(String id) throws Exception {
        mvc.perform(patch("/api/v1/products/{productId}/like", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"liked\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRODUCT_ID"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 상품 ID입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, Long.MAX_VALUE})
    void acceptsInt64Boundaries(long id) throws Exception {
        when(service.changeLiked(id, true)).thenReturn(new ProductLikeResponse(id, true));
        mvc.perform(patch("/api/v1/products/{productId}/like", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"liked\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.productId").value(id));
    }

    @Test
    void returnsProductNotFound() throws Exception {
        when(service.changeLiked(201, true)).thenThrow(new CustomException(ErrorType.PRODUCT_NOT_FOUND));
        mvc.perform(patch("/api/v1/products/201/like").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"liked\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"code\":\"PRODUCT_NOT_FOUND\",\"message\":\"상품을 찾을 수 없습니다.\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void hidesInternalErrorDetails() throws Exception {
        when(service.changeLiked(201, true)).thenThrow(new IllegalStateException("private database detail"));
        mvc.perform(patch("/api/v1/products/201/like").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"liked\":true}"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void hidesListQueryFailureDetails() throws Exception {
        when(service.getProducts(null)).thenThrow(new IllegalStateException("private database detail"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/products"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"서버 내부 오류가 발생했습니다.\"}"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsCommonErrorsAndPreservesFrameworkHeaders() throws Exception {
        mvc.perform(patch("/api/v1/products/201/like").contentType(MediaType.TEXT_PLAIN)
                        .content("liked=true"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 Content-Type입니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/products/201/like"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("PATCH")))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
        verifyNoInteractions(service);
    }
    private void assertError(String body, String code, String message) throws Exception {
        mvc.perform(patch("/api/v1/products/201/like").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.data").doesNotExist());
        verifyNoInteractions(service);
    }
}
