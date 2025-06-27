package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProductDao;
import org.yearup.models.Product;

import java.math.BigDecimal;
import java.util.List;

// Marks this class as a REST controller, handling incoming web requests.
@RestController
// Allows cross-origin requests, essential for front-end applications running on different domains/ports.
@CrossOrigin
// Defines the base path for all endpoints in this controller.
@RequestMapping("products")
public class ProductsController
{
    private ProductDao productDao; // Inject the ProductDao for database operations.

    // Constructor for dependency injection. Spring automatically provides an instance of ProductDao.
    @Autowired
    public ProductsController(ProductDao productDao)
    {
        this.productDao = productDao;
    }

    // GET: /products
    // Retrieves all products or searches based on query parameters.
    // This method addresses Bug 1 (Product Search Functionality).
    @GetMapping
    @PreAuthorize("permitAll()") // Allows all users to view products.
    public List<Product> searchProducts(
            @RequestParam(name = "cat", required = false) Integer categoryId,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "color", required = false) String color
    )
    {
        try
        {
            // If any search parameters are provided, call the search method in the DAO.
            if (categoryId != null || minPrice != null || maxPrice != null || (color != null && !color.isEmpty()))
            {
                return productDao.search(categoryId, minPrice, maxPrice, color);
            } else
            {
                // If no search parameters, return all products.
                return productDao.getAllProducts();
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error searching or retrieving all products: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }

    // GET: /products/{id}
    // Retrieves a single product by its ID.
    @GetMapping("{id}")
    @PreAuthorize("permitAll()") // Allows all users to view individual products.
    public Product getProductById(@PathVariable int id)
    {
        try
        {
            Product product = productDao.getById(id);
            if (product == null)
            {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found.");
            }
            return product;
        }
        catch (Exception ex)
        {
            System.err.println("Error retrieving product by ID " + id + ": " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }

    // POST: /products
    // Creates a new product. Only accessible by users with the 'ADMIN' role.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Restricts access to ADMIN users only.
    @ResponseStatus(HttpStatus.CREATED) // Sets the default success status code to 201 Created.
    public Product addProduct(@RequestBody Product product)
    {
        try
        {
            // Basic validation: ensure product name and category ID are not null/empty.
            if (product.getName() == null || product.getName().trim().isEmpty() || product.getCategoryId() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name and category ID are required.");
            }
            return productDao.create(product);
        }
        catch (ResponseStatusException rse) {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error adding new product: " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add product.");
        }
    }

    // PUT: /products/{id}
    // Updates an existing product. Only accessible by users with the 'ADMIN' role.
    // This method uses the fixed update in MySqlProductDao to address Bug 2 (Duplication).
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')") // Restricts access to ADMIN users only.
    public void updateProduct(@PathVariable int id, @RequestBody Product product)
    {
        try
        {
            // Check if the product exists before attempting to update.
            Product existingProduct = productDao.getById(id);
            if (existingProduct == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found for update.");
            }
            // Basic validation for required fields on update.
            if (product.getName() == null || product.getName().trim().isEmpty() || product.getCategoryId() == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name and category ID are required for update.");
            }
            productDao.update(id, product);
        }
        catch (ResponseStatusException rse) {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error updating product ID " + id + ": " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update product.");
        }
    }

    // DELETE: /products/{id}
    // Deletes a product. Only accessible by users with the 'ADMIN' role.
    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')") // Restricts access to ADMIN users only.
    @ResponseStatus(HttpStatus.NO_CONTENT) // Sets the default success status code to 204 No Content.
    public void deleteProduct(@PathVariable int id)
    {
        try
        {
            // Check if the product exists before attempting to delete.
            Product existingProduct = productDao.getById(id);
            if (existingProduct == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found for deletion.");
            }
            productDao.delete(id);
        }
        catch (ResponseStatusException rse) {
            throw rse;
        }
        catch (Exception ex)
        {
            System.err.println("Error deleting product ID " + id + ": " + ex.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete product.");
        }
    }
}