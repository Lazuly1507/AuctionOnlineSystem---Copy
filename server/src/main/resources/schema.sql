SET
FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS bids;
DROP TABLE IF EXISTS auction_sessions;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS auto_bids;

SET
FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50) UNIQUE NOT NULL,
    password   VARCHAR(255)       NOT NULL,
    full_name  VARCHAR(100)       NOT NULL,
    email      VARCHAR(100) UNIQUE,
    available_balance DECIMAL(18, 2) DEFAULT 0,
    frozen_funds TEXT,
    role       ENUM('ADMIN','SELLER','BIDDER') DEFAULT 'BIDDER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE items
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    seller_id      INT          NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    TEXT,
    image_url      VARCHAR(255) DEFAULT NULL,
    category       VARCHAR(50),
    starting_price BIGINT       NOT NULL CHECK (starting_price > 0),
    step_price     BIGINT       NOT NULL CHECK (step_price > 0),
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted        BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE auction_sessions
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    item_id        INT      NOT NULL,
    UNIQUE (item_id),
    seller_id      INT      NOT NULL,
    winner_id      INT NULL,
    status         ENUM('OPEN','RUNNING','FINISHED','PAID','CANCELED') DEFAULT 'OPEN',
    start_time     DATETIME NULL DEFAULT NULL,
    end_time       DATETIME NOT NULL,
    deposit_amount BIGINT    DEFAULT 0 CHECK (deposit_amount >= 0),
    highest_bid    BIGINT    DEFAULT 0,
    version        INT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    extended_count INT       DEFAULT 0,
    FOREIGN KEY (item_id) REFERENCES items (id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE bids
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    session_id  INT    NOT NULL,
    user_id     INT    NOT NULL,
    bid_amount  BIGINT NOT NULL,
    bid_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_auto_bid BOOLEAN   DEFAULT FALSE,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES auction_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE auto_bids
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    session_id       INT    NOT NULL,
    user_id          INT    NOT NULL,
    UNIQUE (session_id, user_id),
    max_amount       BIGINT  NOT NULL,
    increment_amount BIGINT NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES auction_sessions (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_bids_session ON bids (session_id);
CREATE INDEX idx_bids_user ON bids (user_id);
CREATE INDEX idx_auction_status ON auction_sessions (status);
CREATE INDEX idx_session_end_time ON auction_sessions (end_time);
CREATE INDEX idx_auto_bid_session ON auto_bids (session_id);
