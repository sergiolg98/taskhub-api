CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    role       VARCHAR(20)   NOT NULL,
    created_at TIMESTAMP     NOT NULL
);
