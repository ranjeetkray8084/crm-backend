-- Comprehensive Database Migration Script
-- This script will safely update all enum columns without losing any data
-- Run this script BEFORE starting your Spring Boot application
-- Database: chandra_realtors

-- ========================================
-- NOTES TABLE MIGRATION
-- ========================================

-- Step 1: Check current data in notes table
SELECT 'Notes table - Current visibility values:' as info;
SELECT DISTINCT visibility FROM notes WHERE visibility IS NOT NULL;

SELECT 'Notes table - Current status values:' as info;
SELECT DISTINCT status FROM notes WHERE status IS NOT NULL;

SELECT 'Notes table - Current priority values:' as info;
SELECT DISTINCT priority FROM notes WHERE priority IS NOT NULL;

-- Step 2: Create backup of notes table
CREATE TABLE IF NOT EXISTS notes_backup AS SELECT * FROM notes;

-- Step 3: Update invalid visibility values to safe defaults
UPDATE notes 
SET visibility = 'ONLY_ME' 
WHERE visibility NOT IN (
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) OR visibility IS NULL;

-- Step 4: Update invalid status values to safe defaults
UPDATE notes 
SET status = 'NEW' 
WHERE status NOT IN ('NEW', 'PROCESSING', 'COMPLETED') OR status IS NULL;

-- Step 5: Update invalid priority values to safe defaults
UPDATE notes 
SET priority = 'PRIORITY_B' 
WHERE priority NOT IN ('PRIORITY_A', 'PRIORITY_B', 'PRIORITY_C') OR priority IS NULL;

-- Step 6: Safely modify visibility column
ALTER TABLE notes MODIFY COLUMN visibility VARCHAR(50) NOT NULL DEFAULT 'ONLY_ME';
ALTER TABLE notes MODIFY COLUMN visibility ENUM(
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) NOT NULL DEFAULT 'ONLY_ME';

-- Step 7: Safely modify status column
ALTER TABLE notes MODIFY COLUMN status ENUM('NEW', 'PROCESSING', 'COMPLETED') NOT NULL DEFAULT 'NEW';

-- Step 8: Safely modify priority column
ALTER TABLE notes MODIFY COLUMN priority ENUM('PRIORITY_A', 'PRIORITY_B', 'PRIORITY_C') NOT NULL DEFAULT 'PRIORITY_B';

-- ========================================
-- ANNOUNCEMENTS TABLE MIGRATION
-- ========================================

-- Step 9: Create announcements table if it doesn't exist
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    company_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL',
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_company_active (company_id, is_active),
    INDEX idx_priority (priority),
    INDEX idx_expires (expires_at)
);

-- Step 10: Check current data in announcements table
SELECT 'Announcements table - Current priority values:' as info;
SELECT DISTINCT priority FROM announcements WHERE priority IS NOT NULL;

-- Step 11: Create backup of announcements table
CREATE TABLE IF NOT EXISTS announcements_backup AS SELECT * FROM announcements;

-- Step 12: Update invalid priority values to safe defaults
UPDATE announcements 
SET priority = 'NORMAL' 
WHERE priority NOT IN ('LOW', 'NORMAL', 'HIGH', 'URGENT') OR priority IS NULL;

-- Step 13: Safely modify priority column
ALTER TABLE announcements MODIFY COLUMN priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') NOT NULL DEFAULT 'NORMAL';

-- ========================================
-- VERIFICATION
-- ========================================

-- Step 14: Verify all migrations were successful
SELECT '=== MIGRATION VERIFICATION ===' as info;

SELECT 'Notes table - Final visibility values:' as info;
SELECT visibility, COUNT(*) as count FROM notes GROUP BY visibility;

SELECT 'Notes table - Final status values:' as info;
SELECT status, COUNT(*) as count FROM notes GROUP BY status;

SELECT 'Notes table - Final priority values:' as info;
SELECT priority, COUNT(*) as count FROM notes GROUP BY priority;

SELECT 'Announcements table - Final priority values:' as info;
SELECT priority, COUNT(*) as count FROM announcements GROUP BY priority;

-- Step 15: Show final table structures
SELECT '=== FINAL TABLE STRUCTURES ===' as info;
DESCRIBE notes;
DESCRIBE announcements;

-- Step 16: Count total records to ensure no data loss
SELECT '=== DATA COUNT VERIFICATION ===' as info;
SELECT 'Notes table total records:' as table_name, COUNT(*) as count FROM notes
UNION ALL
SELECT 'Notes backup total records:' as table_name, COUNT(*) as count FROM notes_backup
UNION ALL
SELECT 'Announcements table total records:' as table_name, COUNT(*) as count FROM announcements
UNION ALL
SELECT 'Announcements backup total records:' as table_name, COUNT(*) as count FROM announcements_backup;

-- Step 17: Clean up backup tables if everything looks good (optional)
-- DROP TABLE notes_backup;
-- DROP TABLE announcements_backup;

SELECT '=== MIGRATION COMPLETED SUCCESSFULLY ===' as info;
