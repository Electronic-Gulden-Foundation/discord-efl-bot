# --- !Ups

ALTER TABLE transactions
    ADD COLUMN withdrawal_address VARCHAR(64) DEFAULT NULL AFTER vout;

# --- !Downs

ALTER TABLE transactions
    DROP COLUMN withdrawal_address;
