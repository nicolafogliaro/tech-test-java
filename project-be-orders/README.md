# Docker Compose Setup for Spring Boot "order-service" microservice

This setup provides a ready-to-use Docker Compose configuration for developing the `project-be-orders`
Spring Boot microservice with:

* MariaDB,
* Adminer (DB GUI) (added by me for debugging purpose),
* MeiliSearch,
* Redis.

----

## Services Overview

| Service     | Image                      | Purpose                     | Credentials                                                                               | Port |
|-------------|----------------------------|-----------------------------|-------------------------------------------------------------------------------------------|------|
| db          | mariadb:11.4               | Database for orders service | User: `ordersapp` / `orderspass`<br>Root: `root` / `rootpassword`<br>Database: `ordersdb` | 3306 |
| adminer     | adminer                    | Database web GUI            | N/A                                                                                       | 8080 |
| meilisearch | getmeili/meilisearch:v1.12 | Search service              | N/A                                                                                       | 7700 |
| redis       | redis:7.2.4                | Caching/message broker      | N/A (no password)                                                                         | 6379 |

----

### Database Configuration Details

- **Application Database:** `ordersdb`
- **Application User:** `ordersapp`
- **User Password:** `orderspass`
- **Root Password:** `rootpassword`
- **Data Persistence:** Uses a named volume `db_data` to ensure your MariaDB data persists across restarts.

# Application Build and Deployment Scripts

This guide explains how to use the provided scripts for building the application with Maven (Jib integration) and
deploying it using Docker Compose. The scripts are tailored for **Windows** (Batch script) and **Linux/MacOS** (Bash
script).

---

## Prerequisites

### Ensure the following tools are installed on your system:

1. **Java Development Kit (JDK)** (Version 21 or above is recommended)
2. **Maven** (or ensure the `mvnw` wrapper provided in the project is usable)
3. **Docker** (with Docker Compose installed and configured)

---

## Script Overview

### **Steps Executed by the Script**:

1. **Build the Docker image:**
    - Uses Maven's `jib:dockerBuild` goal to compile the application and package it directly into a Docker image.
    - Skips tests during the build process (`-DskipTests` flag).

2. **Deploy services using Docker Compose:**
    - Uses `docker-compose.yml` to spin up the required services (e.g., the main application, database, Redis, etc.).

3. **Error handling:**
    - The scripts exit with an error message if any step fails.

---

## Windows Batch Script

### **Usage**

1. Ensure that the Batch script `build-and-deploy.bat` is present in the root directory of your project.
2. Ensure that `mvnw.cmd` is present in the root directory of your project.
3. Run the script by double-clicking `build-and-deploy.bat` or executing it via the command line:

   ```cmd
   .\build-and-deploy.bat
   ```

### Additionally:

- The `docker-compose.yml` file should be in the same directory where you run the script.
- Your `pom.xml` should have `jib-maven-plugin` properly configured to build Docker images.

---

## Linux/MacOS Batch Script

## Script Usage

Follow these steps to use the shell script:

1. Ensure that the Batch script `build-and-deploy.sh` is present in the root directory of your project.
2. Ensure that `mvnw` is present in the root directory of your project and has execution permissions.
   ```sh
   chmod +x mvnw
   ```
3. Make the Script Executable. Run the following command to make the script executable:

   ```sh
   chmod +x build-and-deploy.sh
   ```

4. Run the Script. Execute the shell script to build and deploy your application:

   ```sh
   ./build-and-deploy.sh
   ```

## Expected Output

When running ./build-and-deploy.sh, you should see:

1. Progress for the Maven Jib build step (e.g., downloading dependencies, compiling, etc.).
2. Docker Compose logs for starting your services.

## Cleanup: Stopping the Services

If you want to stop the containers, you can use:

   ```sh
    docker compose stop
   ```

If you want to stop the containers and clean up the Docker Compose setup, you can use:

   ```sh
    docker compose down
   ```

This will stop the services and remove associated containers and networks.

## Restart in background

```sh
docker compose up -d
```

## Notes

The `-DskipTests` option during the Maven build skips running tests. Remove this flag if you want to run tests during
the build.
Modify the `docker-compose.yml` to set the appropriate environment variables or volumes before running the script.
This script is designed for local development.
For production, you may want to use container orchestration tools like Kubernetes or CI/CD pipelines.


----

# Useful links

1. **Access Adminer to view/manage the database:**
    - Open your web browser to [http://localhost:8888](http://localhost:8888) for home page access

2. **Orders Database Direct Access**
    - The app will connect to the database using the `ordersapp` user and `ordersdb` schema.
    - Use Adminer
      at [http://localhost:8888/?server=db&username=ordersapp&db=ordersdb](http://localhost:8888/?server=db&username=ordersapp&db=ordersdb)
      to visualize and manage your data.

4. **Adminer login credentials:**
    - **System:** MySQL
    - **Server:** db
    - **Username:** ordersapp
    - **Password:** orderspass
    - **Database:** ordersdb

----

# Open Api

### OpenAPI - Swagger Home Page

* [swagger](http://localhost:8080/order-service/swagger-ui/index.html)

### Api-Docs to create a Postman Collection

* [api-docs](http://localhost:8080/order-service/v3/api-docs)