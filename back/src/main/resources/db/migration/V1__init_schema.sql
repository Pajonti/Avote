-- Schéma initial : parlementaires des trois chambres (AN, Sénat, PE).

create table political_group (
    id      bigint generated always as identity primary key,
    chamber varchar(10)  not null check (chamber in ('AN', 'SENAT', 'EP')),
    code    varchar(50)  not null,
    label   varchar(300) not null,
    color   varchar(9),
    unique (chamber, code)
);

create table parliamentarian (
    id           bigint generated always as identity primary key,
    chamber      varchar(10)  not null check (chamber in ('AN', 'SENAT', 'EP')),
    source_id    varchar(30)  not null,
    civility     varchar(10),
    first_name   varchar(150) not null,
    last_name    varchar(150) not null,
    gender       char(1) check (gender in ('M', 'F')),
    birth_date   date,
    profession   varchar(300),
    photo_url    varchar(500),
    email        varchar(255),
    external_url varchar(500),
    hatvp_url    varchar(500),
    -- prénom + nom, minuscules, sans accents : alimente la recherche
    search_text  varchar(400) not null,
    active       boolean      not null default true,
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now(),
    unique (chamber, source_id)
);

create index idx_parliamentarian_chamber_active on parliamentarian (chamber, active);

create table mandate (
    id                 bigint generated always as identity primary key,
    parliamentarian_id bigint  not null references parliamentarian (id) on delete cascade,
    group_id           bigint references political_group (id),
    constituency       varchar(200),
    start_date         date,
    end_date           date,
    active             boolean not null default true
);

-- Un seul mandat actif par parlementaire ; l'historique reste en base.
create unique index uq_mandate_active on mandate (parliamentarian_id) where active;
create index idx_mandate_group on mandate (group_id);

create table ingestion_run (
    id            bigint generated always as identity primary key,
    chamber       varchar(10) not null check (chamber in ('AN', 'SENAT', 'EP')),
    started_at    timestamptz not null default now(),
    finished_at   timestamptz,
    status        varchar(10) not null check (status in ('RUNNING', 'SUCCESS', 'FAILED')),
    fetched_count integer,
    error         text
);

create index idx_ingestion_run_chamber_started on ingestion_run (chamber, started_at desc);
