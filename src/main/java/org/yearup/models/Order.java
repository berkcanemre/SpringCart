package org.yearup.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Represents a customer's order.
// This model maps to the 'orders' table in the database.
public class Order
{
    private int orderId;
    private int userId;
    private LocalDateTime date; // Timestamp of the order.
    private String address;     // Shipping address details.
    private String city;
    private String state;
    private String zip;
    private BigDecimal shippingAmount; // Cost of shipping for this order.
    private List<OrderLineItem> lineItems; // List of products and quantities in this order.

    public Order()
    {
        this.date = LocalDateTime.now(); // Default to current time when a new order object is created.
        this.shippingAmount = BigDecimal.ZERO; // Default shipping amount.
        this.lineItems = new ArrayList<>(); // Initialize line items list.
    }

    public Order(int orderId, int userId, LocalDateTime date, String address, String city, String state, String zip, BigDecimal shippingAmount)
    {
        this.orderId = orderId;
        this.userId = userId;
        this.date = date;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.shippingAmount = shippingAmount;
        this.lineItems = new ArrayList<>(); // Initialize line items list.
    }

    // --- Getters and Setters ---

    public int getOrderId()
    {
        return orderId;
    }

    public void setOrderId(int orderId)
    {
        this.orderId = orderId;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public LocalDateTime getDate()
    {
        return date;
    }

    public void setDate(LocalDateTime date)
    {
        this.date = date;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getZip()
    {
        return zip;
    }

    public void setZip(String zip)
    {
        this.zip = zip;
    }

    public BigDecimal getShippingAmount()
    {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount)
    {
        this.shippingAmount = shippingAmount;
    }

    public List<OrderLineItem> getLineItems()
    {
        return lineItems;
    }

    public void setLineItems(List<OrderLineItem> lineItems)
    {
        this.lineItems = lineItems;
    }

    // Helper method to add an OrderLineItem to the order.
    public void addLineItem(OrderLineItem item)
    {
        this.lineItems.add(item);
    }

    // Calculates the total cost of the order (sum of line item totals + shipping).
    public BigDecimal getTotal()
    {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineItem item : lineItems)
        {
            total = total.add(item.getLineTotal());
        }
        return total.add(shippingAmount);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                ", date=" + date +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip='" + zip + '\'' +
                ", shippingAmount=" + shippingAmount +
                ", lineItems=" + lineItems +
                '}';
    }
}