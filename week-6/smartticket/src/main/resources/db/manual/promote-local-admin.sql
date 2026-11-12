UPDATE users
SET role = 'ADMIN'
WHERE email = 'admin@local.test';

SELECT user_id, full_name, email, role
FROM users
WHERE email = 'admin@local.test';
