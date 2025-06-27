package org.yearup.data.mysql;

import org.yearup.data.ShoppingCartItemDao;

import javax.sql.DataSource;

public class MySqlShoppingCartItemDao extends MySqlDaoBase implements ShoppingCartItemDao {
    public MySqlShoppingCartItemDao(DataSource dataSource) {
        super(dataSource);
    }
}
