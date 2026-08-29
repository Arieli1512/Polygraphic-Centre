--------------------
-- Troje klientów (rekordy demonstracyjne bazy, nie konta Firebase do logowania)
--------------------
--------------------
INSERT INTO
    clients (
        client_id,
        email,
        firebase_uid,
        first_name,
        last_name,
        status
    )
VALUES
    (
        1,
        'anna.kowalska@example.com',
        'firebase_client_anna_kowalska',
        'Anna',
        'Kowalska',
        'ACTIVE'
    ),
    (
        2,
        'jan.nowak@example.com',
        'firebase_client_jan_nowak',
        'Jan',
        'Nowak',
        'ACTIVE'
    ),
    (
        3,
        'marta.wisniewska@example.com',
        'firebase_client_marta_wisniewska',
        'Marta',
        'Wiśniewska',
        'BLOCKED'
    );

INSERT INTO
    wallets (client_id, balance, status)
VALUES
    (1, 8930, 'ACTIVE'),
    (2, 3180, 'ACTIVE'),
    (3, 5000, 'BLOCKED');

INSERT INTO
    wallet_top_ups (top_up_id, client_id, amount)
VALUES
    (1, 1, 10000),
    (2, 1, 10000),
    (3, 2, 30000),
    (4, 3, 5000);

--------------------
-- Dwa punkty druku oraz troje operatów (rekordy demonstracyjne bazy)
--------------------
--------------------
INSERT INTO
    printing_points (
        printing_point_id,
        name,
        street_address,
        city,
        postal_code,
        country,
        hourly_order_limit
    )
VALUES
    (
        1,
        'Drobny Druk Centrum',
        'Piotrkowska 123/5',
        'Łódź',
        '90-001',
        'Polska',
        12
    ),
    (
        2,
        'Drobny Druk Politechnika',
        'Wólczańska 215',
        'Łódź',
        '93-005',
        'Polska',
        8
    );

INSERT INTO
    opening_hours (
        printing_point_id,
        day_of_week,
        start_time,
        end_time
    )
VALUES
    (1, 1, '08:00', '18:00'),
    (1, 2, '08:00', '18:00'),
    (1, 3, '08:00', '18:00'),
    (1, 4, '08:00', '18:00'),
    (1, 5, '08:00', '18:00'),
    (1, 6, '10:00', '14:00'),
    (2, 1, '09:00', '17:00'),
    (2, 2, '09:00', '17:00'),
    (2, 3, '09:00', '17:00'),
    (2, 4, '09:00', '17:00'),
    (2, 5, '09:00', '15:00');

INSERT INTO
    operators (
        operator_id,
        printing_point_id,
        firebase_uid,
        email,
        employee_number,
        role,
        status
    )
VALUES
    (
        1,
        1,
        'firebase_operator_adam_zielinski',
        'adam.zielinski@drobnyd.pl',
        'EMP-CEN-001',
        'ADMIN',
        'ACTIVE'
    ),
    (
        2,
        1,
        'firebase_operator_ewa_lis',
        'ewa.lis@drobnyd.pl',
        'EMP-CEN-002',
        'EMPLOYEE',
        'ACTIVE'
    ),
    (
        3,
        2,
        'firebase_operator_tomasz_wrobel',
        'tomasz.wrobel@drobnyd.pl',
        'EMP-POL-001',
        'ADMIN',
        'ACTIVE'
    );

INSERT INTO
    rate_sheets (printing_point_id, paper_type, format, page_price)
VALUES
    -- Punkt 1
    (1, 'standardowy', 'A5', 30),
    (1, 'standardowy', 'A4', 50),
    (1, 'standardowy', 'A3', 100),
    (1, 'błyszczący', 'A5', 70),
    (1, 'błyszczący', 'A4', 120),
    (1, 'błyszczący', 'A3', 220),
    (1, 'matowy', 'A5', 60),
    (1, 'matowy', 'A4', 100),
    (1, 'matowy', 'A3', 190),
    (1, 'kredowy', 'A5', 80),
    (1, 'kredowy', 'A4', 140),
    (1, 'kredowy', 'A3', 260),
    -- Punkt 2
    (2, 'standardowy', 'A5', 25),
    (2, 'standardowy', 'A4', 45),
    (2, 'standardowy', 'A3', 90),
    (2, 'błyszczący', 'A5', 65),
    (2, 'błyszczący', 'A4', 110),
    (2, 'błyszczący', 'A3', 210),
    (2, 'matowy', 'A5', 55),
    (2, 'matowy', 'A4', 95),
    (2, 'matowy', 'A3', 180),
    (2, 'kredowy', 'A5', 75),
    (2, 'kredowy', 'A4', 130),
    (2, 'kredowy', 'A3', 250);

INSERT INTO
    extra_pricing (
        printing_point_id,
        binding_price,
        stapling_price,
        cover_price
    )
VALUES
    (1, 800, 150, 500),
    (2, 1000, 100, 600);

--------------------
-- 10 zamówień
--------------------
--------------------
INSERT INTO
    orders (
        order_id,
        printing_point_id,
        client_id,
        file_path,
        page_count,
        total_price,
        pickup_at,
        status
    )
VALUES
    (
        1,
        1,
        1,
        'orders/1/praca-dyplomowa.pdf',
        52,
        2600,
        '2026-06-05 10:00:00+02',
        'PENDING'
    ),
    (
        2,
        1,
        2,
        'orders/2/prezentacja.pdf',
        24,
        2880,
        '2026-06-05 11:00:00+02',
        'READY'
    ),
    (
        3,
        2,
        1,
        'orders/3/notatki.pdf',
        180,
        8100,
        '2026-06-05 13:00:00+02',
        'APPROVED'
    ),
    (
        4,
        1,
        3,
        'orders/4/ulotka.pdf',
        2,
        1320,
        '2026-06-06 12:00:00+02',
        'CANCELLED'
    ),
    (
        5,
        2,
        2,
        'orders/5/instrukcja.pdf',
        36,
        3420,
        '2026-06-05 14:00:00+02',
        'DISPENSED'
    ),
    (
        6,
        1,
        1,
        'orders/6/cv.pdf',
        3,
        150,
        '2026-06-04 16:00:00+02',
        'READY'
    ),
    (
        7,
        2,
        2,
        'orders/7/skrypt.pdf',
        120,
        11400,
        '2026-06-06 11:00:00+02',
        'APPROVED'
    ),
    (
        8,
        1,
        1,
        'orders/8/plakat.pdf',
        1,
        220,
        '2026-06-06 13:00:00+02',
        'PENDING'
    ),
    (
        9,
        2,
        3,
        'orders/9/regulamin.pdf',
        14,
        630,
        '2026-06-07 10:00:00+02',
        'APPROVED'
    ),
    (
        10,
        1,
        2,
        'orders/10/oferta.pdf',
        48,
        9120,
        '2026-06-07 15:00:00+02',
        'READY'
    );

INSERT INTO
    print_settings (
        order_id,
        format,
        paper_type,
        color_mode,
        duplex,
        orientation,
        finishing,
        copies
    )
VALUES
    (
        1,
        'A4',
        'standardowy',
        'GRAYSCALE',
        'DOUBLE_SIDED',
        'PORTRAIT',
        'NONE',
        1
    ),
    (
        2,
        'A4',
        'błyszczący',
        'COLOR',
        'SINGLE_SIDED',
        'LANDSCAPE',
        'STAPLING',
        1
    ),
    (
        3,
        'A4',
        'standardowy',
        'GRAYSCALE',
        'DOUBLE_SIDED',
        'PORTRAIT',
        'BINDING',
        2
    ),
    (
        4,
        'A5',
        'błyszczący',
        'COLOR',
        'SINGLE_SIDED',
        'LANDSCAPE',
        'NONE',
        10
    ),
    (
        5,
        'A4',
        'matowy',
        'GRAYSCALE',
        'DOUBLE_SIDED',
        'PORTRAIT',
        'COVER',
        3
    ),
    (
        6,
        'A4',
        'standardowy',
        'GRAYSCALE',
        'SINGLE_SIDED',
        'PORTRAIT',
        'NONE',
        1
    ),
    (
        7,
        'A4',
        'matowy',
        'COLOR',
        'DOUBLE_SIDED',
        'PORTRAIT',
        'BINDING',
        2
    ),
    (
        8,
        'A3',
        'błyszczący',
        'COLOR',
        'SINGLE_SIDED',
        'LANDSCAPE',
        'NONE',
        1
    ),
    (
        9,
        'A4',
        'standardowy',
        'GRAYSCALE',
        'DOUBLE_SIDED',
        'PORTRAIT',
        'NONE',
        1
    ),
    (
        10,
        'A3',
        'kredowy',
        'COLOR',
        'SINGLE_SIDED',
        'LANDSCAPE',
        'STAPLING',
        2
    );

INSERT INTO
    printouts (order_id, operator_id, printed_at)
VALUES
    (2, 2, '2026-06-04 15:20:00+02'),
    (5, 3, '2026-06-04 09:15:00+02'),
    (6, 1, '2026-06-04 15:10:00+02'),
    (10, 2, '2026-06-05 17:40:00+02');