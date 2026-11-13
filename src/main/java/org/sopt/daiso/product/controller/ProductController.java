package org.sopt.daiso.product.controller;

import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.product.dto.ProductLikeRequest;
import org.sopt.daiso.product.dto.ProductLikeResponse;
import org.sopt.daiso.product.dto.ProductListResponse;
import org.sopt.daiso.product.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ApiResponse<ProductListResponse> getProducts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success("상품 목록을 조회했습니다.", productService.getProducts(keyword));
    }

    @PatchMapping(value = "/{productId}/like", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<ProductLikeResponse> changeLiked(
            @PathVariable String productId, @RequestBody(required = false) JsonNode body) {
        long id = parseProductId(productId);
        ProductLikeRequest request = ProductLikeRequest.from(body);
        return ApiResponse.success("상품 좋아요 상태를 저장했습니다.",
                productService.changeLiked(id, request.liked()));
    }

    private long parseProductId(String productId) {
        // 형식·범위 오류를 Spring 기본 응답 대신 계약의 INVALID_PRODUCT_ID로 통일한다.
        try {
            if (!productId.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            long id = Long.parseLong(productId);
            if (id < 1) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorType.INVALID_PRODUCT_ID);
        }
    }
}
