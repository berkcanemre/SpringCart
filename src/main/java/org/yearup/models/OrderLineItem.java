package org.yearup.models;

import java.math.BigDecimal;

// Represents a single item (product) within an order.
// This model maps to the 'order_line_items' table in the database.
public class OrderLineItem
{
    private int orderLineItemId;
    private int orderId;
    private Product product;    // The actual product details at the time of purchase.
    private BigDecimal salesPrice; // The price the product was sold at (can differ from current product price).
    private int quantity;       // Quantity of this product in the order.
    private BigDecimal discount;   // Discount applied to this specific line item (as an amount).

    public OrderLineItem()
    {
        this.discount = BigDecimal.ZERO; // Default discount to zero.
    }

    // Constructor for creating a new line item (e.g., from a shopping cart item).
    public OrderLineItem(int orderId, Product product, BigDecimal salesPrice, int quantity, BigDecimal discount)
    {
        this.orderId = orderId;
        this.product = product;
        this.salesPrice = salesPrice;
        this.quantity = quantity;
        this.discount = discount;
    }

    // Constructor for loading from the database.
    public OrderLineItem(int orderLineItemId, int orderId, Product product, BigDecimal salesPrice, int quantity, BigDecimal discount)
    {
        this.orderLineItemId = orderLineItemId;
        this.orderId = orderId;
        this.product = product;
        this.salesPrice = salesPrice;
        this.quantity = quantity;
        this.discount = discount;
    }

    // --- Getters and Setters ---

    public int getOrderLineItemId()
    {
        return orderLineItemId;
    }

    public void setOrderLineItemId(int orderLineItemId)
    {
        this.orderLineItemId = orderLineItemId;
    }

    public int getOrderId()
    {
        return orderId;
    }

    public void setOrderId(int orderId)
    {
        this.orderId = orderId;
    }

    public Product getProduct()
    {
        return product;
    }

    public void setProduct(Product product)
    {
        this.product = product;
    }

    public BigDecimal getSalesPrice()
    {
        return salesPrice;
    }

    public void setSalesPrice(BigDecimal salesPrice)
    {
        this.salesPrice = salesPrice;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public void setQuantity(int quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal getDiscount()
    {
        return discount;
    }

    public void setDiscount(BigDecimal discount)
    {
        this.discount = discount;
    }

    // Calculates the total for this line item (salesPrice * quantity - discount).
    public BigDecimal getLineTotal()
    {
        // Calculate (salesPrice * quantity) and subtract the discount amount.
        return salesPrice.multiply(BigDecimal.valueOf(quantity)).subtract(discount);
    }

    @Override
    public String toString() {
        return "OrderLineItem{" +
                "orderLineItemId=" + orderLineItemId +
                ", orderId=" + orderId +
                ", product=" + product +
                ", salesPrice=" + salesPrice +
                ", quantity=" + quantity +
                ", discount=" + discount +
                '}';
    }
}