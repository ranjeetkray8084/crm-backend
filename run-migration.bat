@echo off
echo ========================================
echo Database Migration Script Runner
echo ========================================
echo.

echo This script will help you run the database migration
echo to fix the enum column issues in your CRM application.
echo.

echo IMPORTANT: Make sure your Spring Boot application is STOPPED before running this script!
echo.

set /p confirm="Are you ready to proceed? (y/n): "
if /i not "%confirm%"=="y" (
    echo Migration cancelled.
    pause
    exit /b
)

echo.
echo ========================================
echo Step 1: Testing Database Connection
echo ========================================
echo.

echo Running database connection test...
mysql -h 178.16.137.180 -u root -p chandra_realtors < test-database-connection.sql

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Database connection failed!
    echo Please check your database credentials and network connection.
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 2: Running Migration Script
echo ========================================
echo.

echo Running comprehensive migration script...
mysql -h 178.16.137.180 -u root -p chandra_realtors < comprehensive-migration.sql

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Migration failed!
    echo Please check the error messages above.
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Step 3: Verifying Migration
echo ========================================
echo.

echo Running verification test...
mysql -h 178.16.137.180 -u root -p chandra_realtors < test-database-connection.sql

echo.
echo ========================================
echo Migration Complete!
echo ========================================
echo.

echo The database migration has been completed successfully.
echo.
echo Next steps:
echo 1. Update your application.properties file:
echo    - Change spring.jpa.hibernate.ddl-auto from 'validate' back to 'update'
echo 2. Start your Spring Boot application
echo 3. Verify that the application starts without errors
echo.
echo If you encounter any issues, check the DATABASE_MIGRATION_README.md file.
echo.

pause
