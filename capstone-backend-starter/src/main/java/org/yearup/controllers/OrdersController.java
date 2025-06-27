package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.OrderDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class OrdersController
{
    private OrderDao orderDao;
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;

    @Autowired
    public OrdersController(OrderDao orderDao, ShoppingCartDao shoppingCartDao, UserDao userDao)
    {
        this.orderDao = orderDao;
        this.shoppingCartDao = shoppingCartDao;
        this.userDao = userDao;
    }

    @PostMapping
    public void checkout(Principal principal)
    {
        try
        {
            String username = principal.getName();
            User user = userDao.getByUserName(username);
            int userId = user.getId();

            // get shopping cart
            ShoppingCart cart = shoppingCartDao.getByUserId(userId);

            if(cart.getItems().isEmpty())
            {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Cart is empty :(");
            }

            Order order = new Order(); // create no param order with default values
            order.setUserId(userId);

            order = orderDao.create(order); // save to DB

            // add each ShoppingCartItem as an OrderLineItem
            for (ShoppingCartItem item : cart.getItems().values())
            {
                OrderLineItem lineItem = new OrderLineItem();
                lineItem.setOrderId(order.getOrderId());
                lineItem.setProductId(item.getProduct().getProductId());
                lineItem.setSalesPrice(item.getProduct().getPrice());
                lineItem.setQuantity(item.getQuantity());
                lineItem.setDiscount(BigDecimal.ZERO);

                orderDao.addLineItem(lineItem);
            }

            // clear cart
            shoppingCartDao.clearCart(userId);
        }
        catch (Exception e)
        {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Checkout failed");
        }
    }
}
