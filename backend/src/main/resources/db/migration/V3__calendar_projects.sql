-- Kalendarz (wydarzenia o różnych zasięgach + organizer) i projekty z postępem
-- patrz docs/kalendarz-projekty.md

CREATE TABLE event_categories (
    id         UUID PRIMARY KEY,
    name       TEXT NOT NULL UNIQUE,
    color      TEXT NOT NULL,
    sort_order INT  NOT NULL DEFAULT 0,
    active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE calendar_events (
    id               UUID PRIMARY KEY,
    title            TEXT NOT NULL,
    description      TEXT,
    category_id      UUID NOT NULL REFERENCES event_categories (id),
    scope            TEXT NOT NULL CHECK (scope IN ('NATIONAL', 'PRESCHOOL', 'CLASS_GROUP', 'STUDENT')),
    class_group_id   UUID REFERENCES class_groups (id) ON DELETE CASCADE,
    student_id       UUID REFERENCES students (id) ON DELETE CASCADE,
    starts_on        DATE NOT NULL,
    ends_on          DATE NOT NULL,
    -- proste powtarzanie roczne dla świąt stałodatowych (bez silnika RRULE)
    yearly_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    -- NULL dla wpisów seedowanych migracją (przed utworzeniem kont)
    created_by       UUID REFERENCES users (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_on >= starts_on),
    CHECK (
        (scope IN ('NATIONAL', 'PRESCHOOL') AND class_group_id IS NULL AND student_id IS NULL)
        OR (scope = 'CLASS_GROUP' AND class_group_id IS NOT NULL AND student_id IS NULL)
        OR (scope = 'STUDENT' AND class_group_id IS NOT NULL AND student_id IS NOT NULL)
    )
);
CREATE INDEX idx_calendar_events_dates ON calendar_events (starts_on, ends_on);
CREATE INDEX idx_calendar_events_group ON calendar_events (class_group_id);

CREATE TABLE event_tasks (
    id         UUID PRIMARY KEY,
    event_id   UUID NOT NULL REFERENCES calendar_events (id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    done       BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_event_tasks_event ON event_tasks (event_id);

CREATE TABLE projects (
    id             UUID PRIMARY KEY,
    title          TEXT NOT NULL,
    description    TEXT,
    kind           TEXT NOT NULL CHECK (kind IN ('TRIP', 'CONTEST', 'OTHER')),
    scope          TEXT NOT NULL CHECK (scope IN ('PRESCHOOL', 'CLASS_GROUP')),
    class_group_id UUID REFERENCES class_groups (id) ON DELETE CASCADE,
    starts_on      DATE NOT NULL,
    ends_on        DATE NOT NULL,
    status         TEXT NOT NULL DEFAULT 'PLANNED'
                   CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    created_by     UUID REFERENCES users (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_on >= starts_on),
    CHECK (
        (scope = 'PRESCHOOL' AND class_group_id IS NULL)
        OR (scope = 'CLASS_GROUP' AND class_group_id IS NOT NULL)
    )
);
CREATE INDEX idx_projects_group ON projects (class_group_id);

CREATE TABLE project_tasks (
    id         UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    due_on     DATE,
    done       BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_project_tasks_project ON project_tasks (project_id);

-- Kategorie startowe
INSERT INTO event_categories (id, name, color, sort_order) VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Święto',                   '#b3503f', 1),
    ('c0000000-0000-0000-0000-000000000002', 'Dzień tematyczny',         '#2e7d6b', 2),
    ('c0000000-0000-0000-0000-000000000003', 'Tydzień tematyczny',       '#6b5fa8', 3),
    ('c0000000-0000-0000-0000-000000000004', 'Uroczystość przedszkolna', '#de9a3c', 4),
    ('c0000000-0000-0000-0000-000000000005', 'Inne',                     '#64766f', 5);

-- Święta ogólnopolskie obchodzone w przedszkolach — powtarzane co rok
-- (daty bazowe z roku 2026; wystąpienia w kolejnych latach wylicza aplikacja)
INSERT INTO calendar_events (id, title, category_id, scope, starts_on, ends_on, yearly_recurring) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'Dzień Babci',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-01-21', '2026-01-21', TRUE),
    ('e0000000-0000-0000-0000-000000000002', 'Dzień Dziadka',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-01-22', '2026-01-22', TRUE),
    ('e0000000-0000-0000-0000-000000000003', 'Dzień Kobiet',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-03-08', '2026-03-08', TRUE),
    ('e0000000-0000-0000-0000-000000000004', 'Pierwszy dzień wiosny',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-03-21', '2026-03-21', TRUE),
    ('e0000000-0000-0000-0000-000000000005', 'Dzień Ziemi',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-04-22', '2026-04-22', TRUE),
    ('e0000000-0000-0000-0000-000000000006', 'Dzień Mamy',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-05-26', '2026-05-26', TRUE),
    ('e0000000-0000-0000-0000-000000000007', 'Dzień Dziecka',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-06-01', '2026-06-01', TRUE),
    ('e0000000-0000-0000-0000-000000000008', 'Dzień Taty',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-06-23', '2026-06-23', TRUE),
    ('e0000000-0000-0000-0000-000000000009', 'Ogólnopolski Dzień Przedszkolaka',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-09-20', '2026-09-20', TRUE),
    ('e0000000-0000-0000-0000-000000000010', 'Dzień Edukacji Narodowej',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-10-14', '2026-10-14', TRUE),
    ('e0000000-0000-0000-0000-000000000011', 'Andrzejki',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-11-30', '2026-11-30', TRUE),
    ('e0000000-0000-0000-0000-000000000012', 'Mikołajki',
     'c0000000-0000-0000-0000-000000000001', 'NATIONAL', '2026-12-06', '2026-12-06', TRUE);
