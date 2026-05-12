INSERT INTO users (
    id,
    nic,
    email,
    password,
    name,
    role,
    phone,
    address,
    business_name,
    business_reg_no,
    created_at
)
VALUES
    (
        '11111111-1111-1111-1111-111111111111',
        '199001230001',
        'admin@gastracker.lk',
        '$2a$10$kDKaEp1osB9Og9fQJ1THWOfHv.LwIjmw3RhZvzW5qSoU1IVdMuDue',
        'System Admin',
        'ADMIN',
        '0770000000',
        '1 Government Secretariat, Colombo 01',
        'GasTracker Admin Office',
        'ADM-0001',
        '2026-05-12 09:00:00'
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        '987654321V',
        'dealer1@gastracker.lk',
        '$2a$10$kDKaEp1osB9Og9fQJ1THWOfHv.LwIjmw3RhZvzW5qSoU1IVdMuDue',
        'Silva Gas Station',
        'DEALER',
        '0771234567',
        '45/B Galle Road, Colombo 03',
        'Silva Gas Distribution',
        'REG-2026-001',
        '2026-05-12 09:05:00'
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        '876543210V',
        'dealer2@gastracker.lk',
        '$2a$10$kDKaEp1osB9Og9fQJ1THWOfHv.LwIjmw3RhZvzW5qSoU1IVdMuDue',
        'Kandy Gas Center',
        'DEALER',
        '0772345678',
        '123 Peradeniya Road, Kandy',
        'Kandy Gas Center',
        'REG-2026-002',
        '2026-05-12 09:10:00'
    ),
    (
        '44444444-4444-4444-4444-444444444444',
        '123456789V',
        'citizen@gastracker.lk',
        '$2a$10$kDKaEp1osB9Og9fQJ1THWOfHv.LwIjmw3RhZvzW5qSoU1IVdMuDue',
        'John Citizen',
        'CITIZEN',
        '0773456789',
        '12 Lake Drive, Colombo 07',
        NULL,
        NULL,
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
    address = EXCLUDED.address,
    business_name = EXCLUDED.business_name,
    business_reg_no = EXCLUDED.business_reg_no,
    created_at = EXCLUDED.created_at;
