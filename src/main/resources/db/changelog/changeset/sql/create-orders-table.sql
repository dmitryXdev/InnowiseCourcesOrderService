CREATE TABLE orders(
    id bigserial primary key,
    user_id bigint not null,
    status varchar(255) not null,
    total_price numeric(15, 2) not null,
    deleted boolean not null,
    created_at timestamp           not null,
    updated_at timestamp
)