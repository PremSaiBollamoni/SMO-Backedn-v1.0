-- Clear all tracking-related data for fresh testing
-- This will reset the database to a clean state for end-to-end testing

-- 1. Clear WIP Tracking
DELETE FROM wiptracking WHERE 1=1;
ALTER TABLE wiptracking AUTO_INCREMENT = 1;

-- 2. Clear Temp Active Assignments
DELETE FROM temp_active_assignment WHERE 1=1;
ALTER TABLE temp_active_assignment AUTO_INCREMENT = 1;

-- 3. Clear Temp Assignment Logs
DELETE FROM temp_assignment_log WHERE 1=1;
ALTER TABLE temp_assignment_log AUTO_INCREMENT = 1;

-- 4. Clear Bin Assignment History
DELETE FROM bin_assignment_history WHERE 1=1;
ALTER TABLE bin_assignment_history AUTO_INCREMENT = 1;

-- 5. Clear Bin Merge History
DELETE FROM bin_merge_history WHERE 1=1;
ALTER TABLE bin_merge_history AUTO_INCREMENT = 1;

-- 6. Clear Temp Bin Merge
DELETE FROM temp_bin_merge WHERE 1=1;
ALTER TABLE temp_bin_merge AUTO_INCREMENT = 1;

-- 7. Clear QR Events
DELETE FROM qr_event WHERE 1=1;
ALTER TABLE qr_event AUTO_INCREMENT = 1;

-- 8. Reset Bins to initial state (remove current_operation_id)
UPDATE bin SET current_operation_id = NULL, status = 'new' WHERE 1=1;

-- 9. Clear Orders (optional - only if you want to start fresh)
-- DELETE FROM `order` WHERE 1=1;
-- ALTER TABLE `order` AUTO_INCREMENT = 1;

-- Verify cleanup
SELECT 'wiptracking' as table_name, COUNT(*) as count FROM wiptracking
UNION ALL
SELECT 'temp_active_assignment', COUNT(*) FROM temp_active_assignment
UNION ALL
SELECT 'temp_assignment_log', COUNT(*) FROM temp_assignment_log
UNION ALL
SELECT 'bin_assignment_history', COUNT(*) FROM bin_assignment_history
UNION ALL
SELECT 'bin_merge_history', COUNT(*) FROM bin_merge_history
UNION ALL
SELECT 'temp_bin_merge', COUNT(*) FROM temp_bin_merge
UNION ALL
SELECT 'qr_event', COUNT(*) FROM qr_event
UNION ALL
SELECT 'bin (with tracking)', COUNT(*) FROM bin WHERE current_operation_id IS NOT NULL;
