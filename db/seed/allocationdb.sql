INSERT INTO allocations (
    id,
    dealer_id,
    dealer_name,
    requested_quantity,
    approved_quantity,
    status,
    rejection_reason,
    requested_at,
    resolved_at,
    delivered_at
)
VALUES
    (
        '66666666-6666-6666-6666-666666666666',
        '22222222-2222-2222-2222-222222222222',
        'Silva Gas Station',
        100,
        80,
        'APPROVED',
        NULL,
        '2026-05-12 09:00:00',
        '2026-05-12 09:10:00',
        NULL
    ),
    (
        '77777777-7777-7777-7777-777777777777',
        '33333333-3333-3333-3333-333333333333',
        'Kandy Gas Center',
        75,
        NULL,
        'PENDING',
        NULL,
        '2026-05-12 09:25:00',
        NULL,
        NULL
    )
ON CONFLICT (id) DO UPDATE
SET
    dealer_id = EXCLUDED.dealer_id,
    dealer_name = EXCLUDED.dealer_name,
    requested_quantity = EXCLUDED.requested_quantity,
    approved_quantity = EXCLUDED.approved_quantity,
    status = EXCLUDED.status,
    rejection_reason = EXCLUDED.rejection_reason,
    requested_at = EXCLUDED.requested_at,
    resolved_at = EXCLUDED.resolved_at,
    delivered_at = EXCLUDED.delivered_at;
