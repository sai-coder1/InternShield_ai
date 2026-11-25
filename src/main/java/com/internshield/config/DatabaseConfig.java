package com.internshield.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url:}")
    private String springDatasourceUrl;

    @Value("${database.url:${DATABASE_URL:}}")
    private String databaseUrl;

    @Value("${DB_HOST:}")
    private String dbHost;

    @Value("${DB_PORT:}")
    private String dbPort;

    @Value("${DB_NAME:}")
    private String dbName;

    @Value("${DB_USERNAME:${spring.datasource.username:}}")
    private String dbUsername;

    @Value("${DB_PASSWORD:${spring.datasource.password:}}")
    private String dbPassword;

    @Value("${DB_SSL_MODE:}")
    private String dbSslMode;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:600000}")
    private long maxLifetime;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();

        ResolvedDatabaseDetails details = resolveDatabaseDetails();

        log.info("Configuring DataSource with JDBC URL: {}", maskUrl(details.jdbcUrl));
        log.info("Configuring DataSource with Driver: {}", details.driverClassName);

        config.setJdbcUrl(details.jdbcUrl);
        config.setDriverClassName(details.driverClassName);

        if (details.username != null && !details.username.trim().isEmpty()) {
            config.setUsername(details.username.trim());
        }
        if (details.password != null) {
            config.setPassword(details.password);
        }

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        return new HikariDataSource(config);
    }

    public ResolvedDatabaseDetails resolveDatabaseDetails() {
        // 1. Check DATABASE_URL (standard for Render and Supabase connection string)
        if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
            return parseConnectionUrl(databaseUrl.trim());
        }

        // 2. Check spring.datasource.url or SPRING_DATASOURCE_URL
        if (springDatasourceUrl != null && !springDatasourceUrl.trim().isEmpty()) {
            String url = springDatasourceUrl.trim();
            if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
                return parseConnectionUrl(url);
            }
            if (url.startsWith("jdbc:postgresql:")) {
                String safeUrl = ensurePostgreSqlSsl(url);
                return new ResolvedDatabaseDetails(safeUrl, dbUsername, dbPassword, "org.postgresql.Driver");
            }
            if (url.startsWith("jdbc:h2:")) {
                return new ResolvedDatabaseDetails(url, dbUsername, dbPassword, "org.h2.Driver");
            }
            if (url.startsWith("jdbc:mysql:")) {
                return new ResolvedDatabaseDetails(url, dbUsername, dbPassword, "com.mysql.cj.jdbc.Driver");
            }
        }

        // 3. Discrete environment variables
        if (dbHost != null && !dbHost.trim().isEmpty()) {
            String host = dbHost.trim();
            String port = (dbPort != null && !dbPort.trim().isEmpty()) ? dbPort.trim() : "";
            String name = (dbName != null && !dbName.trim().isEmpty()) ? dbName.trim() : "postgres";

            boolean isPostgres = port.equals("5432") || port.equals("6543") ||
                    host.contains("supabase") || host.contains("postgres");

            if (isPostgres) {
                if (port.isEmpty()) port = "5432";
                String ssl = (dbSslMode != null && !dbSslMode.trim().isEmpty()) ? dbSslMode.trim() : "require";
                String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s?sslmode=%s", host, port, name, ssl);
                return new ResolvedDatabaseDetails(jdbcUrl, dbUsername, dbPassword, "org.postgresql.Driver");
            } else {
                if (port.isEmpty()) port = "3306";
                String ssl = (dbSslMode != null && !dbSslMode.trim().isEmpty()) ? dbSslMode.trim() : "PREFERRED";
                String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?sslMode=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        host, port, name, ssl);
                return new ResolvedDatabaseDetails(jdbcUrl, dbUsername, dbPassword, "com.mysql.cj.jdbc.Driver");
            }
        }

        // 4. Default fallback (Local MySQL)
        String fallbackUrl = "jdbc:mysql://localhost:3306/internshield?sslMode=PREFERRED&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return new ResolvedDatabaseDetails(fallbackUrl, dbUsername != null ? dbUsername : "root", dbPassword, "com.mysql.cj.jdbc.Driver");
    }

    public ResolvedDatabaseDetails parseConnectionUrl(String rawUrl) {
        try {
            String cleanUrl = rawUrl;
            if (cleanUrl.startsWith("jdbc:")) {
                cleanUrl = cleanUrl.substring(5);
            }

            URI uri = new URI(cleanUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String dbName = (path != null && path.length() > 1) ? path.substring(1) : "postgres";

            String username = dbUsername;
            String password = dbPassword;

            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                username = parts[0];
                if (parts.length > 1) {
                    password = parts[1];
                }
            }

            int finalPort = port > 0 ? port : 5432;
            String query = uri.getQuery();
            if (query == null || query.isEmpty()) {
                query = "sslmode=require";
            } else if (!query.contains("sslmode")) {
                query += "&sslmode=require";
            }

            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s?%s", host, finalPort, dbName, query);
            return new ResolvedDatabaseDetails(jdbcUrl, username, password, "org.postgresql.Driver");
        } catch (Exception e) {
            log.warn("Failed to parse connection URI '{}', wrapping directly: {}", rawUrl, e.getMessage());
            String jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:" + rawUrl;
            return new ResolvedDatabaseDetails(jdbcUrl, dbUsername, dbPassword, "org.postgresql.Driver");
        }
    }

    private String ensurePostgreSqlSsl(String url) {
        if (url.contains("supabase") && !url.contains("sslmode")) {
            return url.contains("?") ? url + "&sslmode=require" : url + "?sslmode=require";
        }
        return url;
    }

    private String maskUrl(String url) {
        if (url == null) return null;
        return url.replaceAll(":[^/@:]+@", ":****@");
    }

    public static class ResolvedDatabaseDetails {
        public final String jdbcUrl;
        public final String username;
        public final String password;
        public final String driverClassName;

        public ResolvedDatabaseDetails(String jdbcUrl, String username, String password, String driverClassName) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
            this.driverClassName = driverClassName;
        }
    }
}
