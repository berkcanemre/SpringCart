EasyShop E-Commerce API
Capstone 3: E-Commerce API - Spring Boot Application

User Authentication & Authorization: Secure registration and login, with role-based access control using JWTs.

User Profile Management: Users can view and update their personal shipping and contact information.

Product Catalog: CRUD operations for managing products, including details like name, price, category, description, stock, and images.

Category Management: CRUD operations for organizing products into categories.

Shopping Cart: Add, update quantities, remove individual items, and clear the entire shopping cart for a logged-in user.

Checkout Process: Convert the user's shopping cart into a new order, including:

Validation for complete user profile details and sufficient product stock.

Creation of new order records and associated order line items.

Automatic decrementing of product stock upon purchase.

Clearing the shopping cart after a successful order.

Atomic transactions to ensure data consistency during checkout.

Data Persistence: All data is stored securely in a MySQL database.

POST /register: Register a new user.

POST /login: Authenticate a user and receive a JWT token.

GET /categories: Retrieve all product categories.

GET /categories/{id}: Retrieve a single category by ID.

GET /products: Retrieve all products.

GET /products/{id}: Retrieve a single product by ID.

GET /products/search: Search products by category, price range, or color.

GET /profile: View the logged-in user's profile.

PUT /profile: Update the logged-in user's profile.

GET /cart: View the logged-in user's shopping cart.

POST /cart/products/{productId}: Add a product to the cart (or increment quantity).

PUT /cart/products/{productId}: Update the quantity of a product in the cart.

DELETE /cart/products/{productId}: Remove a specific product from the cart.

DELETE /cart: Clear the entire shopping cart.

POST /orders: Checkout, converting the shopping cart into an order.

User Registration (Postman): register_postman.png

User Login (Postman): login_postman.png

Get All Products (Postman): get_products_postman.png

Add to Cart (Postman): add_to_cart_postman.png

View Shopping Cart (Postman): view_cart_postman.png

User Profile Retrieval (Postman): get_profile_postman.png

Update User Profile (Postman): update_profile_postman.png

Successful Checkout (Postman): checkout_postman.png

Self-note: Replace these placeholder paths with actual screenshots from your Postman interactions or from a demo frontend if available.

// src/main/java/org/yearup/controllers/OrdersController.java
// This method orchestrates the entire checkout process, ensuring atomicity
// across multiple database operations using manual transaction management.
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Order checkout(Principal principal)
{
Connection connection = null; // JDBC Connection for transaction control
try
{
int userId = getUserIdFromPrincipal(principal);

        // 1. Validate User Profile for shipping details
        Profile userProfile = profileDao.getByUserId(userId);
        if (userProfile == null || userProfile.getAddress() == null || userProfile.getAddress().isEmpty() ||
            userProfile.getCity() == null || userProfile.getCity().isEmpty() ||
            userProfile.getState() == null || userProfile.getState().isEmpty() ||
            userProfile.getZip() == null || userProfile.getZip().isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shipping address is incomplete. Please update your profile.");
        }

        // 2. Retrieve Shopping Cart
        ShoppingCart cart = shoppingCartDao.getByUserId(userId);
        if (cart == null || cart.getItems().isEmpty())
        {
            throw new new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your shopping cart is empty. Please add items before checking out.");
        }

        // --- Start Transaction ---
        connection = dataSource.getConnection();
        connection.setAutoCommit(false); // Disable auto-commit for manual transaction

        // 3. Create the main Order record
        Order newOrder = new Order();
        newOrder.setUserId(userId);
        newOrder.setDate(LocalDateTime.now());
        newOrder.setAddress(userProfile.getAddress());
        newOrder.setCity(userProfile.getCity());
        newOrder.setState(userProfile.getState());
        newOrder.setZip(userProfile.getZip());
        newOrder.setShippingAmount(BigDecimal.ZERO); // Example: assuming free shipping
        Order createdOrder = orderDao.createOrder(newOrder); // This DAO call is part of the transaction

        // 4. Process each item in the cart: create line items and update product stock
        for (ShoppingCartItem cartItem : cart.getItems().values())
        {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            // Validate sufficient stock
            if (product.getStock() < quantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.getName());
            }

            OrderLineItem lineItem = new OrderLineItem();
            lineItem.setOrderId(createdOrder.getOrderId());
            lineItem.setProduct(product);
            lineItem.setSalesPrice(product.getPrice());
            lineItem.setQuantity(quantity);
            lineItem.setDiscount(BigDecimal.ZERO);
            orderDao.createOrderLineItem(lineItem); // Part of transaction
            createdOrder.addLineItem(lineItem); // Add to object for response

            productDao.updateStock(product.getProductId(), -quantity); // Decrement stock, part of transaction
        }

        // 5. Clear the shopping cart
        shoppingCartDao.clearCart(userId); // Part of transaction

        connection.commit(); // Commit all changes if successful
        // --- End Transaction ---

        return createdOrder;
    }
    catch (ResponseStatusException rse)
    {
        // Rollback on expected business logic errors
        if (connection != null) { try { connection.rollback(); } catch (SQLException ex) { System.err.println("Error rolling back: " + ex.getMessage()); } }
        throw rse;
    }
    catch (Exception ex)
    {
        // Rollback on any unexpected errors
        if (connection != null) { try { connection.rollback(); } catch (SQLException rollbackEx) { System.err.println("Error rolling back: " + rollbackEx.getMessage()); } }
        System.err.println("Error during checkout process: " + ex.getMessage());
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during checkout. Please try again.");
    }
    finally
    {
        // Ensure connection is closed and auto-commit is reset
        if (connection != null) { try { connection.setAutoCommit(true); connection.close(); } catch (SQLException e) { System.err.println("Error closing connection: " + e.getMessage()); } }
    }
}

This code snippet illustrates the checkout method in OrdersController, which is responsible for orchestrating the entire order creation process. It demonstrates:

Endpoint Definition: How a POST request to /orders is handled.

Data Validation: Checks for complete user profiles and non-empty shopping carts.

Transactional Integrity: Uses explicit JDBC Connection management (setAutoCommit(false), commit(), rollback()) to ensure that all database operations related to an order (creating the order, creating line items, updating product stock, clearing the cart) either succeed together or fail together, maintaining data consistency.

Cross-DAO Interaction: Shows how multiple Data Access Objects (profileDao, shoppingCartDao, orderDao, productDao) are utilized within a single business operation.

Error Handling: Catches various exceptions and ensures proper rollback in case of failures, returning meaningful HTTP status codes.

Java 17+

Apache Maven (for dependency management and build)

MySQL Server (version 8.0+)

MySQL Workbench or another SQL client

Postman (for API testing)

IDE (IntelliJ IDEA recommended)

Installation & Run:

Clone the repository:

git clone https://github.com/berkcanemre/EasyShop-API.git
cd EasyShop-API

(Note: Adjust the repository name above if it's different)

Database Setup:

Open create_database.sql (located in your project root or src/main/resources).

Open MySQL Workbench, connect to your MySQL server.

Execute the create_database.sql script to create the easyshop database and populate it with initial data. This script will drop and recreate the database, so ensure you backup any existing data if necessary.

Configure Application Properties:

Open src/main/resources/application.properties.

Update the MySQL database connection details to match your local setup:

spring.datasource.url=jdbc:mysql://localhost:3306/easyshop
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

Run the Spring Boot Application:

Open the project in IntelliJ IDEA.

Navigate to src/main/java/org/yearup/EasyshopApplication.java.

Run the main method (click the green play icon next to the main method or class name).

The application should start on http://localhost:8080.

Test the API with Postman:

Import the Postman collections provided with your starter code (easyshop.postman_collection.json).

Follow the API Endpoints section above to test various functionalities, starting with user registration and login to obtain a JWT token for authenticated requests.

Java 17

Spring Boot (Web, Security, JDBC)

MySQL Database

JDBC (Java Database Connectivity)

Maven (Build Automation)

JWT (JSON Web Tokens) for authentication

Spring Security

Berkcan Emre

https://github.com/berkcanemre

This project is open source and available under the MIT License.

Special thanks to Yearup United’s staff and instructors for their guidance and inspiration throughout the Capstone project.