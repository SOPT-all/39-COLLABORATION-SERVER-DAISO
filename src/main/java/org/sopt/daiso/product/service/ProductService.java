package org.sopt.daiso.product.service;

import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.inventory.repository.StoreInventoryRepository;
import org.sopt.daiso.product.dto.ProductStoresResponse;
import org.sopt.daiso.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final StoreInventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, StoreInventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public ProductStoresResponse findStores(long productId, boolean soldOut) {
        if (!productRepository.existsById(productId)) {
            throw new CustomException(ErrorType.PRODUCT_NOT_FOUND);
        }
        return new ProductStoresResponse(productId, inventoryRepository.findStores(productId, soldOut));
    }
}
