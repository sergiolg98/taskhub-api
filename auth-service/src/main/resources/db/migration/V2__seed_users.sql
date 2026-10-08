-- Usuarios de ejemplo para la versión base. El campo password es un valor de relleno:
-- el cifrado y el login se implementan en la clase 1 (Spring Security + JWT).
INSERT INTO users (name, email, password, role, created_at) VALUES
    ('Ana Admin', 'ana@taskhub.com', 'PLACEHOLDER', 'ADMIN', CURRENT_TIMESTAMP),
    ('Luis User', 'luis@taskhub.com', 'PLACEHOLDER', 'USER', CURRENT_TIMESTAMP);
