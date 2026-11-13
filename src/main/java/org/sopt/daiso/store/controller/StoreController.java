package org.sopt.daiso.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.global.response.ErrorResponse;
import org.sopt.daiso.store.dto.StoreFavoriteRequest;
import org.sopt.daiso.store.dto.StoreFavoriteResponse;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.store.service.StoreService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "매장")
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PatchMapping(value = "/{storeId}/favorite", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "매장 찜 설정·해제", description = "공유 상태에 요청한 값을 저장합니다. true는 설정, false는 해제이며 재고 조회의 bookmarked에도 반영됩니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StoreFavoriteRequest.class))))
    @ApiResponses({
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공",
                            useReturnTypeSchema = true, content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                                    {"code":"SUCCESS","message":"매장 찜 상태를 저장했습니다.","data":{"storeId":201,"favorite":true}}
                                    """))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                            description = "INVALID_STORE_ID: ID 형식·범위 오류 / FAVORITE_REQUIRED: 본문·favorite 누락 또는 null / "
                                    + "INVALID_FAVORITE_TYPE: favorite 타입 오류 / INVALID_REQUEST: JSON·객체 형식 오류 / "
                                    + "NULL_VALUE_NOT_ALLOWED: DB·JPA 필수값 누락 저장",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(value = "{\"code\":\"FAVORITE_REQUIRED\",\"message\":\"찜 상태는 필수입니다.\"}"))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", ref = "#/components/responses/StoreNotFound"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", ref = "#/components/responses/MethodNotAllowed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", ref = "#/components/responses/UnsupportedMediaType")
            })
    public ApiResponse<StoreFavoriteResponse> changeFavorite(
            @Parameter(description = "매장 ID", required = true,
                    schema = @Schema(type = "integer", format = "int64", minimum = "1", maximum = "9223372036854775807", example = "201"))
            @PathVariable String storeId, @RequestBody(required = false) JsonNode body) {
        long id = parseStoreId(storeId);
        StoreFavoriteRequest request = StoreFavoriteRequest.from(body);
        return ApiResponse.success("매장 찜 상태를 저장했습니다.",
                storeService.changeFavorite(id, request.favorite()));
    }

    private long parseStoreId(String storeId) {
        try {
            if (!storeId.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            long id = Long.parseLong(storeId);
            if (id < 1) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorType.INVALID_STORE_ID);
        }
    }
}
