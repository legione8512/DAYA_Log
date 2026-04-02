INSERT INTO studio (id, name, legal_name, email, phone, address_line1, city, active)
VALUES ('11111111-1111-1111-1111-111111111111', 'DAYA Pilates', 'DAYA Pilates Studio SRL',
        'contact@dayalog.ro', '+40 721 000 000', 'Strada Exemplu 10', 'Constanța', TRUE);

-- bcrypt hash example for password: Admin123!Change
INSERT INTO app_user (id, studio_id, email, password_hash, role, email_verified, force_password_change, active)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111',
        'admin@dayalog.ro', '$2a$12$S3k16uC7gQ4sb3Xk9A0LFu9g5z0Q2cXjI2S6iQY2mKXnN6r0sRz5K',
        'ADMIN', TRUE, FALSE, TRUE);

INSERT INTO service (id, studio_id, name, duration_minutes, price, description, active)
VALUES
('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Pilates Reformer', 60, 180.00, 'Ședință individuală pe reformer', TRUE),
('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111111', 'Pilates Mat', 60, 120.00, 'Clasă de mat work', TRUE),
('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Recovery Session', 60, 150.00, 'Sesiune de recuperare și mobilitate', TRUE);

INSERT INTO instructor (id, studio_id, first_name, last_name, email, phone, specialisation, active)
VALUES
('44444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111111', 'Ana', 'Ionescu', 'ana@dayalog.ro', '+40 722 111 111', 'Reformer & posture', TRUE),
('44444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111111', 'Maria', 'Popa', 'maria@dayalog.ro', '+40 722 222 222', 'Mat & recovery', TRUE);

INSERT INTO resource (id, studio_id, name, type, description, active)
VALUES
('55555555-5555-5555-5555-555555555551', '11111111-1111-1111-1111-111111111111', 'Reformer 1', 'REFORMER', 'Pat reformer premium', TRUE),
('55555555-5555-5555-5555-555555555552', '11111111-1111-1111-1111-111111111111', 'Studio A', 'ROOM', 'Sala principală', TRUE);

INSERT INTO client (id, studio_id, first_name, last_name, email, phone, date_of_birth, gdpr_consent, gdpr_consent_at, email_allowed, sms_allowed, marketing_allowed, active)
VALUES
('66666666-6666-6666-6666-666666666661', '11111111-1111-1111-1111-111111111111', 'Elena', 'Marin', 'elena.marin@example.com', '+40 723 111 111', '1994-05-10', TRUE, NOW(), TRUE, FALSE, FALSE, TRUE),
('66666666-6666-6666-6666-666666666662', '11111111-1111-1111-1111-111111111111', 'Ioana', 'Georgescu', 'ioana.georgescu@example.com', '+40 723 222 222', '1990-08-21', TRUE, NOW(), TRUE, TRUE, FALSE, TRUE);