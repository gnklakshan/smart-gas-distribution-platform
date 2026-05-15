-- Seed users table (without dealer-specific fields)
INSERT INTO users (
    id,
    nic,
    email,
    password,
    name,
    role,
    phone,
    created_at
)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        '199001230001',
        'admin@gastracker.lk',
        '$2b$10$/kMTXjlrxH.C02XAx4.xjO2rzYv9iy84a7Bo7j7nOlhDx9B8A/S8y',
        'System Admin',
        'ADMIN',
        '0770000000',
        '2026-05-12 09:00:00'
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        '987654321V',
        'dealer1@gastracker.lk',
        '$2b$10$/kMTXjlrxH.C02XAx4.xjO2rzYv9iy84a7Bo7j7nOlhDx9B8A/S8y',
        'Silva Gas Station',
        'DEALER',
        '0771234567',
        '2026-05-12 09:05:00'
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        '876543210V',
        'dealer2@gastracker.lk',
        '$2b$10$/kMTXjlrxH.C02XAx4.xjO2rzYv9iy84a7Bo7j7nOlhDx9B8A/S8y',
        'Kandy Gas Center',
        'DEALER',
        '0772345678',
        '2026-05-12 09:10:00'
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        '123456789V',
        'citizen@gastracker.lk',
        '$2b$10$/kMTXjlrxH.C02XAx4.xjO2rzYv9iy84a7Bo7j7nOlhDx9B8A/S8y',
        'John Citizen',
        'CITIZEN',
        '0773456789',
        '2026-05-12 09:15:00'
    )
ON CONFLICT (nic) DO UPDATE
SET
    id = EXCLUDED.id,
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    name = EXCLUDED.name,
    role = EXCLUDED.role,
    phone = EXCLUDED.phone,
    created_at = EXCLUDED.created_at;

-- Seed dealers table (1-to-1 with users)
INSERT INTO dealers (
    id,
    user_id,
    business_name,
    business_reg_no,
    address,
    latitude,
    longitude
)
VALUES
    (
        'dd111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222',
        'Silva Gas Distribution',
        'REG-2026-001',
        '45/B Galle Road, Colombo 03',
        6.9271,
        79.8612
    ),
    (
        'dd222222-2222-2222-2222-222222222222',
        '33333333-3333-3333-3333-333333333333',
        'Kandy Gas Center',
        'REG-2026-002',
        '123 Peradeniya Road, Kandy',
        7.2906,
        80.6337
    )
ON CONFLICT (user_id) DO UPDATE
SET
    id = EXCLUDED.id,
    business_name = EXCLUDED.business_name,
    business_reg_no = EXCLUDED.business_reg_no,
    address = EXCLUDED.address,
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude;
