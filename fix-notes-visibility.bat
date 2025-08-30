@echo off
echo ========================================
echo Fix Notes Table Visibility Issue
echo ========================================
echo.

echo This script will fix the notes table visibility enum mismatch
echo that is preventing your Spring Boot application from starting.
echo.

echo IMPORTANT: Make sure your Spring Boot application is STOPPED before running this script!
echo.

set /p confirm="Are you ready to proceed? (y/n): "
if /i not "%confirm%"=="y" (
    echo Fix cancelled.
    pause
    exit /b
)

echo.
echo ========================================
echo Step 1: Testing Database Connection
echo ========================================
echo.

echo Testing database connection...
mysql -h 178.16.137.180 -u root -p chandra_realtors -e "SELECT 'Connection successful' as status;"

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Database connection failed!
    echo Please check your database credentials and network connection.
    echo.
    pause
    exit /b 1
)

echo Database connection test successful!
echo.

echo ========================================
echo Step 2: Running Notes Visibility Fix
echo ========================================
echo.

echo Running notes visibility fix script...
mysql -h 178.16.137.180 -u root -p chandra_realtors < fix-notes-visibility.sql

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Notes visibility fix failed!
    echo Please check the error messages above.
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Fix Completed Successfully!
echo ========================================
echo.

echo The notes table visibility issue has been fixed!
echo.
echo Next steps:
echo 1. Update your application.properties file:
echo    - Change spring.jpa.hibernate.ddl-auto from 'validate' back to 'update'
echo 2. Start your Spring Boot application
echo 3. Verify that the application starts without errors
echo.
echo The migration script has:
echo - Created a backup of your notes table
echo - Fixed all invalid enum values
echo - Updated the table schema to use proper enums
echo - Preserved all your existing data
echo.

pause
