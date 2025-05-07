@echo off
title Chat Application Build Script

:: Check if Java is installed
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo Java is not installed or not in PATH
    pause
    exit /b 1
)

:: Create output directory if it doesn't exist
if not exist "bin" mkdir "bin"

:: Clean previous builds
del /q "bin\*.class" >nul 2>nul

:: Compile Server
echo Compiling Server...
javac -d bin Server\Server.java
if %errorlevel% neq 0 (
    echo Failed to compile Server
    pause
    exit /b 1
)

:: Compile Client
echo Compiling Client...
javac -d bin Client\Client.java
if %errorlevel% neq 0 (
    echo Failed to compile Client
    pause
    exit /b 1
)

echo.
echo Build successful! Files compiled to 'bin' directory.
echo.

:: Provide run commands
echo To start the server:
echo   java -cp bin Server.Server
echo   or use runServer.bat
echo.
echo To start a client:
echo   java -cp bin Client.Client
echo   or use runClient.bat
echo.
pause