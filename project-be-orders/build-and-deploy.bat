@REM Windows Batch Script
@echo off
echo Starting application build and deployment...

REM Step 1: Compile and build Docker image with Maven Jib
echo [STEP 1/2] Building Docker image with Maven Jib...
call mvnw.cmd -DskipTests clean package jib:dockerBuild

REM Check if Maven command was successful
IF ERRORLEVEL 1 (
    echo ERROR: Maven Jib build failed.
    goto :EOF
) ELSE (
    echo Maven Jib build successful.
)

REM Step 2: Start services with Docker Compose
echo [STEP 2/2] Starting services with Docker Compose...
call docker-compose up

REM Check if Docker Compose command was successful
IF ERRORLEVEL 1 (
    echo ERROR: Docker Compose failed to start.
) ELSE (
    echo Docker Compose services started.
)

echo Script finished.
:EOF
pause