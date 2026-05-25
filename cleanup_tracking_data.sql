-- CLEANUP: Delete all tracking-related data for fresh start

-- Delete QR Events
DELETE FROM qr_event;

-- Delete WIP Tracking
DELETE FROM wiptracking;

-- Delete Bin Merge History
DELETE FROM bin_merge_history;

-- Delete Bin Assignment History
DELETE FROM bin_assignment_history;

-- Delete Bins
DELETE FROM bin;

-- Reset auto-increment counters
ALTER TABLE bin AUTO_INCREMENT = 1;
ALTER TABLE wiptracking AUTO_INCREMENT = 1;
ALTER TABLE bin_assignment_history AUTO_INCREMENT = 1;
ALTER TABLE bin_merge_history AUTO_INCREMENT = 1;

SELECT "✓ All tracking data cleaned. Ready for fresh test." AS status;
