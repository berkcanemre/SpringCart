package org.yearup.data;

import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

// This interface defines the contract for interacting with shopping cart data.
public interface ShoppingCartDao
{
    // Retrieves the shopping cart for a specific user ID.
    // The ShoppingCart object will contain all items currently in the user's cart.
    ShoppingCart getByUserId(int userId);

    // Adds a product to the user's shopping cart.
    // If the product already exists in the cart, its quantity should be incremented.
    // If it's a new product, it should be added with a quantity of 1.
    void addProductToCart(int userId, int productId);

    // Updates the quantity of a specific product in the user's shopping cart.
    // If newQuantity is 0 or less, the item should be removed from the cart.
    void updateProductQuantity(int userId, int productId, int quantity);

    // Clears all items from a user's shopping cart.
    void clearCart(int userId);

    // NEW: Removes a specific product from a user's shopping cart.
    void removeProductFromCart(int userId, int productId);

    // Checks if a specific product exists in the user's cart.
    boolean productExistsInCart(int userId, int productId);

    // Get a specific cart item for a user and product.
    ShoppingCartItem getCartItemByUserIdAndProductId(int userId, int productId);
}