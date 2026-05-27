-- Clean up wiptracking table to reset for fresh testing
DELETE FROM wiptracking WHERE 1=1;

-- Reset auto-increment
ALTER TABLE wiptracking AUTO_INCREMENT = 1;

-- Verify cleanup
SELECT COUNT(*) as total_wip_records FROM wiptracking;
