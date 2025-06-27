package org.yearup.data;

import org.yearup.models.Product;

import java.math.BigDecimal;
import java.util.List;

// This interface defines the contract for interacting with product data.
public interface ProductDao
{
    // Retrieves a list of all products.
    List<Product> getAllProducts();

    // Retrieves a single product by its ID.
    Product getById(int productId);

    // Creates a new product in the database.
    Product create(Product product);

    // Updates an existing product in the database.
    void update(int productId, Product product);

    // Deletes a product from the database by its ID.
    void delete(int productId);

    // Retrieves a list of products by category ID.
    List<Product> getProductsByCategoryId(int categoryId);

    // Searches for products based on various criteria (category, price range, color).
    List<Product> search(Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String color);

    // NEW: Updates the stock of a product by a specified amount.
    // Use a negative quantityChange to decrement stock (e.g., when an item is ordered).
    void updateStock(int productId, int quantityChange);
}