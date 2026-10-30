ALTER TABLE inventory.stock_reservations DROP CONSTRAINT ck_stock_reservations_status;
ALTER TABLE inventory.stock_reservations ADD CONSTRAINT ck_stock_reservations_status
    CHECK (status IN ('ACTIVE', 'CONFIRMED', 'COMMITTED', 'RELEASED'));