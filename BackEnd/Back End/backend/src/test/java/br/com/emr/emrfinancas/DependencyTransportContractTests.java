package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.support.PasswordHttpSupport;
import br.com.emr.emrfinancas.support.BoundedHttpExchange;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.apache.coyote.AbstractProtocol;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
        "server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:task4_transport;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "app.security.rate-limit.exempt-local=false"})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class DependencyTransportContractTests extends PasswordHttpSupport {
    @LocalServerPort int port;
    @Autowired ServletWebServerApplicationContext context;

    @ParameterizedTest
    @ValueSource(strings={"request_line", "header_name", "conflicting_length_and_transfer_encoding"})
    void malformedHttpIsRejectedBeforeApplicationWrites(String scenario) throws Exception {
        var tomcat = ((TomcatWebServer) context.getWebServer()).getTomcat();
        var protocol = (AbstractProtocol<?>) tomcat.getConnector().getProtocolHandler();
        assertNotNull(protocol.getAddress());
        assertTrue(protocol.getAddress().isLoopbackAddress());
        assertEquals(List.of("test"), List.of(context.getEnvironment().getActiveProfiles()));
        int initialUsers = jdbc.queryForObject("SELECT COUNT(*) FROM tb_emr_usuario", Integer.class);
        int initialExpenses = jdbc.queryForObject("SELECT COUNT(*) FROM tb_emr_gasto", Integer.class);
        String request = switch(scenario) {
            case "request_line" -> "GE(T /health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n";
            case "header_name" -> "GET /health HTTP/1.1\r\nHost: localhost\r\nBad Header: x\r\nConnection: close\r\n\r\n";
            default -> "POST /api/gastos HTTP/1.1\r\nHost: localhost\r\nContent-Length: 1\r\nContent-Length: 2\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n0\r\n\r\n";
        };
        String marker = "synthetic-task4-sensitive-marker";
        request = request.replace("Host: localhost\r\n", "Host: localhost\r\nX-Task4-Probe: " + marker + "\r\n");
        byte[] bytes = request.getBytes(StandardCharsets.US_ASCII);
        assertTrue(bytes.length <= 8192);
        List<String> observations = new ArrayList<>();
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        for(int sample=0; sample<3; sample++) {
            var exchange = BoundedHttpExchange.read(port, request, 8192, Duration.ofSeconds(5));
            String firstLine = exchange.firstLine();
            assertTrue(firstLine.isEmpty() || firstLine.matches("HTTP/1\\.[01] 400 .*"),
                    "malformed framing must be rejected or connection closed before handler");
            assertTrue(exchange.termination().equals("remote_eof") || exchange.termination().equals("remote_reset"));
            exchange.requireNoMarkers(List.of(marker, br.com.emr.emrfinancas.support.LegacyBcryptFixtures.BOUNDARY,
                    br.com.emr.emrfinancas.support.LegacyBcryptFixtures.BOUNDARY_HASH));
            assertEquals(initialUsers, jdbc.queryForObject("SELECT COUNT(*) FROM tb_emr_usuario", Integer.class));
            assertEquals(initialExpenses, jdbc.queryForObject("SELECT COUNT(*) FROM tb_emr_gasto", Integer.class));
            {
                var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/health"))
                        .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
            }
            observations.add(scenario+","+sample+","+firstLine+","+bytes.length+","+exchange.responseBytes()
                    +","+exchange.termination()+","+exchange.elapsedMillis()+",no_sensitive_marker,health_200,no_writes");
        }
        String evidence = System.getenv("EMR_SECURITY_EVIDENCE_DIRECTORY");
        Path output = evidence==null ? Path.of("target","transport-evidence") : Path.of(evidence);
        Files.createDirectories(output);
        Files.write(output.resolve("transport-"+scenario+".csv"), observations, StandardCharsets.UTF_8);
        List<String> connectors = new ArrayList<>();
        for(var connector : tomcat.getService().findConnectors()) {
            connectors.add("protocol="+connector.getProtocol()+",handler="+connector.getProtocolHandler().getClass().getName()
                    +",ssl="+connector.getSecure()+",upgrades="+connector.findUpgradeProtocols().length);
        }
        connectors.add("servlets="+context.getServletContext().getServletRegistrations().keySet());
        connectors.add("TLS/HTTP2/AJP/WebSocket client/proxy not exercised; H2 fixture does not validate PostgreSQL");
        Files.write(output.resolve("connector-inventory.txt"), connectors, StandardCharsets.UTF_8);
    }
}
