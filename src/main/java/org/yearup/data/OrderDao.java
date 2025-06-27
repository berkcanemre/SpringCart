package org.yearup.data;

import org.yearup.models.Order;
import org.yearup.models.OrderLineItem;

// This interface defines the contract for interacting with order and order line item data.
public interface OrderDao
{
    // Creates a new order in the database.
    // Returns the created Order object, typically with its generated order ID.
    Order createOrder(Order order);

    // Creates a new order line item for a given order in the database.
    // Returns the created OrderLineItem object, typically with its generated ID.
    OrderLineItem createOrderLineItem(OrderLineItem orderLineItem);
}