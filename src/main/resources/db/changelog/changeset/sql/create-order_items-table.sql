create table order_items(
    id bigserial primary key,
    order_id bigint not null,
    item_id bigint not null,
    quantity integer not null,
    foreign key (order_id) references orders(id),
    foreign key (item_id) references items(id)
)