-- Seed cylinder_types table
INSERT INTO cylinder_types (id, name, capacity_kg)
VALUES
    ('ct111111-1111-1111-1111-111111111111', '12.5kg Domestic', 12.5),
    ('ct222222-2222-2222-2222-222222222222', '5kg Domestic', 5.0),
    ('ct333333-3333-3333-3333-333333333333', '2.3kg Camping', 2.3),
    ('ct444444-4444-4444-4444-444444444444', '37.5kg Commercial', 37.5)
ON CONFLICT (id) DO UPDATE
SET
    name = EXCLUDED.name,
    capacity_kg = EXCLUDED.capacity_kg;

-- Seed inventory table (per dealer, per cylinder type)
INSERT INTO inventory (
    id,
    dealer_id,
    cylinder_type_id,
    available_stock,
    last_updated
)
VALUES
    (
        '934a62d3-0a28-4465-834c-bd355d29cd34',
        '22222222-2222-2222-2222-222222222222',
        'ct111111-1111-1111-1111-111111111111',
        30,
        '2026-05-12 09:32:25'
    ),
    (
        'in222222-2222-2222-2222-222222222222',
        '22222222-2222-2222-2222-222222222222',
        'ct222222-2222-2222-2222-222222222222',
        50,
        '2026-05-12 09:32:25'
    ),
    (
        '55555555-5555-5555-5555-555555555555',
        '33333333-3333-3333-3333-333333333333',
        'ct111111-1111-1111-1111-111111111111',
        62,
        '2026-05-12 09:20:00'
    ),
    (
        'in444444-4444-4444-4444-444444444444',
        '33333333-3333-3333-3333-333333333333',
        'ct333333-3333-3333-3333-333333333333',
        100,
        '2026-05-12 09:20:00'
    )
ON CONFLICT (id) DO UPDATE
SET
    dealer_id = EXCLUDED.dealer_id,
    cylinder_type_id = EXCLUDED.cylinder_type_id,
    available_stock = EXCLUDED.available_stock,
    last_updated = EXCLUDED.last_updated;
