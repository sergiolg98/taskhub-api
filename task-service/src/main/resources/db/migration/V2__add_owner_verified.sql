-- false = the task was accepted while auth-service could not confirm the owner (class 11). Existing tasks were verified.
ALTER TABLE tasks ADD COLUMN owner_verified BOOLEAN NOT NULL DEFAULT TRUE;
