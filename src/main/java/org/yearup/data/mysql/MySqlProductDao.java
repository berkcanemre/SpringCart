package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.ProductDao;
import org.yearup.models.Product;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlProductDao extends MySqlDaoBase implements ProductDao
{
    @Autowired
    public MySqlProductDao(DataSource dataSource)
    {
        super(dataSource);
    }

    // Helper method to map a ResultSet row to a Product object.
    private Product mapRow(ResultSet row) throws SQLException
    {
        int productId = row.getInt("product_id");
        String name = row.getString("name");
        BigDecimal price = row.getBigDecimal("price");
        int categoryId = row.getInt("category_id");
        String description = row.getString("description");
        String color = row.getString("color");
        int stock = row.getInt("stock");
        boolean featured = row.getBoolean("featured");
        String imageUrl = row.getString("image_url");

        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = "https://placehold.co/300x300/e0e0e0/333333?text=No+Image";
        }

        return new Product(productId, name, price, categoryId, description, color, stock, featured, imageUrl);
    }

    @Override
    public List<Product> getAllProducts()
    {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, name, price, category_id, description, color, stock, featured, image_url FROM products";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet row = ps.executeQuery())
        {
            while (row.next())
            {
                products.add(mapRow(row));
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving all products: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all products.", e);
        }
        return products;
    }

    @Override
    public Product getById(int productId)
    {
        String sql = "SELECT product_id, name, price, category_id, description, color, stock, featured, image_url FROM products WHERE product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, productId);

            try (ResultSet row = ps.executeQuery())
            {
                if (row.next())
                {
                    return mapRow(row);
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving product by ID " + productId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve product by ID: " + productId, e);
        }
        return null;
    }

    @Override
    public Product create(Product product)
    {
        String sql = "INSERT INTO products (name, price, category_id, description, color, stock, featured, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int newId = 0;

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            ps.setString(1, product.getName());
            ps.setBigDecimal(2, product.getPrice());
            ps.setInt(3, product.getCategoryId());
            ps.setString(4, product.getDescription());
            ps.setString(5, product.getColor());
            ps.setInt(6, product.getStock());
            ps.setBoolean(7, product.isFeatured());
            ps.setString(8, product.getImageUrl());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    newId = generatedKeys.getInt(1);
                    product.setProductId(newId);
                } else
                {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
            return product;
        }
        catch (SQLException e)
        {
            System.err.println("Error creating product '" + product.getName() + "': " + e.getMessage());
            throw new RuntimeException("Failed to create product.", e);
        }
    }

    @Override
    public void update(int productId, Product product)
    {
        String sql = "UPDATE products SET name = ?, price = ?, category_id = ?, description = ?, color = ?, stock = ?, featured = ?, image_url = ? WHERE product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setString(1, product.getName());
            ps.setBigDecimal(2, product.getPrice());
            ps.setInt(3, product.getCategoryId());
            ps.setString(4, product.getDescription());
            ps.setString(5, product.getColor());
            ps.setInt(6, product.getStock());
            ps.setBoolean(7, product.isFeatured());
            ps.setString(8, product.getImageUrl());
            ps.setInt(9, productId);

            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error updating product ID " + productId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update product with ID: " + productId, e);
        }
    }

    @Override
    public void delete(int productId)
    {
        String sql = "DELETE FROM products WHERE product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, productId);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error deleting product ID " + productId + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete product with ID: " + productId, e);
        }
    }

    @Override
    public List<Product> getProductsByCategoryId(int categoryId)
    {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT product_id, name, price, category_id, description, color, stock, featured, image_url FROM products WHERE category_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, categoryId);

            try (ResultSet row = ps.executeQuery())
            {
                while (row.next())
                {
                    products.add(mapRow(row));
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error retrieving products for category ID " + categoryId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve products for category ID: " + categoryId, e);
        }
        return products;
    }

    @Override
    public List<Product> search(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String color)
    {
        List<Product> products = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT product_id, name, price, category_id, description, color, stock, featured, image_url FROM products WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (categoryId != null)
        {
            sqlBuilder.append(" AND category_id = ?");
            params.add(categoryId);
        }
        if (minPrice != null)
        {
            sqlBuilder.append(" AND price >= ?");
            params.add(minPrice);
        }
        if (maxPrice != null)
        {
            sqlBuilder.append(" AND price <= ?");
            params.add(maxPrice);
        }
        if (color != null && !color.trim().isEmpty())
        {
            sqlBuilder.append(" AND color LIKE ?");
            params.add("%" + color.trim() + "%");
        }

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sqlBuilder.toString()))
        {
            for (int i = 0; i < params.size(); i++)
            {
                Object param = params.get(i);
                if (param instanceof Integer)
                {
                    ps.setInt(i + 1, (Integer) param);
                } else if (param instanceof BigDecimal)
                {
                    ps.setBigDecimal(i + 1, (BigDecimal) param);
                } else if (param instanceof String)
                {
                    ps.setString(i + 1, (String) param);
                }
            }

            try (ResultSet row = ps.executeQuery())
            {
                while (row.next())
                {
                    products.add(mapRow(row));
                }
            }
        }
        catch (SQLException e)
        {
            System.err.println("Error searching products: " + e.getMessage());
            throw new RuntimeException("Failed to search products.", e);
        }
        return products;
    }

    // NEW: Updates the stock of a product.
    // quantityChange can be positive (increase) or negative (decrease).
    @Override
    public void updateStock(int productId, int quantityChange)
    {
        String sql = "UPDATE products SET stock = stock + ? WHERE product_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, quantityChange); // The change amount (can be negative)
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error updating stock for product ID " + productId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update product stock.", e);
        }
    }
}