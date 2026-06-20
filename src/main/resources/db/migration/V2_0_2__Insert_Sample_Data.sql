-- Insert sample roles with correct activities

INSERT INTO role (role_id, role_name, activities, status, created_at, updated_at) VALUES
(1, 'HR', 'HR_DASHBOARD,HR_MANAGE_ROLES,HR_MANAGE_EMPLOYEES,HR_ATTENDANCE_REPORT', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO role (role_id, role_name, activities, status, created_at, updated_at) VALUES
(2, 'SUPERVISOR', 'SUPERVISOR_WORK_ASSIGNMENT,SUPERVISOR_EFFICIENCY,SUPERVISOR_HISTORY,SUPERVISOR_ATTENDANCE,SUPERVISOR_LINE_BALANCING', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO role (role_id, role_name, activities, status, created_at, updated_at) VALUES
(3, 'GM', 'GM_VIEW_PRODUCTION,GM_VIEW_REPORTS,GM_VIEW_EFFICIENCY', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Insert sample employees
INSERT INTO employee_info (emp_id, emp_name, emp_email, emp_phone, emp_department, emp_designation, emp_status, created_at, updated_at) VALUES
(1001, 'Rajesh Kumar', 'rajesh@smo.com', '9876543210', 'HR', 'HR Manager', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employee_info (emp_id, emp_name, emp_email, emp_phone, emp_department, emp_designation, emp_status, created_at, updated_at) VALUES
(1002, 'Priya Singh', 'priya@smo.com', '9876543211', 'Production', 'Supervisor', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employee_info (emp_id, emp_name, emp_email, emp_phone, emp_department, emp_designation, emp_status, created_at, updated_at) VALUES
(1003, 'Amit Patel', 'amit@smo.com', '9876543212', 'Management', 'General Manager', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employee_info (emp_id, emp_name, emp_email, emp_phone, emp_department, emp_designation, emp_status, created_at, updated_at) VALUES
(1004, 'Sneha Verma', 'sneha@smo.com', '9876543213', 'Operations', 'Operator', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employee_info (emp_id, emp_name, emp_email, emp_phone, emp_department, emp_designation, emp_status, created_at, updated_at) VALUES
(1005, 'Vikram Nair', 'vikram@smo.com', '9876543214', 'Quality', 'QC Inspector', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- Assign roles to employees
INSERT INTO employee_role (emp_id, role_id, created_at, updated_at) VALUES
(1001, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employee_role (emp_id, role_id, created_at, updated_at) VALUES
(1002, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

INSERT INTO employee_role (emp_id, role_id, created_at, updated_at) VALUES
(1003, 3, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
