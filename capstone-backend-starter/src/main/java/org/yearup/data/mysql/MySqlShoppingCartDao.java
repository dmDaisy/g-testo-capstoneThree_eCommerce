package org.yearup.data.mysql;
import org.springframework.stereotype.Component;
import org.yearup.data.ShoppingCartDao;
import org.yearup.models.Product;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Component
public class MySqlShoppingCartDao extends MySqlDaoBase implements ShoppingCartDao {

    public MySqlShoppingCartDao(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ShoppingCart getByUserId(int userId)
    {
        String sql = """
            SELECT sc.product_id, sc.quantity, p.*
            FROM shopping_cart sc
            JOIN products p ON sc.product_id = p.product_id
            WHERE sc.user_id = ?
        """;

        ShoppingCart cart = new ShoppingCart();
        Map<Integer, ShoppingCartItem> items = new HashMap<>();

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Product product = mapRow(rs);
                int quantity = rs.getInt("quantity");
                ShoppingCartItem item = new ShoppingCartItem(product, quantity, 0);
                items.put(product.getProductId(), item);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

        cart.setItems(items);
        return cart;
    }


    @Override
    public ShoppingCartItem addToCart(int userId, int productId)
    {
        int stock = getProductStock(productId);
        int cartQuantity = getCartQuantity(userId, productId);

        // check if item in stock
        if (cartQuantity + 1 > stock) {
            throw new IllegalStateException("Item out of stock.");
        }

        if (cartQuantity > 0) { // already in cart
            updateQuantity(userId, productId, cartQuantity + 1);
        } else { // new to cart
            insertToCart(userId, productId, 1);
        }

        // return updated cart item
        Product product = getProduct(productId);
        return new ShoppingCartItem(product, cartQuantity + 1, 0);
    }

    @Override
    public void updateQuantity(int userId, int productId, int quantity)
    {
        int stock = getProductStock(productId);

        if (quantity > stock) {
            throw new IllegalStateException("Cannot set quantity greater than available stock.");
        }

        String sql = "UPDATE shopping_cart SET quantity = ? WHERE user_id = ? AND product_id = ?";

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, quantity);
            stmt.setInt(2, userId);
            stmt.setInt(3, productId);
            int rows = stmt.executeUpdate();

            // If no row existed, optionally insert instead:
            if (rows == 0) {
                insertToCart(userId, productId, quantity);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearCart(int userId)
    {
        String sql = "DELETE FROM shopping_cart WHERE user_id = ?";

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // helper methods below----------------------------------------------------------

    // helper method to check product stock
    private int getProductStock(int productId)
    {
        String sql = "SELECT stock FROM products WHERE product_id = ?";
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("stock");
            }
            else {
                throw new IllegalStateException("Product not found.");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // helper method to check product quantity in cart
    private int getCartQuantity(int userId, int productId)
    {
        String sql = "SELECT quantity FROM shopping_cart WHERE user_id = ? AND product_id = ?";
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("quantity");
            }
            return 0;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // helper method to insert NEW product to cart
    private void insertToCart(int userId, int productId, int quantity)
    {
        String sql = "INSERT INTO shopping_cart (user_id, product_id, quantity) VALUES (?, ?, ?)";
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // helper method to get Product object by id
    private Product getProduct(int productId)
    {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
            else {
                throw new IllegalStateException("Product not found.");
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // helper method to create Product from a ResultSet row
    private Product mapRow(ResultSet rs) throws SQLException
    {
        return new Product(
                rs.getInt("product_id"),
                rs.getString("name"),
                rs.getBigDecimal("price"),
                rs.getInt("category_id"),
                rs.getString("description"),
                rs.getString("color"),
                rs.getInt("stock"),
                rs.getBoolean("featured"),
                rs.getString("image_url")
        );
    }
}