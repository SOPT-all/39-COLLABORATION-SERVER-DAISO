-- 기존 매장 이미지와 재고의 NULL은 실제 값으로 준비한 뒤 적용한다.
ALTER TABLE store
    MODIFY COLUMN image_url VARCHAR(2048) NOT NULL;

ALTER TABLE store_inventory
    MODIFY COLUMN stock_quantity INT NOT NULL;
