package org.yearup.data;

import org.yearup.models.Order;
import org.yearup.models.OrderLineItem;

import java.util.List;

public interface OrderDao {
    Order create(Order order);
    void addLineItem(OrderLineItem item);
    List<Order> getOrdersByUserId(int id);
    List<OrderLineItem> getLineItemsByOrderId(int orderId);
}
