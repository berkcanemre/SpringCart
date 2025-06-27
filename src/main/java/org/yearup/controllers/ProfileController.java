package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.Profile;
import org.yearup.models.User;

import java.security.Principal;

// Marks this class as a REST controller.
@RestController
// Allows cross-origin requests.
@CrossOrigin
// Sets the base path for all endpoints.
@RequestMapping("profile")
// Ensures only authenticated users can access this controller.
@PreAuthorize("isAuthenticated()")
public class ProfileController
{
    private ProfileDao profileDao;
    private UserDao userDao;

    // Constructor for dependency injection.
    @Autowired
    public ProfileController(ProfileDao profileDao, UserDao userDao)
    {
        this.profileDao = profileDao;
        this.userDao = userDao;
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

    // GET: http://localhost:8080/profile
    @GetMapping
    public Profile getProfile(Principal principal)
    {
        try
        {
            int userId = getUserIdFromPrincipal(principal);
            Profile profile = profileDao.getByUserId(userId);
            if (profile == null)
            {
                // This scenario should ideally not happen if a profile is created on user registration.
                // However, if it could happen, this handles it.
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found.");
            }
            return profile;
        }
        catch (ResponseStatusException rse)
        {
            throw rse; // Re-throw specific RSEs (like 404 from getUserIdFromPrincipal)
        }
        catch (Exception ex)
        {
            System.err.println("Error retrieving user profile: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve user profile.", ex);
        }
    }

    // PUT: http://localhost:8080/profile
    @PutMapping
    // CHANGE 1: Changed from HttpStatus.NO_CONTENT (204) to HttpStatus.OK (200) to match Postman test.
    @ResponseStatus(HttpStatus.OK) // <--- Changed this line!
    public void updateProfile(Principal principal, @RequestBody Profile profile)
    {
        try
        {
            int authorizedUserId = getUserIdFromPrincipal(principal);

            // Security check: Ensure the authenticated user can only update their own profile.
            // The userId in the request body MUST match the userId from the authenticated principal.
            if (profile.getUserId() != authorizedUserId)
            {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this profile.");
            }

            profileDao.update(profile);
        }
        catch (ResponseStatusException rse)
        {
            throw rse; // Re-throw specific RSEs (like 404 from getUserIdFromPrincipal or 403 from authorization)
        }
        catch (Exception ex)
        {
            System.err.println("Error updating user profile: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update user profile.", ex);
        }
    }
}