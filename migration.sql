-- MIGRATION: Delete current test employees, import real ones from SMO
-- IMPORTANT: Roles table is NOT deleted

-- Step 1: Delete relationships first (foreign key constraints)
DELETE FROM employee_role;
DELETE FROM employee_login;
DELETE FROM employee_info;

-- Step 2: Insert real employees from SMO (with encrypted fields)
INSERT INTO employee_info (emp_id, emp_name, emp_email, encrypted_phone, encrypted_aadhar_number, encrypted_pan_card_number, encrypted_dob, emp_department, emp_status, created_at) VALUES
(1001, 'Prem Sai Bollamoni', 'bollamonipremsai@gmail.com', '8074850696', 'UeHxHhcT6Puc+aoC:h2BqLzWvXCChk7OTIqMlOWfQ9JO47TSWryEZ8w==', 'PX1TSg6+OZ3NFbEx:EBAhB3KRHjD8LddnpprAFxCnqT4Yt3SEMl8=', '2004-08-20', 'HR', 'ACTIVE', NOW()),
(1002, 'Supervisor One', 'supervisor@smo.local', '9000000002', NULL, NULL, '1992-01-01', 'Operations', 'ACTIVE', NOW()),
(1003, 'General Manager', 'gm@smo.local', '9000000003', NULL, NULL, '1992-01-01', 'Management', 'ACTIVE', NOW()),
(1004, 'Process Planner', 'planner@smo.local', '9000000004', NULL, NULL, '1992-01-01', 'Planning', 'ACTIVE', NOW()),
(1005, 'V MAHENDRA', 'vanummahendra@gmail.com', '7032964167', '5fAyDewpScKjmCpg:5Gj7APkTkb1Hy8E+3DQULy8=', 'yv+NJMBN7n9AtH+/:OMUmi4sXmZ0wtgTiqBL8okc=', '2005-08-10', 'Operations', 'ACTIVE', NOW()),
(1006, 'K SAI', 'konerusai727@gmail.com', '9492389215', 'Ei6YGrePOg6S4wYn:UOG15o9WQaAnMxx7y3cy958=', 'Ge2jqMft8Ftzj5Ix:YrmX14yEyn9Qa3RNqRmmHas=', '2005-01-09', 'Operations', 'ACTIVE', NOW()),
(1007, 'B CHIRU VENKATA SATYA SAI', 'chiruvenkatasatyasaibalivada@gmail.com', '6301035380', 'I+G+gcQJTh/vyt88:FKJbv5KK4uEcUU6egOFWZWQ=', 'yENW9xcwODvf/5iP:uwsJ6fS5udlRxv8nOGZ3Qsw=', '2005-12-11', 'Operations', 'ACTIVE', NOW()),
(1008, 'P SUNDARI', 'smo1008@gmail.com', '7675964756', 'KTAaWZgfQziy55OB:whxgBnGdyuaT0BI0+JJT2GA=', 'BytpwAT+9Dw2js4M:miPMI2UTqOEOzNSmONj97zw=', '1979-12-24', 'Operations', 'ACTIVE', NOW()),
(1009, 'sk.haseena', 'smo1009@gmail.com', '6302994336', 'Xv+pKT1P8UAJbHHg:bxy0L6iA0IAZtDzXDWyO7m87', 'oqMCEwPS96YNQp1t:8ZtgZFKKX+8UhNfA4ccE1Cla', '2007-10-20', 'Operations', 'ACTIVE', NOW()),
(1010, 'Morapina Swapna', 'smo1010@gmail.com', '9032036006', 'Zmr+x1A/H63RfWCW:PgL0FtNxjKGK/R/FSVsdL+0=', 'eZs+kUNTi08ww67a:j8SLIZwiayvMQBG/nF4XY90=', '1989-06-10', 'Operations', 'ACTIVE', NOW());

-- Step 3: Insert login credentials with BCrypt hashing
-- Note: Passwords from SMO need BCrypt hashing (using pre-hashed versions for now)
-- Format: \\\$ is BCrypt hash prefix
INSERT INTO employee_login (emp_id, login_id, password_hash, status, created_at) VALUES
(1001, '1001', '\\\', 'ACTIVE', NOW()),
(1002, '1002', '\\\', 'ACTIVE', NOW()),
(1003, '1003', '\\\', 'ACTIVE', NOW()),
(1004, '1004', '\\\', 'ACTIVE', NOW()),
(1005, '1005', '\\\', 'ACTIVE', NOW()),
(1006, '1006', '\\\', 'ACTIVE', NOW()),
(1007, '1007', '\\\', 'ACTIVE', NOW()),
(1008, '1008', '\\\', 'ACTIVE', NOW()),
(1009, '1009', '\\\', 'ACTIVE', NOW()),
(1010, '1010', '\\\', 'ACTIVE', NOW());

-- Step 4: Assign roles (based on role_id from SMO)
INSERT INTO employee_role (emp_id, role_id, created_at) VALUES
(1001, 1, NOW()),  -- HR
(1002, 2, NOW()),  -- SUPERVISOR
(1003, 4, NOW()),  -- GM
(1004, 5, NOW()),  -- Process Planner
(1005, 2, NOW()),  -- SUPERVISOR
(1006, 2, NOW()),  -- SUPERVISOR
(1007, 2, NOW()),  -- SUPERVISOR
(1008, 2, NOW()),  -- SUPERVISOR
(1009, 2, NOW()),  -- SUPERVISOR
(1010, 2, NOW());  -- SUPERVISOR

-- Verify
SELECT COUNT(*) as total_employees FROM employee_info;
SELECT COUNT(*) as total_logins FROM employee_login;
SELECT COUNT(*) as total_roles FROM role;
