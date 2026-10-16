package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.security.JwtService;
import br.com.emr.emrfinancas.support.LegacyBcryptFixtures;
import br.com.emr.emrfinancas.support.PasswordHttpSupport;
import io.jsonwebtoken.io.Serializer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.ServiceLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class DependencySecurityContractTests extends PasswordHttpSupport {
    @Autowired JwtService jwt;

    @Test
    void usesJackson3ForMvcAndJackson2ForJjwt() throws Exception {
        assertTrue(json.getClass().getName().startsWith("tools.jackson."),
                "MVC must use the approved Jackson 3 family");
        var serializer = ServiceLoader.load(Serializer.class).findFirst().orElseThrow();
        assertEquals("io.jsonwebtoken.jackson.io.JacksonSerializer", serializer.getClass().getName());
        assertTrue(java.util.Arrays.stream(serializer.getClass().getConstructors())
                .flatMap(c -> java.util.Arrays.stream(c.getParameterTypes()))
                .anyMatch(t -> t.getName().equals("com.fasterxml.jackson.databind.ObjectMapper")),
                "JJWT Jackson SPI remains on its Jackson 2 API");
    }

    @Test
    void errorHandlersKeepGeneric401403AndSecurityHeaders() throws Exception {
        var anonymous = mvc.perform(get("/api/auth/me")).andReturn();
        status(anonymous, 401);
        assertEquals("Autenticacao necessaria.", json.readTree(anonymous.getResponse().getContentAsString()).get("message").asText());
        assertEquals("nosniff", anonymous.getResponse().getHeader("X-Content-Type-Options"));
        assertEquals("DENY", anonymous.getResponse().getHeader("X-Frame-Options"));
        var u = owner();
        var token = login(u, LegacyBcryptFixtures.BOUNDARY);
        var denied = mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token)).andReturn();
        status(denied, 403);
        assertEquals("Acesso negado.", json.readTree(denied.getResponse().getContentAsString()).get("message").asText());
        assertEquals("nosniff", denied.getResponse().getHeader("X-Content-Type-Options"));
        assertEquals("DENY", denied.getResponse().getHeader("X-Frame-Options"));
        noSensitiveBody(anonymous, LegacyBcryptFixtures.BOUNDARY);
        noSensitiveBody(denied, token, LegacyBcryptFixtures.BOUNDARY);
    }

    @Test
    void passwordWriteOnlyNeverAppearsInUserJson() throws Exception {
        Usuario u = owner();
        var serialized = json.readTree(json.writeValueAsString(u));
        assertFalse(serialized.has("senha"));
        var token = login(u, LegacyBcryptFixtures.BOUNDARY);
        var response = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andReturn();
        status(response, 200);
        var body = json.readTree(response.getResponse().getContentAsString());
        assertFalse(body.has("senha"));
        assertFalse(response.getResponse().getContentAsString().contains(u.getSenha()));
    }

    @Test
    void jwtClaimsSurviveTwoJacksonFamilies() throws Exception {
        Usuario u = owner();
        String token = login(u, LegacyBcryptFixtures.BOUNDARY);
        assertEquals(u.getEmail(), jwt.parseToken(token).getSubject());
        String[] parts = token.split("\\.");
        var claims = json.readTree(new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8));
        assertEquals(u.getEmail(), claims.get("sub").asText());
        assertEquals(0, claims.get("tokenVersion").asLong());
        assertTrue(claims.get("exp").asLong() > Instant.now().getEpochSecond());
        String tampered = parts[0] + "." + parts[1] + "." + (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);
        var bad = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered)).andReturn();
        status(bad, 401);
        noSensitiveBody(bad, token, tampered);
        u.invalidateTokens(); users.saveAndFlush(u);
        var revoked = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andReturn();
        status(revoked, 401);
        noSensitiveBody(revoked, token);
    }

    @Test
    void financialNumbersDatesAndNullsPreserveApiContract() throws Exception {
        var u = owner(); var token = login(u, LegacyBcryptFixtures.BOUNDARY);
        var body = Map.of("descricao", "Despesa sintetica", "categoria", "Teste", "valor", new BigDecimal("1234.56"), "data", "2026-10-14");
        var created = request("/api/gastos", body, token, "198.51.100.61");
        status(created, 201);
        var result = json.readTree(created.getResponse().getContentAsString());
        assertTrue(result.get("valor").isNumber());
        assertEquals(0, result.get("valor").decimalValue().compareTo(new BigDecimal("1234.56")));
        assertTrue(result.get("data").isTextual());
        assertEquals("2026-10-14", result.get("data").asText());
        assertTrue(result.has("formaPagamento")); assertTrue(result.get("formaPagamento").isNull());
        assertTrue(result.has("observacao")); assertTrue(result.get("observacao").isNull());
        var read = mvc.perform(get("/api/gastos/" + result.get("codigo").asLong()).header("Authorization", "Bearer " + token)).andReturn();
        status(read, 200);
        assertEquals(result, json.readTree(read.getResponse().getContentAsString()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM tb_emr_gasto", Integer.class));
    }

    @Test
    @Timeout(5)
    void numericStringsRejectInvalidConversionWithoutWrites() throws Exception {
        var u = owner(); var token = login(u, LegacyBcryptFixtures.BOUNDARY);
        String invalid = "9".repeat(1199) + "x";
        var result = request("/api/gastos", Map.of("descricao", "Despesa sintetica", "categoria", "Teste", "valor", invalid, "data", "2026-10-14"), token, "198.51.100.61");
        status(result, 400);
        assertEquals("Requisicao invalida", json.readTree(result.getResponse().getContentAsString()).get("message").asText());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tb_emr_gasto", Integer.class));
        unchanged(u, LegacyBcryptFixtures.BOUNDARY_HASH, 0);
        noSensitiveBody(result, invalid, token);
    }

    @Test
    void untypedJsonDoesNotInstantiateAddressClasses() throws Exception {
        Object value = json.readValue("{\"@class\":\"java.net.InetAddress\",\"address\":\"127.0.0.1\"}", Object.class);
        assertInstanceOf(Map.class, value);
        Map<?, ?> map = (Map<?, ?>) value;
        assertEquals("java.net.InetAddress", map.get("@class"));
        assertEquals("127.0.0.1", map.get("address"));
    }
}
