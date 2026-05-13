INSERT INTO citizen_queues (
    id,
    user_id,
    dealer_id,
    cylinder_type_id,
    token_number,
    status,
    requested_at,
    fulfilled_at
)
VALUES
    (
        'qq111111-1111-1111-1111-111111111111',
        '44444444-4444-4444-4444-444444444444',
        '22222222-2222-2222-2222-222222222222',
        'ct111111-1111-1111-1111-111111111111',
        'TKN-ABCD1234',
        'WAITING',
        '2026-05-12 10:00:00',
        NULL
    )
ON CONFLICT (id) DO UPDATE
SET
    user_id = EXCLUDED.user_id,
    dealer_id = EXCLUDED.dealer_id,
    cylinder_type_id = EXCLUDED.cylinder_type_id,
    token_number = EXCLUDED.token_number,
    status = EXCLUDED.status,
    requested_at = EXCLUDED.requested_at,
    fulfilled_at = EXCLUDED.fulfilled_at;
