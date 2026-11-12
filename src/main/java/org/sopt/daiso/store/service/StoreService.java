package org.sopt.daiso.store.service;

import org.sopt.daiso.store.dto.StoreFavoriteResponse;
import org.sopt.daiso.store.entity.Store;
import org.sopt.daiso.store.exception.StoreErrorCode;
import org.sopt.daiso.store.exception.StoreException;
import org.sopt.daiso.store.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {
    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional
    public StoreFavoriteResponse changeFavorite(long storeId, boolean favorite) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreException(StoreErrorCode.STORE_NOT_FOUND));
        store.changeFavorite(favorite);
        return new StoreFavoriteResponse(store.getId(), store.isFavorite());
    }
}
