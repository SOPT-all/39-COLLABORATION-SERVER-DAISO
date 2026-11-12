CREATE TABLE product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_number VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price INT NOT NULL,
    image_url VARCHAR(2048) NULL,
    review_count INT NOT NULL,
    is_liked BOOLEAN NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    image_url VARCHAR(2048) NULL,
    business_status VARCHAR(30) NOT NULL,
    is_favorite BOOLEAN NOT NULL,
    distance BIGINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE store_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    stock_quantity INT NULL,
    pickup_available BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_inventory_product_store UNIQUE (product_id, store_id),
    INDEX idx_store_inventory_store_id (store_id),
    CONSTRAINT fk_store_inventory_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_store_inventory_store FOREIGN KEY (store_id) REFERENCES store (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
