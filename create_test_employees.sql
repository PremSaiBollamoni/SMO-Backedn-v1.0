-- Create test employees for E2E testing
-- Employee IDs: 2001-2018

SET @role_id = 1; -- Assuming role_id 1 exists for operator/worker role

INSERT INTO employee (emp_id, name, role_id, phone, email, emp_date, status, created_at)
VALUES
  (2001, 'Test Employee 001', @role_id, '9999900001', 'emp2001@test.com', CURDATE(), 'active', NOW()),
  (2002, 'Test Employee 002', @role_id, '9999900002', 'emp2002@test.com', CURDATE(), 'active', NOW()),
  (2003, 'Test Employee 003', @role_id, '9999900003', 'emp2003@test.com', CURDATE(), 'active', NOW()),
  (2004, 'Test Employee 004', @role_id, '9999900004', 'emp2004@test.com', CURDATE(), 'active', NOW()),
  (2005, 'Test Employee 005', @role_id, '9999900005', 'emp2005@test.com', CURDATE(), 'active', NOW()),
  (2006, 'Test Employee 006', @role_id, '9999900006', 'emp2006@test.com', CURDATE(), 'active', NOW()),
  (2007, 'Test Employee 007', @role_id, '9999900007', 'emp2007@test.com', CURDATE(), 'active', NOW()),
  (2008, 'Test Employee 008', @role_id, '9999900008', 'emp2008@test.com', CURDATE(), 'active', NOW()),
  (2009, 'Test Employee 009', @role_id, '9999900009', 'emp2009@test.com', CURDATE(), 'active', NOW()),
  (2010, 'Test Employee 010', @role_id, '9999900010', 'emp2010@test.com', CURDATE(), 'active', NOW()),
  (2011, 'Test Employee 011', @role_id, '9999900011', 'emp2011@test.com', CURDATE(), 'active', NOW()),
  (2012, 'Test Employee 012', @role_id, '9999900012', 'emp2012@test.com', CURDATE(), 'active', NOW()),
  (2013, 'Test Employee 013', @role_id, '9999900013', 'emp2013@test.com', CURDATE(), 'active', NOW()),
  (2014, 'Test Employee 014', @role_id, '9999900014', 'emp2014@test.com', CURDATE(), 'active', NOW()),
  (2015, 'Test Employee 015', @role_id, '9999900015', 'emp2015@test.com', CURDATE(), 'active', NOW()),
  (2016, 'Test Employee 016', @role_id, '9999900016', 'emp2016@test.com', CURDATE(), 'active', NOW()),
  (2017, 'Test Employee 017', @role_id, '9999900017', 'emp2017@test.com', CURDATE(), 'active', NOW()),
  (2018, 'Test Employee 018', @role_id, '9999900018', 'emp2018@test.com', CURDATE(), 'active', NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = 'active',
  created_at = NOW();

-- Also create employees with lower IDs in case they're needed
INSERT INTO employee (emp_id, name, role_id, phone, email, emp_date, status, created_at)
VALUES
  (1001, 'Test Employee 1001', @role_id, '9999901001', 'emp1001@test.com', CURDATE(), 'active', NOW()),
  (1002, 'Test Employee 1002', @role_id, '9999901002', 'emp1002@test.com', CURDATE(), 'active', NOW()),
  (1003, 'Test Employee 1003', @role_id, '9999901003', 'emp1003@test.com', CURDATE(), 'active', NOW()),
  (1004, 'Test Employee 1004', @role_id, '9999901004', 'emp1004@test.com', CURDATE(), 'active', NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  status = 'active',
  created_at = NOW();
