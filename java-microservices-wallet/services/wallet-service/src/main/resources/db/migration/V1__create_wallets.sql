CREATE TABLE wallets (
    user_id UUID PRIMARY KEY,
    balance BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',

    CONSTRAINT wallets_balance_range
        CHECK (balance BETWEEN 0 AND 1000000000000),

    CONSTRAINT wallets_currency_vnd
        CHECK (currency = 'VND')
);