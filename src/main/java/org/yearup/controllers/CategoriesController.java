package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.CategoryDao;
import org.yearup.data.ProductDao; // Likely needed for search or related operations
import org.yearup.models.Category;
import org.yearup.models.Product; // Assuming you might use Product in search in future

import java.util.List;

// Marks this class as a REST controller.
@RestController
// Allows cross-origin requests.
@CrossOrigin
// Sets the base path for all endpoints.
@RequestMapping("categories")
@PreAuthorize("permitAll()") // Changed to permitAll to allow anonymous browsing of categories
public class CategoriesController
{
    private CategoryDao categoryDao;
    // Potentially needed for future product-related functionality within categories
    // private ProductDao productDao;

    // Constructor for dependency injection.
    @Autowired
    public CategoriesController(CategoryDao categoryDao) //, ProductDao productDao)
    {
        this.categoryDao = categoryDao;
        // this.productDao = productDao;
    }

    // GET: http://localhost:8080/categories
    @GetMapping
    public List<Category> getAll()
    {
        try
        {
            return categoryDao.getAllCategories();
        }
        catch(Exception ex)
        {
            // For any unexpected server errors, return a generic 500.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.", ex);
        }
    }

    // GET: http://localhost:8080/categories/{id}
    @GetMapping("{id}")
    public Category getById(@PathVariable int id)
    {
        try
        {
            Category category = categoryDao.getById(id);
            if (category == null)
            {
                // If category is not found by DAO, return 404 Not Found.
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found.");
            }
            return category;
        }
        catch(ResponseStatusException rse)
        {
            // If the DAO explicitly throws a ResponseStatusException (e.g., 404 from getById),
            // re-throw it directly without changing the status.
            throw rse;
        }
        catch(Exception ex)
        {
            // For any other unexpected exceptions, wrap them in a generic 500.
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.", ex);
        }
    }

    // POST: http://localhost:8080/categories
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 Created on successful creation.
    @PreAuthorize("hasRole('ADMIN')") // Only admins can create categories.
    public Category addCategory(@RequestBody Category category)
    {
        try
        {
            return categoryDao.create(category);
        }
        catch(Exception ex)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create category.", ex);
        }
    }

    // PUT: http://localhost:8080/categories/{id}
    @PutMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')") // Only admins can update categories.
    public void updateCategory(@PathVariable int id, @RequestBody Category category)
    {
        try
        {
            categoryDao.update(id, category);
        }
        catch (ResponseStatusException rse)
        {
            // Re-throw specific status exceptions like 404 if the category to update is not found.
            throw rse;
        }
        catch(Exception ex)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update category.", ex);
        }
    }

    // DELETE: http://localhost:8080/categories/{id}
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Returns 204 No Content on successful deletion.
    @PreAuthorize("hasRole('ADMIN')") // Only admins can delete categories.
    public void deleteCategory(@PathVariable int id)
    {
        try
        {
            categoryDao.delete(id);
        }
        catch (ResponseStatusException rse)
        {
            // Re-throw specific status exceptions like 404 if the category to delete is not found.
            throw rse;
        }
        catch(Exception ex)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete category.", ex);
        }
    }
}