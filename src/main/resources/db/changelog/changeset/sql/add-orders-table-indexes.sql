create index idx_orders_user_id on orders(user_id);
create index idx_orders_status on orders(status);
create index idx_orders_total_price on orders(total_price);
create index idx_orders_total_deleted on orders(deleted);
create index idx_orders_created_at on orders(created_at);
create index idx_orders_updated_at on orders(updated_at);