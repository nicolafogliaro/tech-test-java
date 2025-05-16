## How to Start & Use

1. **Launch the stack with Docker Compose:**
    ```sh
    docker compose up -d
    ```
   
    ```sh
    docker compose down
    ```

3. **Access Adminer to view/manage the database:**
    - Open your web browser and navigate to [http://localhost:8888](http://localhost:8888)

4. **Adminer login credentials:**
    - **System:** MySQL
    - **Server:** db
    - **Username:** ordersapp
    - **Password:** orderspass
    - **Database:** ordersdb

5. **Ready to develop!**
    - Your Spring Boot app will connect to the database using the `ordersapp` user and `ordersdb` schema.
    - Use Adminer at [http://localhost:8888/?server=db&username=ordersapp&db=ordersdb](http://localhost:8888/?server=db&username=ordersapp&db=ordersdb) to visualize and manage your data.


----

# Open Api

[swagger](http://localhost:8080/order-service/swagger-ui/index.html)

[pi-docs](http://localhost:8080/order-service/v3/api-docs)