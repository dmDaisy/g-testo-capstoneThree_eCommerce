package org.yearup.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.yearup.data.ProductDao;
import org.yearup.data.ShoppingCartDao;
import org.yearup.data.UserDao;
import org.yearup.models.ShoppingCart;
import org.yearup.models.ShoppingCartItem;
import org.yearup.models.User;

import java.security.Principal;
import java.util.Map;

// convert this class to a REST controller
@RestController
@RequestMapping("/cart")
@CrossOrigin
// only logged in users should have access to these actions
@PreAuthorize("isAuthenticated()")
public class ShoppingCartController
{
    // a shopping cart requires
    private ShoppingCartDao shoppingCartDao;
    private UserDao userDao;
    private ProductDao productDao;

    @Autowired
    public ShoppingCartController(ShoppingCartDao shoppingCartDao, UserDao userDao, ProductDao productDao) {
        this.shoppingCartDao = shoppingCartDao;
        this.userDao = userDao;
        this.productDao = productDao;
    }

    // each method in this controller requires a Principal object as a parameter
    @GetMapping("")
    public ShoppingCart getCart(Principal principal)
    {
        try
        {
            // get the currently logged in username
            String userName = principal.getName();
            // find database user by userId
            User user = userDao.getByUserName(userName);
            int userId = user.getId();

            // use the shoppingcartDao to get all items in the cart and return the cart
            return shoppingCartDao.getByUserId(userId);
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }

    // add a POST method to add a product to the cart - the url should be
    // https://localhost:8080/cart/products/15 (15 is the productId to be added
//    @PostMapping("/products/{productId}")
//    public ShoppingCartItem addToCart(@PathVariable int productId, Principal principal)
//    {
//        try
//        {
//            // get user from Principal
//            String userName = principal.getName();
//            User user = userDao.getByUserName(userName);
//            int userId = user.getId();
//
//            return shoppingCartDao.addToCart(userId, productId);
//        }
//        catch (IllegalStateException ex)
//        {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
//        }
//        catch(Exception e)
//        {
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
//        }
//    }
    @PostMapping("/products/{productId}")
    public ShoppingCart addToCart(@PathVariable int productId, Principal principal)
    {
        try
        {
            String userName = principal.getName();
            User user = userDao.getByUserName(userName);
            int userId = user.getId();

            shoppingCartDao.addToCart(userId, productId);

            // return updated cart
            return shoppingCartDao.getByUserId(userId);
        }
        catch (IllegalStateException ex)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }



    // add a PUT method to update an existing product in the cart - the url should be
    // https://localhost:8080/cart/products/15 (15 is the productId to be updated)
    // the BODY should be a ShoppingCartItem - quantity is the only value that will be updated
//    @PutMapping("/products/{productId}")
//    public void updateCartItem(@PathVariable int productId,
//                               @RequestParam int quantity,
//                               Principal principal)
//    {
//        try
//        {
//            if (quantity < 1)
//            {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Oops, haven't implemented removing item from cart yet...");
//            }
//            else{
//                String userName = principal.getName();
//                User user = userDao.getByUserName(userName);
//                int userId = user.getId();
//
//                shoppingCartDao.updateQuantity(userId, productId, quantity);
//            }
//        }
//        catch (IllegalStateException ex)
//        {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
//        }
//        catch(Exception e)
//        {
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
//        }
//    }

    @PutMapping("/products/{productId}")
    public ShoppingCart updateCartItem(
            @PathVariable int productId,
            @RequestBody Map<String, Integer> body,
            Principal principal
    )
    {
        try
        {
            String userName = principal.getName();
            User user = userDao.getByUserName(userName);
            int userId = user.getId();

            // get quantity from JSON body
            Integer quantity = body.get("quantity");

            if (quantity == null || quantity < 1)
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product doesn't exist or out of stock. :(");
            }

            // The shopping cart item should only be updated if the user has already added the product to their cart
            ShoppingCart cart = shoppingCartDao.getByUserId(userId);
            if (!cart.contains(productId))
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can only update products already in your cart. :(");
            }

            shoppingCartDao.updateQuantity(userId, productId, quantity);

            // return updated cart
            return shoppingCartDao.getByUserId(userId);
        }
        catch (IllegalStateException ex)
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }



    // add a DELETE method to clear all products from the current users cart
    // https://localhost:8080/cart
    @DeleteMapping("")
    public ShoppingCart clearCart(Principal principal)
    {
        try
        {
            String userName = principal.getName();
            User user = userDao.getByUserName(userName);
            int userId = user.getId();

            shoppingCartDao.clearCart(userId);
            return shoppingCartDao.getByUserId(userId);
        }
        catch(Exception e)
        {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Oops... our bad.");
        }
    }
}