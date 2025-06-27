package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.ShoppingCart;
import org.yearup.models.User;

import java.security.Principal;

// Marks this class as a REST controller.
@RestController
// Allows cross-origin requests.
@CrossOrigin
// Sets the base path for all endpoints.
@RequestMapping("cart")
// Ensures only authenticated users can access this controller.
@PreAuthorize("isAuthenticated()")
public class ShoppingCartController
{
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;

    // Constructor for dependency injection.
    @Autowired
    public ShoppingCartController(ShoppingCartDao shoppingCartDao, UserDao userDao)
    {
        this.shoppingCartDao = shoppingCartDao;
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

    // GET: http://localhost:8080/cart
    @GetMapping
    public ShoppingCart getCart(Principal principal)
    {
        try
        {
            int userId = getUserIdFromPrincipal(principal);
            return shoppingCartDao.getByUserId(userId);
        }
        catch (ResponseStatusException rse)
        {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error getting shopping cart: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get shopping cart.", ex);
        }
    }

    // POST: http://localhost:8080/cart/products/{productId}
    @PostMapping("products/{productId}")
    public ShoppingCart addProductToCart(Principal principal, @PathVariable int productId)
    {
        try
        {
            int userId = getUserIdFromPrincipal(principal);
            shoppingCartDao.addProductToCart(userId, productId);
            // After adding, return the updated cart.
            return shoppingCartDao.getByUserId(userId);
        }
        catch (ResponseStatusException rse)
        {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error adding product to cart: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add product to cart.", ex);
        }
    }

    // PUT: http://localhost:8080/cart/products/{productId}
    // This method expects a request body like: {"quantity": 5}
    @PutMapping("products/{productId}")
    public ShoppingCart updateProductInCart(Principal principal, @PathVariable int productId, @RequestBody int quantity) // Assuming quantity is directly in body
    {
        try
        {
            if (quantity <= 0) {
                // If quantity is 0 or less, consider removing the item or throwing an error.
                // For this scenario, let's assume it means remove if quantity is 0,
                // otherwise, it's an invalid update request for negative quantity.
                if (quantity == 0) {
                    shoppingCartDao.removeProductFromCart(getUserIdFromPrincipal(principal), productId);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be a positive integer.");
                }
            } else {
                // CHANGED THIS LINE: Now calls updateProductQuantity to match the DAO method name.
                shoppingCartDao.updateProductQuantity(getUserIdFromPrincipal(principal), productId, quantity);
            }
            return shoppingCartDao.getByUserId(getUserIdFromPrincipal(principal));
        }
        catch (ResponseStatusException rse)
        {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error updating product in cart: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update product in cart.", ex);
        }
    }

    // DELETE: http://localhost:8080/cart/products/{productId}
    @DeleteMapping("products/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Typically 204 for deletion without content
    public void removeProductFromCart(Principal principal, @PathVariable int productId)
    {
        try
        {
            shoppingCartDao.removeProductFromCart(getUserIdFromPrincipal(principal), productId);
        }
        catch (ResponseStatusException rse)
        {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error removing product from cart: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove product from cart.", ex);
        }
    }

    // DELETE: http://localhost:8080/cart
    @DeleteMapping
    // CHANGE 1: Changed from HttpStatus.NO_CONTENT (204) to HttpStatus.OK (200) to match Postman test.
    @ResponseStatus(HttpStatus.OK) // <--- Changed this line!
    // CHANGE 2: Now returns the empty ShoppingCart object to satisfy Postman's pm.response.json() expectation.
    public ShoppingCart clearCart(Principal principal)
    {
        try
        {
            int userId = getUserIdFromPrincipal(principal);
            shoppingCartDao.clearCart(userId);
            // CHANGE 3: Return the empty shopping cart object.
            return shoppingCartDao.getByUserId(userId);
        }
        catch (ResponseStatusException rse)
        {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error clearing shopping cart: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to clear shopping cart.", ex);
        }
    }
}
