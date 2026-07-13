-- Wymuszenie zmiany hasła po założeniu konta / resecie przez admina

ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
