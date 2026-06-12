create table items(
    id bigserial primary key,
    name varchar(255) not null,
    price numeric(15, 2) not null,
    created_at timestamp not null,
    updated_at timestamp
)