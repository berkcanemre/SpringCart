package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.OrderDao;
import org.yearup.data.ProductDao; // Required to fetch Product details for OrderLineItem
import org.yearup.models.Order;
import org.yearup.models.OrderLineItem;
import org.yearup.models.Product;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp; // For LocalDateTime conversion
import java.time.LocalDateTime;
import java.math.BigDecimal; // ADDED THIS IMPORT

// Marks this class as a Spring component.
@Component
public class MySqlOrderDao extends MySqlDaoBase implements OrderDao
{
    private ProductDao productDao; // Needed to retrieve product details for order line items.

    @Autowired
    public MySqlOrderDao(DataSource dataSource, ProductDao productDao)
    {
        super(dataSource);
        this.productDao = productDao;
    }

    // Helper method to map a ResultSet row to an Order object.
    private Order mapRowToOrder(ResultSet row) throws SQLException
    {
        int orderId = row.getInt("order_id");
        int userId = row.getInt("user_id");
        // Convert SQL Timestamp to LocalDateTime.
        LocalDateTime date = row.getTimestamp("date").toLocalDateTime();
        String address = row.getString("address");
        String city = row.getString("city");
        String state = row.getString("state");
        String zip = row.getString("zip");
        BigDecimal shippingAmount = row.getBigDecimal("shipping_amount");

        return new Order(orderId, userId, date, address, city, state, zip, shippingAmount);
    }

    // Helper method to map a ResultSet row to an OrderLineItem object.
    // This method will also fetch the full Product object.
    private OrderLineItem mapRowToOrderLineItem(ResultSet row) throws SQLException
    {
        int orderLineItemId = row.getInt("order_line_item_id");
        int orderId = row.getInt("order_id");
        int productId = row.getInt("product_id");
        BigDecimal salesPrice = row.getBigDecimal("sales_price");
        int quantity = row.getInt("quantity");
        BigDecimal discount = row.getBigDecimal("discount");

        // Fetch the full Product object using ProductDao.
        Product product = productDao.getById(productId);
        if (product == null) {
            // Handle case where product might not exist (e.g., product deleted but still in order_line_items).
            // Log a warning or throw an exception, depending on desired behavior.
            System.err.println("Warning: Product with ID " + productId + " not found for order line item " + orderLineItemId);
            // You might return null or a partially populated OrderLineItem here.
            // For now, let's throw an exception as a missing product is critical for order integrity.
            throw new SQLException("Product (ID: " + productId + ") not found for order line item.");
        }

        return new OrderLineItem(orderLineItemId, orderId, product, salesPrice, quantity, discount);
    }


    @Override
    public Order createOrder(Order order)
    {
        String sql = "INSERT INTO orders (user_id, date, address, city, state, zip, shipping_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        int newOrderId = 0;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            ps.setInt(1, order.getUserId());
            // Convert LocalDateTime to Timestamp for database storage.
            ps.setTimestamp(2, Timestamp.valueOf(order.getDate()));
            ps.setString(3, order.getAddress());
            ps.setString(4, order.getCity());
            ps.setString(5, order.getState());
            ps.setString(6, order.getZip());
            ps.setBigDecimal(7, order.getShippingAmount());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    newOrderId = generatedKeys.getInt(1);
                    order.setOrderId(newOrderId); // Set the generated ID back to the Order object.
                } else
                {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
            return order;
        }
        catch (SQLException e)
        {
            System.err.println("Error creating order: " + e.getMessage());
            throw new RuntimeException("Failed to create order.", e);
        }
    }

    @Override
    public OrderLineItem createOrderLineItem(OrderLineItem orderLineItem)
    {
        String sql = "INSERT INTO order_line_items (order_id, product_id, sales_price, quantity, discount) " +
                "VALUES (?, ?, ?, ?, ?)";
        int newLineItemId = 0;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            ps.setInt(1, orderLineItem.getOrderId());
            // Ensure product is not null before accessing its ID.
            if (orderLineItem.getProduct() == null) {
                throw new SQLException("OrderLineItem product cannot be null.");
            }
            ps.setInt(2, orderLineItem.getProduct().getProductId());
            ps.setBigDecimal(3, orderLineItem.getSalesPrice());
            ps.setInt(4, orderLineItem.getQuantity());
            ps.setBigDecimal(5, orderLineItem.getDiscount());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    newLineItemId = generatedKeys.getInt(1);
                    // Set the generated ID back to the OrderLineItem object.
                    orderLineItem.setOrderLineItemId(newLineItemId);
                } else
                {
                    throw new SQLException("Creating order line item failed, no ID obtained.");
                }
            }
            return orderLineItem;
        }
        catch (SQLException e)
        {
            System.err.println("Error creating order line item for order " + orderLineItem.getOrderId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to create order line item.", e);
        }
    }
}