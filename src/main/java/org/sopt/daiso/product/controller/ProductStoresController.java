package org.sopt.daiso.product.controller;

import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.product.dto.ProductStoresRequest;
import org.sopt.daiso.product.dto.ProductStoresResponse;
import org.sopt.daiso.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductStoresController {
    private final ProductService productService;

    public ProductStoresController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}/stores")
    public ApiResponse<ProductStoresResponse> findStores(@PathVariable String productId,
            @RequestParam(required = false) String soldOut) {
        ProductStoresRequest request = ProductStoresRequest.from(productId, soldOut);
        return ApiResponse.success("상품의 매장별 재고가 조회되었습니다.",
                productService.findStores(request.productId(), request.soldOut()));
    }
}
