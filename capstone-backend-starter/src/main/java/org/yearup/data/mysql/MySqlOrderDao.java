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
    public Order createOrder(Order order)
    {
        String insertOrderSql = """
            INSERT INTO orders (user_id, date, address, city, state, zip, shipping_amount)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        String insertItemsSql = """
            INSERT INTO order_items (order_id, product_id, quantity)
            VALUES (?, ?, ?)
        """;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // insert order
            PreparedStatement orderStmt = conn.prepareStatement(insertOrderSql, Statement.RETURN_GENERATED_KEYS);
            orderStmt.setInt(1, order.getUserId());
            orderStmt.setTimestamp(2, Timestamp.valueOf(order.getDate()));
            orderStmt.setString(3, order.getAddress());
            orderStmt.setString(4, order.getCity());
            orderStmt.setString(5, order.getState());
            orderStmt.setString(6, order.getZip());
            orderStmt.setBigDecimal(7, order.getShippingAmount());
            orderStmt.executeUpdate();

            ResultSet keys = orderStmt.getGeneratedKeys();
            if (keys.next()) {
                int orderId = keys.getInt(1);
                order.setOrderId(orderId);

//                // insert order items
//                for (var item : order.getProducts().entrySet()) {
//                    int productId = item.getKey();
//                    int quantity = item.getValue();
//
//                    PreparedStatement itemStmt = conn.prepareStatement(insertItemsSql);
//                    itemStmt.setInt(1, orderId);
//                    itemStmt.setInt(2, productId);
//                    itemStmt.setInt(3, quantity);
//                    itemStmt.executeUpdate();
//                }

                conn.commit();
                return order;
            } else {
                conn.rollback();
                throw new SQLException("Failed to insert order");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create order", e);
        }
    }

    @Override
    public List<Order> getOrdersByUserId(int userId)
    {
        List<Order> orders = new ArrayList<>();

        String sql = "SELECT * FROM orders WHERE user_id = ?";

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                orders.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return orders;
    }

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
