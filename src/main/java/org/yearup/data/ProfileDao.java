package org.yearup.data;

import org.yearup.models.Profile;

// This interface defines the contract for interacting with user profile data.
public interface ProfileDao
{
    // Creates a new user profile in the database.
    Profile create(Profile profile);

    // NEW: Retrieves a user profile by their user ID.
    Profile getByUserId(int userId);

    // NEW: Updates an existing user profile in the database.
    void update(Profile profile);
}