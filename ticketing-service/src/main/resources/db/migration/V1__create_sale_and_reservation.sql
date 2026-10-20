create table sale (
    id                uuid         primary key,
    name              varchar(120) not null,
    total_tickets     integer      not null,
    available_tickets integer      not null,
    created_at        timestamptz  not null,

    constraint ck_sale_total_tickets_positive check (total_tickets > 0),
    -- Última linha de defesa: mesmo que uma estratégia de concorrência falhe,
    -- o banco recusa estoque negativo ou maior que o total.
    constraint ck_sale_available_within_total check (available_tickets between 0 and total_tickets)
);

create table reservation (
    id          uuid        primary key,
    sale_id     uuid        not null references sale (id),
    customer_id varchar(60) not null,
    quantity    integer     not null,
    status      varchar(20) not null,
    created_at  timestamptz not null,
    expires_at  timestamptz not null,

    constraint ck_reservation_quantity check (quantity between 1 and 4),
    constraint ck_reservation_status check (status in ('HELD', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    constraint ck_reservation_deadline check (expires_at > created_at)
);

-- O PostgreSQL não cria índice para chave estrangeira automaticamente.
create index ix_reservation_sale_id on reservation (sale_id);
