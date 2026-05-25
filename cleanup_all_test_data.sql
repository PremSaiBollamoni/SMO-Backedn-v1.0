-- COMPREHENSIVE CLEANUP: Remove all test data created during E2E testing

-- Delete QR Events (if exists)
DELETE FROM qr_event WHERE 1=1;

-- Delete WIP Tracking
DELETE FROM wiptracking WHERE 1=1;

-- Delete Bin Merge History
DELETE FROM bin_merge_history WHERE 1=1;

-- Delete Bin Assignment History
DELETE FROM bin_assignment_history WHERE 1=1;

-- Delete Bins
DELETE FROM bin WHERE 1=1;

-- Delete test employee logins first (foreign key dependency)
DELETE FROM login WHERE emp_id BETWEEN 2001 AND 2018;

-- Delete test employees (2001-2018)
DELETE FROM employee WHERE emp_id BETWEEN 2001 AND 2018;

-- Reset auto-increment counters
ALTER TABLE bin AUTO_INCREMENT = 1;
ALTER TABLE wiptracking AUTO_INCREMENT = 1;
ALTER TABLE bin_assignment_history AUTO_INCREMENT = 1;
ALTER TABLE bin_merge_history AUTO_INCREMENT = 1;

SELECT "✓ All test data cleaned. Database ready for fresh start." AS status;
