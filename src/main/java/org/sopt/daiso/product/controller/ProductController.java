package org.sopt.daiso.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.global.response.ErrorResponse;
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
@Tag(name = "상품")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회", description = "페이지네이션 없이 ID 오름차순으로 반환합니다. 결과가 없으면 products는 []입니다.")
    @ApiResponses({
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                            useReturnTypeSchema = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"상품 목록을 조회했습니다.","data":{"products":[{
                                      "productId":1001,"productNumber":"0012345","name":"수분 마스크팩","price":"1,000원",
                                      "imageUrl":"https://example.com/product.png","reviewCount":"9999+","liked":false}]}}
                                    """))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", ref = "#/components/responses/MethodNotAllowed")
            })
    public ApiResponse<ProductListResponse> getProducts(
            @Parameter(description = "선택 검색어. 앞뒤 공백 제거 후 상품명 부분 일치 또는 품번 정확 일치. "
                    + "생략·공백이면 전체 조회하며 %, _도 일반 문자로 검색합니다.", example = "마스크팩")
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success("상품 목록을 조회했습니다.", productService.getProducts(keyword));
    }

    @PatchMapping(value = "/{productId}/like", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "상품 좋아요 설정·해제", description = "공유 상태에 요청한 값을 저장합니다. true는 설정, false는 해제이며 반복 요청은 상태를 반전하지 않습니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductLikeRequest.class))))
    @ApiResponses({
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공",
                            useReturnTypeSchema = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"상품 좋아요 상태를 저장했습니다.","data":{"productId":1001,"liked":true}}
                                    """))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                            description = "INVALID_PRODUCT_ID: ID 형식·범위 오류 / INVALID_REQUEST: 본문·liked 누락, null, 타입·JSON 오류 / "
                                    + "NULL_VALUE_NOT_ALLOWED: DB·JPA 필수값 누락 저장",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = "{\"code\":\"INVALID_REQUEST\",\"message\":\"요청 본문이 올바르지 않습니다.\"}"))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/ProductNotFound"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", ref = "#/components/responses/MethodNotAllowed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", ref = "#/components/responses/UnsupportedMediaType")
            })
    public ApiResponse<ProductLikeResponse> changeLiked(
            @Parameter(description = "상품 ID", required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1", maximum = "9223372036854775807", example = "1001"))
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
