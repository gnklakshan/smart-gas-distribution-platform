INSERT INTO inventory (
    id,
    dealer_id,
    dealer_name,
    address,
    latitude,
    longitude,
    available_stock,
    last_updated
)
VALUES
    (
        '934a62d3-0a28-4465-834c-bd355d29cd34',
        '22222222-2222-2222-2222-222222222222',
        'Silva Gas Station',
        '45/B Galle Road, Colombo 03',
        6.9271,
        79.8612,
        30,
        '2026-05-12 09:32:25'
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        '33333333-3333-3333-3333-333333333333',
        'Kandy Gas Center',
        '123 Peradeniya Road, Kandy',
        7.2906,
        80.6337,
        62,
        '2026-05-12 09:20:00'
    )
ON CONFLICT (dealer_id) DO UPDATE
SET
    id = EXCLUDED.id,
    dealer_name = EXCLUDED.dealer_name,
    address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    available_stock = EXCLUDED.available_stock,
    last_updated = EXCLUDED.last_updated;
