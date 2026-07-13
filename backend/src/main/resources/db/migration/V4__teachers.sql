-- Konta nauczycielek: dezaktywacja kont + przypisania do grup (wiele-do-wielu)

ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE class_group_teachers (
    class_group_id UUID NOT NULL REFERENCES class_groups (id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users (id),
    PRIMARY KEY (class_group_id, user_id)
);

-- backfill: dotychczasowa właścicielka staje się przypisaną nauczycielką
INSERT INTO class_group_teachers (class_group_id, user_id)
SELECT id, owner_user_id FROM class_groups;

ALTER TABLE class_groups DROP COLUMN owner_user_id;
