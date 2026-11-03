create table imports (
    id         bigserial primary key,
    filename   varchar(255) not null,
    created_at timestamptz  not null default now()
);

create table charges (
    id          bigserial primary key,
    import_id   bigint        not null references imports (id) on delete cascade,
    charge_date date          not null,
    description varchar(500)  not null,
    amount      numeric(12, 2) not null
);

create index idx_charges_import_id on charges (import_id);
