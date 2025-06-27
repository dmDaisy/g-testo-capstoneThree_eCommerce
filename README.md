# EasyShop eCommerce Spring Boot Web App

A full-stack Java app simulating an online store with features including user authentication, shopping cart, 
order checkout, and admin product/category management.

---

## Description

EasyShop is an eCommerce backend built with Spring Boot, JDBC, and MySQL. It supports user login, 
role-based authorization, product browsing by category, shopping cart functionality, and checkout with order creation. 
Admin users can manage products and categories.

---

## Features

- User authentication (JWT)
- Role-based authorization (Admin/User)
- Browse products by category
- Add/remove items from shopping cart
- Update cart quantities
- Checkout and create orders
- Admin CRUD for Products and Categories
- Profile management for users

---

## Requirements

- Java 17
- Spring Boot 3.x
- Spring Security (JWT)
- JDBC
- MySQL
- Postman (API testing)

---

## Setup

Clone the repository:
```bash
git clone https://github.com/dmDaisy/g-testo-capstoneThree_eCommerce
```

Open in your IDE and build with Maven:
```bash
mvn clean install
```

Set up your MySQL database:
- Create database named `easyshop`
- Run provided DB script

Configure `application.properties` with your DB connection:
```
spring.datasource.url=jdbc:mysql://localhost:3306/easyshop
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD
jwt.secret=YOUR_SECRET_KEY
```

Run the Spring Boot app:
```bash
mvn spring-boot:run
```

Use Postman to test endpoints:
- Add Authorization header with your JWT Bearer token

---

## Screenshots

### Login

![Screenshot 2025-06-27 at 4 36 18 AM](https://github.com/user-attachments/assets/a0d80d82-6ec8-43ee-b8a7-eb84c529aa69)

### View/Edit profile

![Screenshot 2025-06-27 at 4 36 49 AM](https://github.com/user-attachments/assets/25e932c1-5293-42d4-9711-07047ea5fce8)

### View/Edit cart

![Screenshot 2025-06-27 at 4 37 12 AM](https://github.com/user-attachments/assets/fc8576d3-c4d5-431b-a0eb-2614e7fef95f)


### Interesting Piece of Code
**Updated signature to be compatible with frontend code**

```java
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
