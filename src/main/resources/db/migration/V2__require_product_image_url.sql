-- 기존 상품의 누락 이미지는 실제 또는 대체 이미지 URL로 준비한 뒤 적용한다.
ALTER TABLE product MODIFY COLUMN image_url VARCHAR(2048) NOT NULL;
