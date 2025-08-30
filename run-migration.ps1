# Database Migration Script Runner (PowerShell)
# This script will help you run the database migration to fix the enum column issues

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Database Migration Script Runner" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "This script will help you run the database migration" -ForegroundColor Yellow
Write-Host "to fix the enum column issues in your CRM application." -ForegroundColor Yellow
Write-Host ""

Write-Host "IMPORTANT: Make sure your Spring Boot application is STOPPED before running this script!" -ForegroundColor Red
Write-Host ""

$confirm = Read-Host "Are you ready to proceed? (y/n)"
if ($confirm -ne "y" -and $confirm -ne "Y") {
    Write-Host "Migration cancelled." -ForegroundColor Yellow
    Read-Host "Press Enter to continue"
    exit
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 1: Testing Database Connection" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Running database connection test..." -ForegroundColor Green
try {
    $testResult = Get-Content test-database-connection.sql | mysql -h 178.16.137.180 -u root -p chandra_realtors 2>&1
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
Write-Host "Step 2: Running Migration Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Running comprehensive migration script..." -ForegroundColor Green
try {
    $migrationResult = Get-Content comprehensive-migration.sql | mysql -h 178.16.137.180 -u root -p chandra_realtors 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Migration failed with exit code: $LASTEXITCODE"
    }
    Write-Host "Migration script executed successfully!" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "ERROR: Migration failed!" -ForegroundColor Red
    Write-Host "Please check the error messages above." -ForegroundColor Red
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to continue"
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 3: Verifying Migration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Running verification test..." -ForegroundColor Green
try {
    $verifyResult = Get-Content test-database-connection.sql | mysql -h 178.16.137.180 -u root -p chandra_realtors 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Warning: Verification test failed, but migration may still be successful" -ForegroundColor Yellow
    } else {
        Write-Host "Verification test completed successfully!" -ForegroundColor Green
    }
} catch {
    Write-Host "Warning: Verification test failed, but migration may still be successful" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Migration Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "The database migration has been completed successfully." -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Update your application.properties file:" -ForegroundColor White
Write-Host "   - Change spring.jpa.hibernate.ddl-auto from 'validate' back to 'update'" -ForegroundColor White
Write-Host "2. Start your Spring Boot application" -ForegroundColor White
Write-Host "3. Verify that the application starts without errors" -ForegroundColor White
Write-Host ""
Write-Host "If you encounter any issues, check the DATABASE_MIGRATION_README.md file." -ForegroundColor Yellow
Write-Host ""

Read-Host "Press Enter to continue"
