INSERT INTO wallet_balance (
    user_id,
    asset,
    balance,
    reserved
)
VALUES (
    1,
    'USDT',
    1000,
    0
)
ON CONFLICT (user_id, asset)
DO NOTHING;

INSERT INTO wallet_balance (
    user_id,
    asset,
    balance,
    reserved
)
VALUES (
    1,
    'BTC',
    2,
    0
)
ON CONFLICT (user_id, asset)
DO NOTHING;
