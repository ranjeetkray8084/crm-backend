-- Test Database Connection and Current State
-- Run this script to check your database connection and current table structure

-- Test 1: Check if we can connect and see the database
SELECT 'Database connection test' as test_name;
SELECT DATABASE() as current_database;
SELECT VERSION() as mysql_version;

-- Test 2: Check if notes table exists
SELECT 'Notes table existence test' as test_name;
SHOW TABLES LIKE 'notes';

-- Test 3: Check current notes table structure
SELECT 'Notes table structure test' as test_name;
DESCRIBE notes;

-- Test 4: Check current data in notes table
SELECT 'Notes table data test' as test_name;
SELECT COUNT(*) as total_notes FROM notes;
SELECT visibility, COUNT(*) as count FROM notes GROUP BY visibility;
SELECT status, COUNT(*) as count FROM notes GROUP BY status;
SELECT priority, COUNT(*) as count FROM notes GROUP BY priority;

-- Test 5: Check if announcements table exists
SELECT 'Announcements table existence test' as test_name;
SHOW TABLES LIKE 'announcements';

-- Test 6: Check current announcements table structure
SELECT 'Announcements table structure test' as test_name;
DESCRIBE announcements;

-- Test 7: Check current data in announcements table
SELECT 'Announcements table data test' as test_name;
SELECT COUNT(*) as total_announcements FROM announcements;
SELECT priority, COUNT(*) as count FROM announcements GROUP BY priority;

-- Test 8: Check for any invalid enum values
SELECT 'Invalid enum values check' as test_name;
SELECT 'Notes visibility - Invalid values:' as info;
SELECT DISTINCT visibility FROM notes 
WHERE visibility NOT IN (
    'ALL_ADMIN', 'ALL_USERS', 'ME_AND_ADMIN', 'ME_AND_DIRECTOR', 
    'ONLY_ME', 'SPECIFIC_ADMIN', 'SPECIFIC_USERS'
) OR visibility IS NULL;

SELECT 'Notes status - Invalid values:' as info;
SELECT DISTINCT status FROM notes 
WHERE status NOT IN ('NEW', 'PROCESSING', 'COMPLETED') OR status IS NULL;

SELECT 'Notes priority - Invalid values:' as info;
SELECT DISTINCT priority FROM notes 
WHERE priority NOT IN ('PRIORITY_A', 'PRIORITY_B', 'PRIORITY_C') OR priority IS NULL;

SELECT 'Announcements priority - Invalid values:' as info;
SELECT DISTINCT priority FROM announcements 
WHERE priority NOT IN ('LOW', 'NORMAL', 'HIGH', 'URGENT') OR priority IS NULL;

-- Test 9: Summary
SELECT '=== SUMMARY ===' as info;
SELECT 
    'Notes table' as table_name,
    COUNT(*) as total_records,
    'visibility' as column_name,
    COUNT(DISTINCT visibility) as unique_values
FROM notes
UNION ALL
SELECT 
    'Notes table' as table_name,
    COUNT(*) as total_records,
    'status' as column_name,
    COUNT(DISTINCT status) as unique_values
FROM notes
UNION ALL
SELECT 
    'Notes table' as table_name,
    COUNT(*) as total_records,
    'priority' as column_name,
    COUNT(DISTINCT priority) as unique_values
FROM notes
UNION ALL
SELECT 
    'Announcements table' as table_name,
    COUNT(*) as total_records,
    'priority' as column_name,
    COUNT(DISTINCT priority) as unique_values
FROM announcements;
