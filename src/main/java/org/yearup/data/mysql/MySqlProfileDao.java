package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yearup.data.ProfileDao;
import org.yearup.models.Profile;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// This class implements the ProfileDao interface for MySQL database interactions.
@Component
public class MySqlProfileDao extends MySqlDaoBase implements ProfileDao
{
    @Autowired
    public MySqlProfileDao(DataSource dataSource)
    {
        super(dataSource);
    }

    // Helper method to map a ResultSet row to a Profile object.
    private Profile mapRow(ResultSet row) throws SQLException
    {
        int profileId = row.getInt("profile_id");
        int userId = row.getInt("user_id");
        String firstName = row.getString("first_name");
        String lastName = row.getString("last_name");
        String phone = row.getString("phone");
        String email = row.getString("email");
        String address = row.getString("address");
        String city = row.getString("city");
        String state = row.getString("state");
        String zip = row.getString("zip");

        return new Profile(profileId, userId, firstName, lastName, phone, email, address, city, state, zip);
    }

    @Override
    public Profile create(Profile profile)
    {
        String sql = "INSERT INTO profiles (user_id, first_name, last_name, phone, email, address, city, state, zip) " +
                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        // Initialize generatedId to 0, which is an invalid ID, for error checking.
        int generatedId = 0;

        try (Connection connection = getConnection();
             // Prepare the statement to return auto-generated keys (profile_id).
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            ps.setInt(1, profile.getUserId());
            ps.setString(2, profile.getFirstName());
            ps.setString(3, profile.getLastName());
            ps.setString(4, profile.getPhone());
            ps.setString(5, profile.getEmail());
            ps.setString(6, profile.getAddress());
            ps.setString(7, profile.getCity());
            ps.setString(8, profile.getState());
            ps.setString(9, profile.getZip());

            ps.executeUpdate(); // Execute the insert statement.

            // Retrieve the auto-generated profile ID.
            try (ResultSet generatedKeys = ps.getGeneratedKeys())
            {
                if (generatedKeys.next())
                {
                    generatedId = generatedKeys.getInt(1); // Get the generated ID.
                    profile.setProfileId(generatedId); // Set the ID back to the profile object.
                } else
                {
                    throw new SQLException("Creating profile failed, no ID obtained.");
                }
            }
            return profile; // Return the profile object with its new ID.
        }
        catch (SQLException e)
        {
            // Log the error and re-throw as a RuntimeException.
            System.err.println("Error creating profile for user ID " + profile.getUserId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to create profile.", e);
        }
    }

    // NEW: Retrieves a user profile by their user ID.
    @Override
    public Profile getByUserId(int userId)
    {
        String sql = "SELECT profile_id, user_id, first_name, last_name, phone, email, address, city, state, zip " +
                "FROM profiles WHERE user_id = ?";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setInt(1, userId);

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
            System.err.println("Error retrieving profile for user ID " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to retrieve profile by user ID: " + userId, e);
        }
        return null; // Return null if no profile found for the given user ID.
    }

    // NEW: Updates an existing user profile in the database.
    @Override
    public void update(Profile profile)
    {
        String sql = "UPDATE profiles SET first_name = ?, last_name = ?, phone = ?, email = ?, address = ?, city = ?, state = ?, zip = ? " +
                "WHERE user_id = ?"; // Important: Update based on user_id, not profile_id, as it's typically a 1-to-1 relationship.

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setString(1, profile.getFirstName());
            ps.setString(2, profile.getLastName());
            ps.setString(3, profile.getPhone());
            ps.setString(4, profile.getEmail());
            ps.setString(5, profile.getAddress());
            ps.setString(6, profile.getCity());
            ps.setString(7, profile.getState());
            ps.setString(8, profile.getZip());
            ps.setInt(9, profile.getUserId()); // Use the userId from the profile object to identify the row.

            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            System.err.println("Error updating profile for user ID " + profile.getUserId() + ": " + e.getMessage());
            throw new RuntimeException("Failed to update profile.", e);
        }
    }
}