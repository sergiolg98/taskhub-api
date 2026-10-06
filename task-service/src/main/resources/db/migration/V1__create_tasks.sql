-- owner_id is the user id carried by the JWT (claim "uid"). There is NO foreign key: the users table
-- lives in another service's database (auth_db), and a constraint cannot cross databases.
CREATE TABLE tasks (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(150)  NOT NULL,
    description VARCHAR(1000),
    status      VARCHAR(20)   NOT NULL,
    owner_id    BIGINT        NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL
);

CREATE INDEX idx_tasks_owner_id ON tasks (owner_id);
