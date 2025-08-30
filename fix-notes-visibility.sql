-- Fix Notes Table Visibility Issue
-- This script specifically fixes the visibility column enum mismatch
-- Database: chandra_realtors

-- Step 1: Check current data in notes table
SELECT '=== CURRENT NOTES TABLE STATE ===' as info;
SELECT COUNT(*) as total_notes FROM notes;
SELECT visibility, COUNT(*) as count FROM notes GROUP BY visibility;
SELECT status, COUNT(*) as count FROM notes GROUP BY status;
SELECT priority, COUNT(*) as count FROM notes GROUP BY priority;

-- Step 2: Create backup of notes table
CREATE TABLE IF NOT EXISTS notes_backup_before_fix AS SELECT * FROM notes;
SELECT 'Backup table created: notes_backup_before_fix' as status;

-- Step 3: Check for any invalid visibility values
SELECT '=== INVALID VISIBILITY VALUES ===' as info;
SELECT DISTINCT visibility FROM notes 
WHERE visibility NOT IN (
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) OR visibility IS NULL;

-- Step 4: Update any invalid visibility values to safe defaults
UPDATE notes 
SET visibility = 'ONLY_ME' 
WHERE visibility NOT IN (
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) OR visibility IS NULL;

SELECT 'Invalid visibility values updated to ONLY_ME' as status;

-- Step 5: Update any NULL values to default
UPDATE notes SET visibility = 'ONLY_ME' WHERE visibility IS NULL;
SELECT 'NULL visibility values updated to ONLY_ME' as status;

-- Step 6: Check for any invalid status values
SELECT '=== INVALID STATUS VALUES ===' as info;
SELECT DISTINCT status FROM notes 
WHERE status NOT IN ('NEW', 'PROCESSING', 'COMPLETED') OR status IS NULL;

-- Step 7: Update invalid status values to safe defaults
UPDATE notes 
SET status = 'NEW' 
WHERE status NOT IN ('NEW', 'PROCESSING', 'COMPLETED') OR status IS NULL;

UPDATE notes SET status = 'NEW' WHERE status IS NULL;
SELECT 'Invalid status values updated to NEW' as status;

-- Step 8: Check for any invalid priority values
SELECT '=== INVALID PRIORITY VALUES ===' as info;
SELECT DISTINCT priority FROM notes 
WHERE priority NOT IN ('PRIORITY_A', 'PRIORITY_B', 'PRIORITY_C') OR priority IS NULL;

-- Step 9: Update invalid priority values to safe defaults
UPDATE notes 
SET priority = 'PRIORITY_B' 
WHERE priority NOT IN ('PRIORITY_A', 'PRIORITY_B', 'PRIORITY_C') OR priority IS NULL;

UPDATE notes SET priority = 'PRIORITY_B' WHERE priority IS NULL;
SELECT 'Invalid priority values updated to PRIORITY_B' as status;

-- Step 10: Now safely modify the visibility column
-- First, change to VARCHAR to avoid enum constraint issues
ALTER TABLE notes MODIFY COLUMN visibility VARCHAR(50) NOT NULL DEFAULT 'ONLY_ME';
SELECT 'Visibility column changed to VARCHAR' as status;

-- Step 11: Update the column to use the exact enum values
ALTER TABLE notes MODIFY COLUMN visibility ENUM(
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) NOT NULL DEFAULT 'ONLY_ME';
SELECT 'Visibility column updated to use enum' as status;

-- Step 12: Safely modify status column
ALTER TABLE notes MODIFY COLUMN status ENUM('NEW', 'PROCESSING', 'COMPLETED') NOT NULL DEFAULT 'NEW';
SELECT 'Status column updated to use enum' as status;

-- Step 13: Safely modify priority column
ALTER TABLE notes MODIFY COLUMN priority ENUM('PRIORITY_A', 'PRIORITY_B', 'PRIORITY_C') NOT NULL DEFAULT 'PRIORITY_B';
SELECT 'Priority column updated to use enum' as status;

-- Step 14: Verify the migration was successful
SELECT '=== MIGRATION VERIFICATION ===' as info;

SELECT 'Notes table - Final visibility values:' as info;
SELECT visibility, COUNT(*) as count FROM notes GROUP BY visibility;

SELECT 'Notes table - Final status values:' as info;
SELECT status, COUNT(*) as count FROM notes GROUP BY status;

SELECT 'Notes table - Final priority values:' as info;
SELECT priority, COUNT(*) as count FROM notes GROUP BY priority;

-- Step 15: Show final table structure
SELECT '=== FINAL TABLE STRUCTURE ===' as info;
DESCRIBE notes;

-- Step 16: Count total records to ensure no data loss
SELECT '=== DATA COUNT VERIFICATION ===' as info;
SELECT 'Notes table total records:' as table_name, COUNT(*) as count FROM notes
UNION ALL
SELECT 'Notes backup total records:' as table_name, COUNT(*) as count FROM notes_backup_before_fix;

-- Step 17: Clean up backup table if everything looks good (optional)
-- DROP TABLE notes_backup_before_fix;

SELECT '=== NOTES TABLE MIGRATION COMPLETED SUCCESSFULLY ===' as info;
