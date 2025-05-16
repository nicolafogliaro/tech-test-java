
#!/bin/bash

# Enable error handling
set -e

echo "Starting application build and deployment..."

# Step 1: Compile and build Docker image with Maven Jib
echo "[STEP 1/2] Building Docker image with Maven Jib..."
./mvnw -DskipTests clean package jib:dockerBuild

# Check if Maven command was successful
if [ \$? -ne 0 ]; then
    echo "ERROR: Maven Jib build failed."
    exit 1
else
    echo "Maven Jib build successful."
fi

# Step 2: Start services with Docker Compose
echo "[STEP 2/2] Starting services with Docker Compose..."
docker-compose up

# Check if Docker Compose command was successful
if [ \$? -ne 0 ]; then
    echo "ERROR: Docker Compose failed to start."
    exit 1
else
    echo "Docker Compose services started."
fi

echo "Script finished successfully."

