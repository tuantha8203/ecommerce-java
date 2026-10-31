ALTER TABLE customer.customers ADD COLUMN keycloak_id varchar(64);
ALTER TABLE customer.customers ADD CONSTRAINT uq_customers_keycloak_id UNIQUE (keycloak_id);