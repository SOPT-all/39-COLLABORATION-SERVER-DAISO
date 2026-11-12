package org.sopt.daiso.store.controller;

import org.sopt.daiso.global.response.ApiResponse;
import org.sopt.daiso.store.dto.StoreFavoriteRequest;
import org.sopt.daiso.store.dto.StoreFavoriteResponse;
import org.sopt.daiso.global.exception.StoreErrorCode;
import org.sopt.daiso.global.exception.StoreException;
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
public class StoreController {
    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @PatchMapping(value = "/{storeId}/favorite", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<StoreFavoriteResponse> changeFavorite(
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
            throw new StoreException(StoreErrorCode.INVALID_STORE_ID);
        }
    }
}
