package br.com.emr.emrfinancas.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Reject invalid settings before datasource/Flyway beans can touch the database. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionConfigGuard {
    @Bean
    static BeanFactoryPostProcessor productionSettings(Environment env) {
        return factory -> validate(env);
    }

    public static void validate(Environment env) {
        require(!Arrays.asList(env.getActiveProfiles()).contains("dev")
                && !Arrays.asList(env.getActiveProfiles()).contains("test"), "profiles");
        String url = required(env, "spring.datasource.url");
        // JDBC query parameters override driver properties; keep TLS and credentials separate.
        require(url.startsWith("jdbc:postgresql://") && !url.contains("?") && !url.contains("#"), "DB_URL");
        URI database;
        try { database = URI.create(url.substring(5)); }
        catch (IllegalArgumentException ex) { throw invalid("DB_URL"); }
        require(database.getHost() != null && database.getHost().endsWith(".neon.tech")
                && database.getUserInfo() == null && database.getPath() != null
                && database.getPath().length() > 1, "DB_URL");
        required(env, "spring.datasource.username");
        required(env, "spring.datasource.password");
        equal(env, "spring.datasource.driver-class-name", "org.postgresql.Driver");
        equal(env, "spring.datasource.hikari.data-source-properties.sslmode", "verify-full");
        equal(env, "spring.datasource.hikari.data-source-properties.sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        equal(env, "spring.jpa.hibernate.ddl-auto", "validate");
        equal(env, "spring.flyway.enabled", "true");
        equal(env, "spring.flyway.baseline-on-migrate", "false");
        equal(env, "spring.flyway.clean-disabled", "true");
        equal(env, "app.dev.seed-user.enabled", "false");
        equal(env, "app.security.rate-limit.exempt-local", "false");
        equal(env, "server.forward-headers-strategy", "none");
        origin(required(env, "app.frontend.url"));
        for (String value : required(env, "app.cors.allowed-origins").split(",", -1)) origin(value.trim());
        String secret = required(env, "app.jwt.secret");
        require(secret.getBytes(StandardCharsets.UTF_8).length >= 32 && secret.chars().distinct().count() >= 16, "JWT_SECRET");
        try {
            long lifetime = Long.parseLong(required(env, "app.jwt.expiration-ms"));
            require(lifetime > 0 && lifetime <= 3600000, "JWT_EXPIRATION");
            int port = Integer.parseInt(required(env, "spring.mail.port"));
            require(port > 0 && port <= 65535, "MAIL_PORT");
        } catch (NumberFormatException ex) { throw invalid("JWT_EXPIRATION / MAIL_PORT"); }
        required(env, "spring.mail.host");
        required(env, "spring.mail.username");
        required(env, "spring.mail.password");
        String from = required(env, "app.mail.from");
        require(from.matches("[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+"), "MAIL_FROM");
        equal(env, "spring.mail.properties.mail.smtp.auth", "true");
        equal(env, "spring.mail.properties.mail.smtp.starttls.enable", "true");
        equal(env, "spring.mail.properties.mail.smtp.starttls.required", "true");
        equal(env, "spring.mail.properties.mail.smtp.ssl.checkserveridentity", "true");
    }

    private static void origin(String value) {
        URI uri;
        try { uri = URI.create(value); }
        catch (IllegalArgumentException ex) { throw invalid("FRONTEND_URL / CORS_ALLOWED_ORIGINS"); }
        require("https".equals(uri.getScheme()) && uri.getHost() != null
                && !uri.getHost().equalsIgnoreCase("localhost") && !uri.getHost().startsWith("127.")
                && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                && uri.getPath().isEmpty(), "FRONTEND_URL / CORS_ALLOWED_ORIGINS");
    }

    private static String required(Environment env, String key) {
        String value;
        try { value = env.getProperty(key); }
        catch (IllegalArgumentException ex) { throw invalid(key); }
        require(value != null && !value.isBlank() && !value.contains("${"), key);
        return value;
    }

    private static void equal(Environment env, String key, String expected) {
        require(expected.equals(required(env, key)), key);
    }

    private static void require(boolean condition, String key) {
        if (!condition) throw invalid(key);
    }

    private static IllegalStateException invalid(String key) {
        return new IllegalStateException("Configuracao de producao invalida: " + key);
    }
}
