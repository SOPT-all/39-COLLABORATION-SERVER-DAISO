package org.sopt.daiso.product.service;

import org.sopt.daiso.global.exception.CustomException;
import org.sopt.daiso.global.exception.ErrorType;
import org.sopt.daiso.product.dto.ProductLikeResponse;
import org.sopt.daiso.product.entity.Product;
import org.sopt.daiso.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
