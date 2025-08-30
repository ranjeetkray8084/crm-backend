# Fix Notes Table Visibility Issue
# This script specifically fixes the visibility column enum mismatch

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Fix Notes Table Visibility Issue" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This script will fix the notes table visibility enum mismatch" -ForegroundColor Yellow
Write-Host "that is preventing your Spring Boot application from starting." -ForegroundColor Yellow
Write-Host ""

Write-Host "IMPORTANT: Make sure your Spring Boot application is STOPPED before running this script!" -ForegroundColor Red
Write-Host ""

$confirm = Read-Host "Are you ready to proceed? (y/n)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "Fix cancelled." -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
    exit
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 1: Testing Database Connection" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Testing database connection..." -ForegroundColor Green
try {
    $testResult = mysql -h 178.16.137.180 -u root -p chandra_realtors -e "SELECT 'Connection successful' as status;" 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Database connection failed with exit code: $LASTEXITCODE"
    }
    Write-Host "Database connection test successful!" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "ERROR: Database connection failed!" -ForegroundColor Red
    Write-Host "Please check your database credentials and network connection." -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 2: Running Notes Visibility Fix" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Running notes visibility fix script..." -ForegroundColor Green
try {
    $fixResult = Get-Content fix-notes-visibility.sql | mysql -h 178.16.137.180 -u root -p chandra_realtors 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Fix failed with exit code: $LASTEXITCODE"
    }
    Write-Host "Notes visibility fix completed successfully!" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "ERROR: Notes visibility fix failed!" -ForegroundColor Red
    Write-Host "Please check the error messages above." -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Fix Completed Successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "The notes table visibility issue has been fixed!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Update your application.properties file:" -ForegroundColor White
Write-Host "   - Change spring.jpa.hibernate.ddl-auto from 'validate' back to 'update'" -ForegroundColor White
Write-Host "2. Start your Spring Boot application" -ForegroundColor White
Write-Host "3. Verify that the application starts without errors" -ForegroundColor White
Write-Host ""
Write-Host "The migration script has:" -ForegroundColor Yellow
Write-Host "- Created a backup of your notes table" -ForegroundColor White
Write-Host "- Fixed all invalid enum values" -ForegroundColor White
Write-Host "- Updated the table schema to use proper enums" -ForegroundColor White
Write-Host "- Preserved all your existing data" -ForegroundColor White
Write-Host ""

Read-Host "Press Enter to continue"
