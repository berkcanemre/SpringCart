package org.yearup.models;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

// Represents a user's entire shopping cart.
// It contains a map of ShoppingCartItems, where the key is the product ID,
// and calculates the total price of all items in the cart.
public class ShoppingCart
{
    // A map to store ShoppingCartItem objects, keyed by their product ID.
    // This allows for easy lookup and update of items in the cart.
    private Map<Integer, ShoppingCartItem> items;
    private BigDecimal total; // The total price of all items in the cart.

    public ShoppingCart()
    {
        this.items = new HashMap<>();
        this.total = BigDecimal.ZERO; // Initialize total to zero.
    }

    // --- Getters and Setters ---

    public Map<Integer, ShoppingCartItem> getItems()
    {
        return items;
    }

    // Note: This setter might be used for deserialization but generally,
    // items are added/removed via specific methods (e.g., add()).
    public void setItems(Map<Integer, ShoppingCartItem> items)
    {
        this.items = items;
        calculateTotal(); // Recalculate total if the entire items map is set.
    }

    public BigDecimal getTotal()
    {
        return total;
    }

    // This setter is typically not used directly as total is calculated.
    public void setTotal(BigDecimal total)
    {
        this.total = total;
    }

    // Method to add an item to the shopping cart.
    // If the product already exists, its quantity is updated. Otherwise, a new item is added.
    public void add(ShoppingCartItem item)
    {
        if (items.containsKey(item.getProduct().getProductId()))
        {
            // If item already exists, update its quantity.
            ShoppingCartItem existingItem = items.get(item.getProduct().getProductId());
            existingItem.setQuantity(existingItem.getQuantity() + item.getQuantity());
        } else
        {
            // Otherwise, add the new item.
            items.put(item.getProduct().getProductId(), item);
        }
        calculateTotal(); // Recalculate total after adding/updating items.
    }

    // Method to remove an item from the shopping cart.
    public void remove(int productId)
    {
        items.remove(productId);
        calculateTotal(); // Recalculate total after removing an item.
    }

    // Method to clear all items from the shopping cart.
    public void clear()
    {
        items.clear();
        calculateTotal(); // Recalculate total after clearing the cart.
    }

    // Method to update the quantity of an existing item in the cart.
    public void updateQuantity(int productId, int newQuantity)
    {
        if (items.containsKey(productId))
        {
            ShoppingCartItem item = items.get(productId);
            if (newQuantity <= 0)
            {
                // If new quantity is 0 or less, remove the item from the cart.
                remove(productId);
            } else
            {
                // Otherwise, update the quantity.
                item.setQuantity(newQuantity);
            }
            calculateTotal(); // Recalculate total after updating quantity.
        }
        // If the product is not in the cart, do nothing (or throw an error based on business logic).
    }

    // Helper method to calculate the total price of all items in the cart.
    private void calculateTotal()
    {
        BigDecimal currentTotal = BigDecimal.ZERO;
        for (ShoppingCartItem item : items.values())
        {
            currentTotal = currentTotal.add(item.getLineTotal());
        }
        this.total = currentTotal;
    }

    @Override
    public String toString() {
        return "ShoppingCart{" +
                "items=" + items +
                ", total=" + total +
                '}';
    }
}