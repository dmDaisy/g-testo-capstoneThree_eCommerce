package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.OrderDao;
import org.yearup.data.UserDao;
import org.yearup.models.Order;
import org.yearup.models.User;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("orders")
@CrossOrigin
@PreAuthorize("isAuthenticated()")
public class OrdersController
{
    private OrderDao orderDao;
    private UserDao userDao;

    @Autowired
    public OrdersController(OrderDao orderDao, UserDao userDao) {
        this.orderDao = orderDao;
        this.userDao = userDao;
    }

    @PostMapping("")
    public Order placeOrder(@RequestBody Order order, Principal principal)
    {
        try {
            User user = userDao.getByUserName(principal.getName());
            order.setUserId(user.getId());
            order.setDate(LocalDateTime.now());

            return orderDao.createOrder(order);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to place order.");
        }
    }

    @GetMapping("")
    public List<Order> getUserOrders(Principal principal)
    {
        try {
            User user = userDao.getByUserName(principal.getName());
            return orderDao.getOrdersByUserId(user.getId());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get orders.");
        }
    }
}
