package org.yearup.configurations;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.sql.DataSource;

// This class configures the database connection for the Spring application.
@Configuration // Marks this class as a source of bean definitions.
@PropertySource("classpath:application.properties") // Specifies the properties file to load.
public class DatabaseConfig
{
    // Injects the database URL from application.properties.
    @Value("${datasource.url}")
    private String databaseUrl;

    // Injects the database username from application.properties.
    @Value("${datasource.username}")
    private String databaseUsername;

    // Injects the database password from application.properties.
    @Value("${datasource.password}")
    private String databasePassword;

    // Defines a Spring bean for the DataSource.
    // This DataSource will be used by DAOs to obtain database connections.
    @Bean
    public DataSource dataSource()
    {
        // Using Apache Commons DBCP2 for connection pooling.
        // Connection pooling improves performance by reusing database connections.
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(databaseUrl); // Set the database URL.
        dataSource.setUsername(databaseUsername); // Set the database username.
        dataSource.setPassword(databasePassword); // Set the database password.
        return dataSource; // Return the configured DataSource.
    }
}