package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.CategoryDao;
import org.yearup.models.Category;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


@Component
public class MySqlCategoryDao extends MySqlDaoBase implements CategoryDao
{
    // Constructor to inject the DataSource.
    // The DataSource is used to get database connections.
    @Autowired
    public MySqlCategoryDao(DataSource dataSource)
    {
        super(dataSource);
    }

    // Retrieves all categories from the database.
    @Override
    public List<Category> getAllCategories()
    {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT category_id, name, description FROM categories";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet row = ps.executeQuery())
        {
            while (row.next())
            {
                // Map each row of the ResultSet to a Category object.
                Category category = mapRow(row);
                categories.add(category);
            }
        }
        catch (SQLException e)
        {
            // Log the error and re-throw as a RuntimeException for centralized error handling.
            System.err.println("Error retrieving all categories: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all categories.", e);
        }
        return categories;
    }

    // Retrieves a single category by its ID.
    @Override
    public Category getById(int categoryId)
    {
        String sql = "SELECT category_id, name, description FROM categories WHERE category_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, categoryId); // Set the category ID parameter.

            try (ResultSet row = ps.executeQuery())
            {
                if (row.next())
                {
                    return mapRow(row); // Map the single row to a Category object.
                }
            }
        }
        catch (SQLException e)
        {
            // Log the error and re-throw.
            System.err.println("Error retrieving category by ID " + categoryId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve category by ID: " + categoryId, e);
        }
        return null; // Return null if no category found with the given ID.
    }

    // Creates a new category in the database.
    @Override
    public Category create(Category category)
    {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        int newId = 0;

        try (Connection connection = getConnection();
             // Prepare statement to return generated keys (for category_id).
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());

            ps.executeUpdate(); // Execute the insert.

            try (ResultSet generatedKeys = ps.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    newId = generatedKeys.getInt(1); // Get the auto-generated ID.
                    // Set the generated ID back to the category object.
                    category.setCategoryId(newId);
                } else
                {
                    throw new SQLException("Creating category failed, no ID obtained.");
                }
            }
            return category; // Return the category object with its new ID.
        }
        catch (SQLException e)
        {
            // Log the error and re-throw.
            System.err.println("Error creating category '" + category.getName() + "': " + e.getMessage());
            throw new RuntimeException("Failed to create category.", e);
        }
    }

    // Updates an existing category in the database.
    @Override
    public void update(int categoryId, Category category)
    {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE category_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setString(1, category.getName());
            ps.setString(2, category.getDescription());
            ps.setInt(3, categoryId); // Use the ID from the path, not necessarily from the object.

            ps.executeUpdate(); // Execute the update.
        }
        catch (SQLException e)
        {
            // Log the error and re-throw.
            System.err.println("Error updating category ID " + categoryId + ": " + e.getMessage());
            throw new RuntimeException("Failed to update category with ID: " + categoryId, e);
        }
    }

    // Deletes a category from the database by its ID.
    @Override
    public void delete(int categoryId)
    {
        String sql = "DELETE FROM categories WHERE category_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, categoryId); // Set the category ID parameter.
            ps.executeUpdate(); // Execute the delete.
        }
        catch (SQLException e)
        {
            // Log the error and re-throw.
            System.err.println("Error deleting category ID " + categoryId + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete category with ID: " + categoryId, e);
        }
    }

    // Helper method to map a ResultSet row to a Category object.
    private Category mapRow(ResultSet row) throws SQLException
    {
        int categoryId = row.getInt("category_id");
        String name = row.getString("name");
        String description = row.getString("description");

        return new Category(categoryId, name, description);
    }
}