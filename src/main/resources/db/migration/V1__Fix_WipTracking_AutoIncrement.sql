-- Fix wiptracking table: add AUTO_INCREMENT to wip_id if missing
-- This allows Hibernate's @GeneratedValue(strategy = GenerationType.IDENTITY) to work correctly

ALTER TABLE wiptracking MODIFY COLUMN wip_id BIGINT AUTO_INCREMENT;
