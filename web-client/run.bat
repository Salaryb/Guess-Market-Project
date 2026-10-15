@echo off
REM Guess Market web client (exercise 4).
REM Requires Node.js, and Tomcat running with guessmarket.war (http://localhost:8080/guessmarket).
cd /d "%~dp0"
REM Opens the browser after 2 seconds, when the web server is already up
start "" /b cmd /c "timeout /t 2 /nobreak >nul & start http://localhost:3000"
node server.js
pause
