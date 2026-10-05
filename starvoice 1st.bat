@echo off
title StarVoice Lanka - Server Launcher
echo ===================================================
echo           STARVOICE LANKA - SERVER LAUNCHER        
echo ===================================================
echo.
echo Starting application with Maven (port 4000)...
echo When you see 'Started StarVoiceLankaApplication', open your browser:
echo Application URL: http://localhost:4000
echo Admin Login:     admin@starvoice.lk / admin12345
echo.
echo ===================================================
mvn spring-boot:run
pause
