package io.artur.eventsourcing.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfig {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final String CONFIG_FILE = "/application.properties";
    
    private final Properties properties;
    private DataSource dataSource;
    
    public DatabaseConfig() {
        this.properties = loadProperties();
        this.dataSource = createDataSource();
        initializeSchema();
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    private Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                LOGGER.warning("Unable to find " + CONFIG_FILE);
                return properties;
            }
            properties.load(input);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not load properties file", e);
        }
        return properties;
    }
    
    private DataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(properties.getProperty("db.url", "jdbc:h2:mem:eventstore;DB_CLOSE_DELAY=-1"));
        config.setUsername(properties.getProperty("db.username", "sa"));
        config.setPassword(properties.getProperty("db.password", ""));
        config.setDriverClassName(properties.getProperty("db.driver", "org.h2.Driver"));
        
        // Performance and scalability settings
        config.setMaximumPoolSize(Integer.parseInt(properties.getProperty("db.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(properties.getProperty("db.pool.minIdle", "5")));
        config.setIdleTimeout(Long.parseLong(properties.getProperty("db.pool.idleTimeout", "600000"))); // 10 minutes
        config.setMaxLifetime(Long.parseLong(properties.getProperty("db.pool.maxLifetime", "1800000"))); // 30 minutes
        config.setConnectionTimeout(Long.parseLong(properties.getProperty("db.pool.connectionTimeout", "30000"))); // 30 seconds
        config.setValidationTimeout(Long.parseLong(properties.getProperty("db.pool.validationTimeout", "5000"))); // 5 seconds
        
        // Performance optimizations
        config.setLeakDetectionThreshold(Long.parseLong(properties.getProperty("db.pool.leakDetectionThreshold", "60000"))); // 1 minute
        
        // Prepared statement caching (set as data source properties)
        config.addDataSourceProperty("cachePrepStmts", properties.getProperty("db.cache.preparedStatements", "true"));
        config.addDataSourceProperty("prepStmtCacheSize", properties.getProperty("db.cache.preparedStatementsSize", "250"));
        config.addDataSourceProperty("prepStmtCacheSqlLimit", properties.getProperty("db.cache.preparedStatementsSqlLimit", "2048"));
        config.addDataSourceProperty("useServerPrepStmts", properties.getProperty("db.cache.useServerPreparedStatements", "true"));
        
        // Connection health checks
        config.setConnectionTestQuery(properties.getProperty("db.healthCheck.query", "SELECT 1"));
        
        // Pool name for monitoring
        config.setPoolName(properties.getProperty("db.pool.name", "EventSourcingPool"));
        
        // Register metrics if enabled
        if (Boolean.parseBoolean(properties.getProperty("db.pool.metrics.enabled", "true"))) {
            config.setMetricRegistry(new com.codahale.metrics.MetricRegistry());
        }
        
        LOGGER.info(String.format("Creating connection pool: maxSize=%d, minIdle=%d, poolName=%s", 
                config.getMaximumPoolSize(), config.getMinimumIdle(), config.getPoolName()));
        
        return new HikariDataSource(config);
    }
    
    private void initializeSchema() {
        String schemaFile = properties.getProperty("db.schema");
        if (schemaFile == null || !schemaFile.startsWith("classpath:")) {
            LOGGER.info("No schema file specified or invalid format");
            return;
        }
        
        String resourcePath = "/" + schemaFile.substring("classpath:".length());
        try (InputStream schemaInput = getClass().getResourceAsStream(resourcePath)) {
            if (schemaInput == null) {
                LOGGER.warning("Unable to find schema file: " + resourcePath);
                return;
            }
            
            String schema = new String(schemaInput.readAllBytes());
            try (Connection conn = dataSource.getConnection()) {
                for (String statement : schema.split(";")) {
                    if (!statement.trim().isEmpty()) {
                        try (var stmt = conn.createStatement()) {
                            stmt.execute(statement.trim());
                        }
                    }
                }
            }
            
        } catch (IOException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Could not initialize database schema", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}