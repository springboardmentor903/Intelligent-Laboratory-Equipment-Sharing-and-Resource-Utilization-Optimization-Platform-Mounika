Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Starting Lab Resource Platform - Spring Boot Backend" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Set-Location -Path "$PSScriptRoot\backend"
& .\mvnw.cmd spring-boot:run
