package org.sopt.daiso.product.service;

import java.util.List;
import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.inventory.repository.StoreInventoryRepository;
import org.sopt.daiso.product.dto.ProductLikeResponse;
import org.sopt.daiso.product.dto.ProductListItemResponse;
import org.sopt.daiso.product.dto.ProductListResponse;
import org.sopt.daiso.product.dto.ProductStoresResponse;
import org.sopt.daiso.product.entity.Product;
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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
        return new ProductStoresResponse(productId, ProductListItemResponse.from(product),
                inventoryRepository.findStores(productId, soldOut));
    }

    @Transactional(readOnly = true)
    public ProductListResponse getProducts(String keyword) {
        String search = keyword == null ? "" : keyword.strip();
        List<Product> products = search.isEmpty()
                ? productRepository.findAllByOrderByIdAsc()
                : productRepository.findByNameContainingOrProductNumberOrderByIdAsc(search, search);
        return new ProductListResponse(products.stream().map(ProductListItemResponse::from).toList());
    }

    @Transactional
    public ProductLikeResponse changeLiked(long productId, boolean liked) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
        // 조회한 엔티티는 트랜잭션에서 관리되므로 별도 save 없이 커밋 시 변경이 반영된다.
        product.changeLiked(liked);
        return new ProductLikeResponse(product.getId(), product.isLiked());
    }
}
