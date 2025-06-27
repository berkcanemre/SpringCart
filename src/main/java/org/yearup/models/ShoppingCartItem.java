package org.yearup.models;

import java.math.BigDecimal;

// Represents an individual item within a user's shopping cart.
// It includes the product details, quantity, discount, and calculated line total.
public class ShoppingCartItem
{
    private Product product; // The product associated with this cart item.
    private int quantity;    // The quantity of this product in the cart.
    private int discountPercent; // Discount applied to this item (e.g., 0 for no discount).
    private BigDecimal lineTotal; // Calculated total for this line item (price * quantity * (1 - discountPercent/100)).

    public ShoppingCartItem()
    {
        this.discountPercent = 0; // Default to no discount.
        this.lineTotal = BigDecimal.ZERO; // Default to zero.
    }

    public ShoppingCartItem(Product product, int quantity)
    {
        this.product = product;
        this.quantity = quantity;
        this.discountPercent = 0; // Default to no discount.
        calculateLineTotal(); // Calculate line total upon creation.
    }

    // --- Getters and Setters ---

    public Product getProduct()
    {
        return product;
    }

    public void setProduct(Product product)
    {
        this.product = product;
        calculateLineTotal(); // Recalculate if product (and thus price) changes.
    }

    public int getQuantity()
    {
        return quantity;
    }

    public void setQuantity(int quantity)
    {
        this.quantity = quantity;
        calculateLineTotal(); // Recalculate if quantity changes.
    }

    public int getDiscountPercent()
    {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent)
    {
        this.discountPercent = discountPercent;
        calculateLineTotal(); // Recalculate if discount changes.
    }

    public BigDecimal getLineTotal()
    {
        return lineTotal;
    }

    // This setter is typically not used directly from outside as lineTotal is calculated.
    // It's here for JSON serialization/deserialization if needed.
    public void setLineTotal(BigDecimal lineTotal)
    {
        this.lineTotal = lineTotal;
    }

    // Helper method to calculate the line total based on product price, quantity, and discount.
    private void calculateLineTotal()
    {
        if (this.product != null && this.product.getPrice() != null)
        {
            BigDecimal price = this.product.getPrice();
            BigDecimal quantityBd = BigDecimal.valueOf(this.quantity);
            BigDecimal discountFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(this.discountPercent).divide(BigDecimal.valueOf(100), BigDecimal.ROUND_HALF_UP));

            this.lineTotal = price.multiply(quantityBd).multiply(discountFactor);
        } else
        {
            this.lineTotal = BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return "ShoppingCartItem{" +
                "product=" + product +
                ", quantity=" + quantity +
                ", discountPercent=" + discountPercent +
                ", lineTotal=" + lineTotal +
                '}';
    }
}