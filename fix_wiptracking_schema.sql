-- Fix for wiptracking table: Add AUTO_INCREMENT to wip_id column
-- Error: "Field 'wip_id' doesn't have a default value"
-- Root cause: wip_id is defined as PRIMARY KEY but lacks AUTO_INCREMENT,
-- preventing Hibernate's @GeneratedValue(strategy = GenerationType.IDENTITY) from working

-- Run this SQL directly on your database to fix:
ALTER TABLE wiptracking MODIFY COLUMN wip_id BIGINT NOT NULL AUTO_INCREMENT;

-- Verify the fix:
SHOW CREATE TABLE wiptracking;
