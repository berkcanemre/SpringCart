package org.yearup.data;

import org.yearup.models.User;

import java.util.List;

// This interface defines the contract for interacting with user data in the database.
public interface UserDao
{
    // Creates a new user in the database.
    User create(User user);

    // Retrieves all users from the database.
    List<User> getAll();

    // Retrieves a user by their ID.
    User getUserById(int id);

    // Retrieves a user by their username.
    User getByUserName(String username);

    // Retrieves the user ID by username.
    int getIdByUsername(String username);

    // Checks if a user with the given username already exists.
    boolean exists(String username);
}