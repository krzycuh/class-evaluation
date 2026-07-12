-- Schemat bazy: aplikacja oceny przedszkolaków

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name  TEXT NOT NULL,
    role          TEXT NOT NULL CHECK (role IN ('TEACHER', 'ADMIN')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE age_groups (
    id             UUID PRIMARY KEY,
    name           TEXT NOT NULL UNIQUE,
    min_age_years  INT  NOT NULL,
    sort_order     INT  NOT NULL DEFAULT 0
);

CREATE TABLE class_groups (
    id            UUID PRIMARY KEY,
    name          TEXT NOT NULL,
    school_year   TEXT NOT NULL,
    owner_user_id UUID NOT NULL REFERENCES users (id)
);

CREATE TABLE students (
    id             UUID PRIMARY KEY,
    class_group_id UUID NOT NULL REFERENCES class_groups (id),
    first_name     TEXT NOT NULL,
    last_name      TEXT NOT NULL,
    birth_date     DATE NOT NULL,
    age_group_id   UUID NOT NULL REFERENCES age_groups (id),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_students_class_group ON students (class_group_id);

CREATE TABLE development_areas (
    id          UUID PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT,
    sort_order  INT NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE skills (
    id                    UUID PRIMARY KEY,
    area_id               UUID NOT NULL REFERENCES development_areas (id),
    title                 TEXT NOT NULL,
    description           TEXT,
    parent_recommendation TEXT,
    sort_order            INT NOT NULL DEFAULT 0,
    active                BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_skills_area ON skills (area_id);

CREATE TABLE skill_age_groups (
    skill_id     UUID NOT NULL REFERENCES skills (id) ON DELETE CASCADE,
    age_group_id UUID NOT NULL REFERENCES age_groups (id),
    PRIMARY KEY (skill_id, age_group_id)
);

CREATE TABLE assessment_periods (
    id          UUID PRIMARY KEY,
    school_year TEXT NOT NULL,
    name        TEXT NOT NULL,
    starts_on   DATE NOT NULL,
    ends_on     DATE NOT NULL,
    status      TEXT NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    UNIQUE (school_year, name)
);

CREATE TABLE assessments (
    id         UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    skill_id   UUID NOT NULL REFERENCES skills (id),
    period_id  UUID NOT NULL REFERENCES assessment_periods (id),
    -- NULL = brak oceny (możliwa notatka bez rozstrzygnięcia)
    value      TEXT CHECK (value IN ('MASTERED', 'NOT_YET', 'IN_PROGRESS')),
    note       TEXT,
    updated_by UUID NOT NULL REFERENCES users (id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, skill_id, period_id)
);
CREATE INDEX idx_assessments_student_period ON assessments (student_id, period_id);

CREATE TABLE student_period_notes (
    id         UUID PRIMARY KEY,
    student_id UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    period_id  UUID NOT NULL REFERENCES assessment_periods (id),
    content    TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (student_id, period_id)
);

CREATE TABLE reports (
    id           UUID PRIMARY KEY,
    student_id   UUID NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    period_id    UUID NOT NULL REFERENCES assessment_periods (id),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    generated_by UUID NOT NULL REFERENCES users (id),
    content      JSONB NOT NULL,
    UNIQUE (student_id, period_id)
);
