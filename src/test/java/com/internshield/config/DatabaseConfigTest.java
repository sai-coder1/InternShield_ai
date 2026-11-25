package com.internshield.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseConfigTest {

    @Test
    void testParseSupabasePostgreSqlUriWithCredentials() {
        DatabaseConfig config = new DatabaseConfig();
        String rawUri = "postgresql://postgres.myproject:secretpassword@aws-0-us-east-1.pooler.supabase.com:6543/postgres";

        DatabaseConfig.ResolvedDatabaseDetails details = config.parseConnectionUrl(rawUri);

        assertNotNull(details);
        assertEquals("org.postgresql.Driver", details.driverClassName);
        assertEquals("postgres.myproject", details.username);
        assertEquals("secretpassword", details.password);
        assertTrue(details.jdbcUrl.startsWith("jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres"));
        assertTrue(details.jdbcUrl.contains("sslmode=require"));
    }

    @Test
    void testParseSupabasePostgresDirectUri() {
        DatabaseConfig config = new DatabaseConfig();
        String rawUri = "postgres://postgres:mypassword@db.myproject.supabase.co:5432/postgres?sslmode=require";

        DatabaseConfig.ResolvedDatabaseDetails details = config.parseConnectionUrl(rawUri);

        assertNotNull(details);
        assertEquals("org.postgresql.Driver", details.driverClassName);
        assertEquals("postgres", details.username);
        assertEquals("mypassword", details.password);
        assertEquals("jdbc:postgresql://db.myproject.supabase.co:5432/postgres?sslmode=require", details.jdbcUrl);
    }

    @Test
    void testDiscreteVariablesForSupabasePostgres() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "dbHost", "db.voljvzvcoptqadeeymjj.supabase.co");
        ReflectionTestUtils.setField(config, "dbPort", "5432");
        ReflectionTestUtils.setField(config, "dbName", "postgres");
        ReflectionTestUtils.setField(config, "dbUsername", "postgres");
        ReflectionTestUtils.setField(config, "dbPassword", "secretpass");

        DatabaseConfig.ResolvedDatabaseDetails details = config.resolveDatabaseDetails();

        assertNotNull(details);
        assertEquals("org.postgresql.Driver", details.driverClassName);
        assertEquals("postgres", details.username);
        assertEquals("secretpass", details.password);
        assertEquals("jdbc:postgresql://db.voljvzvcoptqadeeymjj.supabase.co:5432/postgres?sslmode=require", details.jdbcUrl);
    }

    @Test
    void testDiscreteVariablesForMySql() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "dbHost", "localhost");
        ReflectionTestUtils.setField(config, "dbPort", "3306");
        ReflectionTestUtils.setField(config, "dbName", "internshield");
        ReflectionTestUtils.setField(config, "dbUsername", "root");
        ReflectionTestUtils.setField(config, "dbPassword", "localpass");

        DatabaseConfig.ResolvedDatabaseDetails details = config.resolveDatabaseDetails();

        assertNotNull(details);
        assertEquals("com.mysql.cj.jdbc.Driver", details.driverClassName);
        assertEquals("root", details.username);
        assertEquals("localpass", details.password);
        assertTrue(details.jdbcUrl.startsWith("jdbc:mysql://localhost:3306/internshield"));
    }

    @Test
    void testH2UrlPassThroughForTesting() {
        DatabaseConfig config = new DatabaseConfig();
        ReflectionTestUtils.setField(config, "springDatasourceUrl", "jdbc:h2:mem:internshield_test;DB_CLOSE_DELAY=-1");
        ReflectionTestUtils.setField(config, "dbUsername", "sa");
        ReflectionTestUtils.setField(config, "dbPassword", "");

        DatabaseConfig.ResolvedDatabaseDetails details = config.resolveDatabaseDetails();

        assertNotNull(details);
        assertEquals("org.h2.Driver", details.driverClassName);
        assertEquals("sa", details.username);
        assertEquals("", details.password);
        assertEquals("jdbc:h2:mem:internshield_test;DB_CLOSE_DELAY=-1", details.jdbcUrl);
    }
}
