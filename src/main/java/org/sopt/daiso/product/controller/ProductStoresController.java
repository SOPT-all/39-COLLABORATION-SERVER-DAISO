package org.sopt.daiso.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.global.response.ErrorResponse;
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
@Tag(name = "상품")
public class ProductStoresController {
    private final ProductService productService;

    public ProductStoresController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}/stores")
    @Operation(summary = "상품별 매장 재고 조회", description = "재고 관계가 있는 매장만 반환합니다. 결과가 없어도 상단 product는 유지하고 stores는 []입니다. "
            + "아래 성공 예시는 soldOut=false일 때 품절 매장도 포함한 결과입니다.")
    @ApiResponses({
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                            useReturnTypeSchema = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"상품의 매장별 재고가 조회되었습니다.","data":{
                                      "productId":1001,"product":{"productId":1001,"productNumber":"0012345","name":"수분 마스크팩",
                                        "price":"1,000원","imageUrl":"https://example.com/product.png","reviewCount":"9999+","liked":false},
                                      "stores":[{"storeId":201,"storeName":"강남본점","imageUrl":"https://example.com/store.png",
                                        "businessStatus":"CLOSED","bookmarked":true,"distanceMeters":1500,"distance":"1.5km",
                                        "stockQuantity":0,"pickupAvailable":false}]}}
                                    """))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                            description = "INVALID_PRODUCT_ID: ID 형식·범위 오류 / INVALID_EXCLUDE_SOLD_OUT: soldOut 허용값·중복 입력 오류",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = "{\"code\":\"INVALID_EXCLUDE_SOLD_OUT\",\"message\":\"품절 제외 여부는 true 또는 false여야 합니다.\"}"))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", ref = "#/components/responses/StorageUnavailable"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", ref = "#/components/responses/MethodNotAllowed")
            })
    public ApiResponse<ProductStoresResponse> findStores(
            @Parameter(description = "상품 ID", required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1", maximum = "9223372036854775807", example = "1001"))
            @PathVariable String productId,
            @Parameter(description = "품절 제외 여부. true는 재고 1 이상만, false는 재고 관계가 있는 품절 매장도 조회. "
                    + "소문자 true·false만 허용하고 빈 값·중복 입력은 400입니다.", schema = @Schema(type = "boolean", defaultValue = "true"))
            @RequestParam(required = false) String soldOut) {
        ProductStoresRequest request = ProductStoresRequest.from(productId, soldOut);
        return ApiResponse.success("상품의 매장별 재고가 조회되었습니다.",
                productService.findStores(request.productId(), request.soldOut()));
    }
}
