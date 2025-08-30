-- Safe Migration Script for Notes Table
-- This script will safely update the notes table without losing any data
-- Run this script BEFORE starting your Spring Boot application

-- Step 1: Check current data in visibility column
SELECT DISTINCT visibility FROM notes WHERE visibility IS NOT NULL;

-- Step 2: Create a backup of the notes table (optional but recommended)
CREATE TABLE notes_backup AS SELECT * FROM notes;

-- Step 3: Update any invalid visibility values to a safe default
-- First, let's see what values exist that might not match our enum
UPDATE notes 
SET visibility = 'ONLY_ME' 
WHERE visibility NOT IN (
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) OR visibility IS NULL;

-- Step 4: Update any NULL values to default
UPDATE notes SET visibility = 'ONLY_ME' WHERE visibility IS NULL;

-- Step 5: Now safely modify the column to use the enum
-- First, change to VARCHAR to avoid enum constraint issues
ALTER TABLE notes MODIFY COLUMN visibility VARCHAR(50) NOT NULL DEFAULT 'ONLY_ME';

-- Step 6: Update the column to use the exact enum values
ALTER TABLE notes MODIFY COLUMN visibility ENUM(
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) NOT NULL DEFAULT 'ONLY_ME';

-- Step 7: Verify the migration was successful
SELECT COUNT(*) as total_notes FROM notes;
SELECT visibility, COUNT(*) as count FROM notes GROUP BY visibility;

-- Step 8: Clean up backup table if everything looks good (optional)
-- DROP TABLE notes_backup;

-- Step 9: Show final table structure
DESCRIBE notes;
