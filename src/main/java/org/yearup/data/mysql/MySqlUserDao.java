package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.UserDao;
import org.yearup.models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlUserDao extends MySqlDaoBase implements UserDao
{
    @Autowired
    public MySqlUserDao(DataSource dataSource)
    {
        super(dataSource);
    }

    @Override
    public User create(User newUser)
    {
        String sql = "INSERT INTO users (username, hashed_password, role) VALUES (?, ?, ?)";
        // Encode the password before storing it using BCrypt.
        String hashedPassword = new BCryptPasswordEncoder().encode(newUser.getPassword());
        int newUserId = 0; // Initialize to store the generated user ID

        try (Connection connection = getConnection())
        {
            // Prepare the statement, indicating that generated keys should be returned.
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, newUser.getUsername());
            ps.setString(2, hashedPassword);
            ps.setString(3, newUser.getRole());

            ps.executeUpdate(); // Execute the insert statement.

            // Retrieve the auto-generated user ID.
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    newUserId = generatedKeys.getInt(1); // Get the first (and likely only) generated key.
                } else {
                    // If no ID was generated, it indicates a failure in insertion.
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            // Create a new User object with the generated ID and an empty password (for security, never return password).
            User createdUser = new User(newUserId, newUser.getUsername(), "", newUser.getRole());
            return createdUser; // Return the user object with the correct ID.

        }
        catch (SQLException e)
        {
            // Log the actual SQL error for debugging.
            System.err.println("Error creating user: " + e.getMessage());
            // Re-throw as a RuntimeException to be caught by the controller,
            // providing better error visibility.
            throw new RuntimeException("Error creating user in database.", e);
        }
    }

    @Override
    public List<User> getAll()
    {
        List<User> users = new ArrayList<>();

        String sql = "SELECT * FROM users";
        try (Connection connection = getConnection())
        {
            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet row = statement.executeQuery();

            while (row.next())
            {
                User user = mapRow(row);
                users.add(user);
            }
        }
        catch (SQLException e)
        {
            System.err.println("Database error retrieving all users: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve all users.", e);
        }

        return users;
    }

    @Override
    public User getUserById(int id)
    {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection connection = getConnection())
        {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);

            ResultSet row = statement.executeQuery();

            if(row.next())
            {
                User user = mapRow(row);
                return user;
            }
        }
        catch (SQLException e)
        {
            System.err.println("Database error retrieving user by ID: " + id + " - " + e.getMessage());
            throw new RuntimeException("Failed to retrieve user by ID: " + id, e);
        }
        return null; // Return null if no user found with the given ID.
    }

    @Override
    public User getByUserName(String username)
    {
        String sql = "SELECT * " +
                " FROM users " +
                " WHERE username = ?";

        try (Connection connection = getConnection())
        {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, username);

            ResultSet row = statement.executeQuery();
            if(row.next())
            {
                User user = mapRow(row);
                return user;
            }
        }
        catch (SQLException e)
        {
            // IMPORTANT: Do not silently catch and print. Re-throw as a RuntimeException.
            System.err.println("Database error retrieving user by username: " + username + " - " + e.getMessage());
            throw new RuntimeException("Failed to retrieve user by username: " + username, e);
        }

        return null; // Returns null if no user is found with the given username.
    }

    @Override
    public int getIdByUsername(String username)
    {
        User user = getByUserName(username);

        if(user != null)
        {
            return user.getId();
        }

        return -1; // Return -1 if user is not found.
    }

    @Override
    public boolean exists(String username)
    {
        // This will now correctly throw a RuntimeException if there's a database error,
        // rather than silently failing and potentially causing a NullPointerException later.
        User user = getByUserName(username);
        return user != null;
    }

    // Helper method to map a ResultSet row to a User object.
    private User mapRow(ResultSet row) throws SQLException
    {
        int userId = row.getInt("user_id");
        String username = row.getString("username");
        String hashedPassword = row.getString("hashed_password");
        String role = row.getString("role");

        return new User(userId, username, hashedPassword, role);
    }
}