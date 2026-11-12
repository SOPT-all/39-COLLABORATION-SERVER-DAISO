-- Abort before persistent DDL if any required value is NULL. Never invent image URLs or stock.
CREATE TEMPORARY TABLE erd_v2_null_guard (
    null_count BIGINT NOT NULL,
    CONSTRAINT chk_erd_v2_no_null CHECK (null_count = 0)
);
INSERT INTO erd_v2_null_guard (null_count)
SELECT (SELECT COUNT(*) FROM product WHERE image_url IS NULL)
     + (SELECT COUNT(*) FROM store WHERE image_url IS NULL)
     + (SELECT COUNT(*) FROM store_inventory WHERE stock_quantity IS NULL);
DROP TEMPORARY TABLE erd_v2_null_guard;

ALTER TABLE product
    MODIFY COLUMN price VARCHAR(255) NOT NULL,
    MODIFY COLUMN review_count VARCHAR(255) NOT NULL,
    MODIFY COLUMN image_url VARCHAR(2048) NOT NULL;
ALTER TABLE store MODIFY COLUMN image_url VARCHAR(2048) NOT NULL;
ALTER TABLE store_inventory MODIFY COLUMN stock_quantity INT NOT NULL;
