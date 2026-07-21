package com.aerionsoft.application.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Value("${db.name:#{null}}")
    private String dbNameArg;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.idle-timeout:30000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:300000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Bean
    @Primary
    public DataSource dataSource() {
        // If a db.name is explicitly provided, replace only the database part of the configured URL.
        // Otherwise use the full configured datasourceUrl as-is.
        String jdbcUrl = buildJdbcUrlWithOptionalDbName(datasourceUrl, dbNameArg);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setConnectionTimeout(connectionTimeout);

        return new HikariDataSource(config);
    }

    private String buildJdbcUrlWithOptionalDbName(String url, String dbNameArg) {
        if (url == null || url.isBlank()) {
            return url;
        }

        // Preserve any query string (e.g. ?sslmode=disable)
        int qIndex = url.indexOf('?');
        String query = qIndex >= 0 ? url.substring(qIndex) : "";
        String base = qIndex >= 0 ? url.substring(0, qIndex) : url;

        if (dbNameArg == null || dbNameArg.isBlank()) {
            return base + query;
        }

        // Replace the part after the last slash with the provided db name.
        int lastSlash = base.lastIndexOf('/');
        if (lastSlash >= 0) {
            String prefix = base.substring(0, lastSlash + 1);
            return prefix + dbNameArg + query;
        }

        // Fallback: append the db name
        return base + "/" + dbNameArg + query;
    }
}