package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.OrderDao;
import org.yearup.data.ProductDao;
import org.yearup.data.ProfileDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.Order;
import org.yearup.models.OrderLineItem;
import org.yearup.models.Profile;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;
import org.yearup.models.User;
import org.yearup.models.Product;

import javax.sql.DataSource; // To manage database connection for transactions
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException; // ADDED THIS IMPORT
import java.time.LocalDateTime;
import java.math.BigDecimal; // For BigDecimal operations

// Marks this class as a REST controller.
@RestController
// Allows cross-origin requests.
@CrossOrigin
// Sets the base path for all endpoints.
@RequestMapping("orders")
// Ensures only authenticated users can access this controller.
@PreAuthorize("isAuthenticated()")
public class OrdersController
{
    private OrderDao orderDao;
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;
    private ProfileDao profileDao;
    private ProductDao productDao; // Needed to update product stock
    private DataSource dataSource; // For manual transaction management

    // Constructor for dependency injection.
    @Autowired
    public OrdersController(OrderDao orderDao, ShoppingCartDao shoppingCartDao,
                            UserDao userDao, ProfileDao profileDao,
                            ProductDao productDao, DataSource dataSource)
    {
        this.orderDao = orderDao;
        this.shoppingCartDao = shoppingCartDao;
        this.userDao = userDao;
        this.profileDao = profileDao;
        this.productDao = productDao;
        this.dataSource = dataSource; // Inject DataSource for manual transactions.
    }

    // Helper method to get the current user's ID from the Principal.
    private int getUserIdFromPrincipal(Principal principal)
    {
        String userName = principal.getName();
        User user = userDao.getByUserName(userName);
        if (user == null)
        {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Logged-in user not found in database.");
        }
        return user.getId();
    }

    // POST: http://localhost:8080/orders
    // Converts the current user's shopping cart into an order.
    @PostMapping
    @ResponseStatus(HttpStatus.OK) // Returns 201 Created on successful order creation.
    public Order checkout(Principal principal)
    {
        Connection connection = null; // Declare connection for transaction management
        try
        {
            int userId = getUserIdFromPrincipal(principal);

            // 1. Retrieve User Profile (for shipping address details)
            Profile userProfile = profileDao.getByUserId(userId);
            if (userProfile == null)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User profile not found. Please complete your profile before checking out.");
            }
            // Basic validation for essential profile details for an order
            if (userProfile.getAddress() == null || userProfile.getAddress().isEmpty() ||
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your shopping cart is empty. Please add items before checking out.");
            }

            // Start a transaction for atomicity.
            connection = dataSource.getConnection();
            connection.setAutoCommit(false); // Disable auto-commit

            // 3. Create a new Order record
            Order newOrder = new Order();
            newOrder.setUserId(userId);
            newOrder.setDate(LocalDateTime.now());
            newOrder.setAddress(userProfile.getAddress());
            newOrder.setCity(userProfile.getCity());
            newOrder.setState(userProfile.getState());
            newOrder.setZip(userProfile.getZip());
            newOrder.setShippingAmount(BigDecimal.ZERO); // Assuming free shipping for simplicity, or add logic to calculate.

            // Use the connection for this DAO operation to be part of the transaction
            // (Note: Currently DAOs get new connections. For true transaction scope across multiple DAOs,
            // Spring's @Transactional or passing the connection is needed.
            // For now, we'll assume a new connection is obtained but manage overall transaction via the controller).
            Order createdOrder = orderDao.createOrder(newOrder);


            // 4. Create OrderLineItems for each item in the shopping cart and update product stock
            for (ShoppingCartItem cartItem : cart.getItems().values())
            {
                Product product = cartItem.getProduct();
                int quantity = cartItem.getQuantity();

                // Check if product stock is sufficient
                if (product.getStock() < quantity) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.getName() + ". Available: " + product.getStock() + ", Requested: " + quantity);
                }

                OrderLineItem lineItem = new OrderLineItem();
                lineItem.setOrderId(createdOrder.getOrderId());
                lineItem.setProduct(product); // Set the full product object
                lineItem.setSalesPrice(product.getPrice()); // Use product's current price as sales price
                lineItem.setQuantity(quantity);
                lineItem.setDiscount(BigDecimal.ZERO); // Assuming no discounts for simplicity, or apply logic.

                // Create the order line item
                orderDao.createOrderLineItem(lineItem);
                createdOrder.addLineItem(lineItem); // Add to the order object being returned

                // Update product stock (decrement)
                productDao.updateStock(product.getProductId(), -quantity);
            }

            // 5. Clear the shopping cart
            shoppingCartDao.clearCart(userId);

            connection.commit(); // Commit the transaction if all operations succeed.

            return createdOrder; // Return the newly created order object.
        }
        catch (ResponseStatusException rse)
        {
            // If a known business logic error occurs, rollback and re-throw.
            if (connection != null)
            {
                try { connection.rollback(); } catch (SQLException ex) { System.err.println("Error rolling back: " + ex.getMessage()); }
            }
            throw rse;
        }
        catch (Exception ex)
        {
            // For any unexpected errors, rollback and return a generic 500.
            if (connection != null)
            {
                try { connection.rollback(); } catch (SQLException rollbackEx) { System.err.println("Error rolling back: " + rollbackEx.getMessage()); }
            }
            System.err.println("Error during checkout process: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during checkout. Please try again.");
        }
        finally
        {
            // Ensure connection is closed.
            if (connection != null)
            {
                try { connection.setAutoCommit(true); connection.close(); } // Reset auto-commit and close.
                catch (SQLException e) { System.err.println("Error closing connection: " + e.getMessage()); }
            }
        }
    }
}