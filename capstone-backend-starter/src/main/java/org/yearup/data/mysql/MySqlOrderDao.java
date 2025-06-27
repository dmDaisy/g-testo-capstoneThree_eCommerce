package org.yearup.data.mysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.OrderDao;
import org.yearup.data.UserDao;
import org.yearup.models.Order;
import org.yearup.models.OrderLineItem;
import org.yearup.models.User;

import javax.sql.DataSource;
import java.security.Principal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class MySqlOrderDao extends MySqlDaoBase implements OrderDao {

    public MySqlOrderDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Order create(Order order)
    {
        String sql = """
            INSERT INTO orders (user_id, date, address, city, state, zip, shipping_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, order.getUserId());
            stmt.setTimestamp(2, Timestamp.valueOf(order.getDate()));
            stmt.setString(3, order.getAddress());
            stmt.setString(4, order.getCity());
            stmt.setString(5, order.getState());
            stmt.setString(6, order.getZip());
            stmt.setBigDecimal(7, order.getShippingAmount());

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next())
            {
                order.setOrderId(keys.getInt(1));
            }

            return order;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void addLineItem(OrderLineItem item)
    {
        String sql = """
            INSERT INTO order_line_items (order_id, product_id, sales_price, quantity, discount)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, item.getOrderId());
            stmt.setInt(2, item.getProductId());
            stmt.setBigDecimal(3, item.getSalesPrice());
            stmt.setInt(4, item.getQuantity());
            stmt.setBigDecimal(5, item.getDiscount());

            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Order> getOrdersByUserId(int userId)
    {
        String sql = "SELECT * FROM orders WHERE user_id = ?";
        List<Order> orders = new ArrayList<>();

        try (Connection conn = getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next())
            {
                Order order = mapRow(rs);
                orders.add(order);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        return orders;
    }

    @Override
    public List<OrderLineItem> getLineItemsByOrderId(int orderId)
    {
        String sql = "SELECT * FROM order_line_items WHERE order_id = ?";
        List<OrderLineItem> items = new ArrayList<>();

        try (Connection conn = getConnection())
        {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next())
            {
                OrderLineItem item = new OrderLineItem();
                item.setOrderLineItemId(rs.getInt("order_line_item_id"));
                item.setOrderId(rs.getInt("order_id"));
                item.setProductId(rs.getInt("product_id"));
                item.setSalesPrice(rs.getBigDecimal("sales_price"));
                item.setQuantity(rs.getInt("quantity"));
                item.setDiscount(rs.getBigDecimal("discount"));

                items.add(item);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        return items;
    }

    // helper method: create an Order object from a ResultSet row
    private Order mapRow(ResultSet rs) throws SQLException {
        return new Order(
                rs.getInt("order_id"),
                rs.getInt("user_id"),
                rs.getTimestamp("date").toLocalDateTime(),
                rs.getString("address"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("zip"),
                rs.getBigDecimal("shipping_amount")
        );
    }
}
