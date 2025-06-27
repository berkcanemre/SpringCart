package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.ProductDao; // Need ProductDao to get product details for ShoppingCartItem
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

// Marks this class as a Spring component for dependency injection.
@Component
public class MySqlShoppingCartDao extends MySqlDaoBase implements ShoppingCartDao
{
    private ProductDao productDao; // Inject ProductDao to fetch product details.

    @Autowired
    public MySqlShoppingCartDao(DataSource dataSource, ProductDao productDao)
    {
        super(dataSource);
        this.productDao = productDao;
    }

    // Helper method to map a ResultSet row from the shopping_cart table to a ShoppingCartItem.
    // It also fetches the full Product details using ProductDao.
    private ShoppingCartItem mapRowToCartItem(ResultSet row) throws SQLException
    {
        int productId = row.getInt("product_id");
        int quantity = row.getInt("quantity");

        // Fetch the full Product object using the ProductDao.
        Product product = productDao.getById(productId);
        if (product == null) {
            // Handle case where product might not exist (e.g., product deleted but still in cart table)
            // Or log an error and potentially skip this item.
            System.err.println("Warning: Product with ID " + productId + " not found for shopping cart.");
            return null; // Or create a placeholder product.
        }

        // Create a new ShoppingCartItem. The lineTotal will be calculated automatically by its constructor.
        ShoppingCartItem item = new ShoppingCartItem(product, quantity);
        // Note: discountPercent is not stored in shopping_cart table, defaults to 0 in ShoppingCartItem.
        return item;
    }

    // Retrieves the shopping cart for a specific user.
    @Override
    public ShoppingCart getByUserId(int userId)
    {
        ShoppingCart cart = new ShoppingCart();
        String sql = "SELECT product_id, quantity FROM shopping_cart WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    ShoppingCartItem item = mapRowToCartItem(rs);
                    if (item != null)
                    {
                        cart.add(item); // Add the item to the cart, which also updates the cart's total.
                    }
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving shopping cart for user ID " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve shopping cart.", e);
        }
        return cart;
    }

    // Adds a product to the user's shopping cart or increments its quantity if already present.
    @Override
    public void addProductToCart(int userId, int productId)
    {
        // Check if the product already exists in the cart for this user
        String checkSql = "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ? AND product_id = ?";
        String updateSql = "UPDATE shopping_cart SET quantity = quantity + 1 WHERE user_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, 1)";

        Connection connection = null; // Declare connection outside the try block

        try
        {
            connection = getConnection(); // Obtain connection
            // Use transaction to ensure atomicity if both check and update/insert are done.
            // For simple increment, two separate statements are often okay.
            connection.setAutoCommit(false); // Start transaction

            try (PreparedStatement checkPs = connection.prepareStatement(checkSql))
            {
                checkPs.setInt(1, userId);
                checkPs.setInt(2, productId);
                try (ResultSet rs = checkPs.executeQuery())
                {
                    if (rs.next() && rs.getInt(1) > 0)
                    {
                        // Product exists, so update quantity
                        try (PreparedStatement updatePs = connection.prepareStatement(updateSql))
                        {
                            updatePs.setInt(1, userId);
                            updatePs.setInt(2, productId);
                            updatePs.executeUpdate();
                        }
                    } else
                    {
                        // Product does not exist, so insert with quantity 1
                        try (PreparedStatement insertPs = connection.prepareStatement(insertSql))
                        {
                            insertPs.setInt(1, userId);
                            insertPs.setInt(2, productId);
                            insertPs.executeUpdate();
                        }
                    }
                }
            }
            connection.commit(); // Commit transaction
        }
        catch (SQLException e)
        {
            try {
                if (connection != null) { // Check if connection was successfully obtained
                    connection.rollback(); // Rollback on error
                }
            } catch (SQLException ex) {
                System.err.println("Error rolling back transaction: " + ex.getMessage());
            }
            System.err.println("Error adding product " + productId + " to cart for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to add product to cart.", e);
        }
        finally {
            // Ensure the connection is closed in a finally block
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    // Updates the quantity of a specific product in the user's shopping cart.
    @Override
    public void updateProductQuantity(int userId, int productId, int quantity)
    {
        if (quantity <= 0)
        {
            // If quantity is 0 or less, delete the item from the cart.
            removeProductFromCart(userId, productId); // Call the specific removal method.
            return;
        }

        String sql = "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, quantity);
            ps.setInt(2, userId);
            ps.setInt(3, productId);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error updating product " + productId + " quantity for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update product quantity in cart.", e);
        }
    }

    // Clears all items from a user's shopping cart.
    @Override
    public void clearCart(int userId)
    {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error clearing cart for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to clear shopping cart.", e);
        }
    }

    // NEW: Removes a specific product from a user's shopping cart.
    @Override
    public void removeProductFromCart(int userId, int productId)
    {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error removing product " + productId + " from cart for user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to remove product from cart.", e);
        }
    }

    @Override
    public boolean productExistsInCart(int userId, int productId)
    {
        String sql = "SELECT COUNT(*) FROM shopping_cart WHERE user_id = ? AND product_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, userId);
            ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return rs.getInt(1) > 0;
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error checking if product exists in cart for user " + userId + ", product " + productId + ": " + e.getMessage());
            throw new RuntimeException("Failed to check product existence in cart.", e);
        }
        return false;
    }

    @Override
    public ShoppingCartItem getCartItemByUserIdAndProductId(int userId, int productId)
    {
        String sql = "SELECT product_id, quantity FROM shopping_cart WHERE user_id = ? AND product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, userId);
            ps.setInt(2, productId);

            try (ResultSet rs = ps.executeQuery())
            {
                if (rs.next())
                {
                    return mapRowToCartItem(rs);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving cart item for user " + userId + ", product " + productId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve cart item.", e);
        }
        return null;
    }
}