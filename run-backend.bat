@echo off
echo ===================================================
echo Starting Lab Resource Platform - Spring Boot Backend
echo ===================================================
cd /d "%~dp0backend"
call .\mvnw.cmd spring-boot:run
pause
