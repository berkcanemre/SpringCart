package org.yearup.controllers;

import javax.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.yearup.models.Profile;
import org.yearup.data.ProfileDao;
import org.yearup.data.UserDao;
import org.yearup.models.authentication.LoginDto;
import org.yearup.models.authentication.LoginResponseDto;
import org.yearup.models.authentication.RegisterUserDto;
import org.yearup.models.User;
import org.yearup.security.jwt.JWTFilter;
import org.yearup.security.jwt.TokenProvider;

@RestController
@CrossOrigin
@PreAuthorize("permitAll()") // Allows all requests to this controller by default. More specific @PreAuthorize will override.
public class AuthenticationController {

    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private UserDao userDao;
    private ProfileDao profileDao;

    public AuthenticationController(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder,
                                    UserDao userDao, ProfileDao profileDao) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userDao = userDao;
        this.profileDao = profileDao;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        // Creates an authentication token from the provided username and password.
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword());

        // Authenticates the user. If authentication fails, Spring Security will throw an exception.
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        // Sets the authenticated user in the SecurityContextHolder for the current request.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generates a JWT token for the authenticated user.
        String jwt = tokenProvider.createToken(authentication, false);

        try {
            // Retrieves the user details from the database.
            User user = userDao.getByUserName(loginDto.getUsername());

            if (user == null) {
                // If user not found (though authentication should prevent this for valid users), throw 404.
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
            }

            // Sets the JWT token in the HTTP response headers.
            HttpHeaders headers = new HttpHeaders();
            headers.add(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);

            // Returns the JWT token and user details in the response body.
            return new ResponseEntity<>(new LoginResponseDto(jwt, user), headers, HttpStatus.OK);
        } catch (Exception ex) {
            // Catches any exceptions during user retrieval or token generation and returns a 500 error.
            ex.printStackTrace(); // Print stack trace for debugging purposes (consider using proper logging in production).
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed.");
        }
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // Sets the HTTP status to 201 Created on successful registration.
    public ResponseEntity<User> register(@Valid @RequestBody RegisterUserDto newUser) {
        try {
            // Optional: enforce password match (good practice for client-side validation fallback).
            if (!newUser.getPassword().equals(newUser.getConfirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match.");
            }

            // Check if user already exists to prevent duplicate registrations.
            boolean exists = userDao.exists(newUser.getUsername());
            if (exists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists.");
            }

            // Create user in the database.
            User user = userDao.create(new User(0, newUser.getUsername(), newUser.getPassword(), newUser.getRole()));

            // Create a default profile for the new user.
            Profile profile = new Profile();
            profile.setUserId(user.getId()); // Link profile to the newly created user's ID.
            profileDao.create(profile);

            // Return the created user object (without sensitive password data).
            return new ResponseEntity<>(user, HttpStatus.CREATED);
        } catch (ResponseStatusException rse) {
            // Re-throw existing ResponseStatusExceptions (e.g., for BAD_REQUEST).
            throw rse;
        } catch (Exception e) {
            // Catch any other unexpected exceptions and return a 500 Internal Server Error.
            e.printStackTrace(); // Print stack trace for debugging.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed.");
        }
    }
}