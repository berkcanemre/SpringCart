package org.yearup.data.mysql;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

// This abstract class serves as a base for all MySQL Data Access Objects (DAOs).
// It provides common functionality, such as obtaining a database connection.
public abstract class MySqlDaoBase
{
    protected DataSource dataSource; // DataSource for obtaining database connections.

    // Constructor to inject the DataSource.
    public MySqlDaoBase(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    // Helper method to get a database connection from the DataSource.
    // This method handles potential SQLException and re-throws it as a RuntimeException
    // for consistent error handling across DAOs.
    protected Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }
}