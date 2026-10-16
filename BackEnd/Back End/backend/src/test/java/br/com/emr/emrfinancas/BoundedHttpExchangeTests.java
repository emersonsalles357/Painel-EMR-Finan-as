package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.support.BoundedHttpExchange;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class BoundedHttpExchangeTests {
    @FunctionalInterface interface Peer { void send(Socket socket) throws Exception; }
    private static final class Fixture implements AutoCloseable {
        final ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        final Future<?> peer;
        final AtomicBoolean closing = new AtomicBoolean();
        Fixture(Peer handler) throws IOException {
            peer = worker.submit(() -> {
                try(Socket accepted = server.accept()) { handler.send(accepted); }
                catch(Exception ex) { if(!closing.get()) throw new CompletionException(ex); }
            });
        }
        int port() { return server.getLocalPort(); }
        public void close() throws Exception {
            closing.set(true); server.close(); worker.shutdownNow();
            assertTrue(worker.awaitTermination(2, TimeUnit.SECONDS), "synthetic peer terminated");
        }
    }
    @Test
    void readsFullBodyAndDetectsSyntheticEcho() throws Exception {
        String marker = "synthetic-task4-sensitive-marker";
        String reply = "HTTP/1.1 400 Bad Request\r\nConnection: close\r\n\r\n" + marker;
        try(var fixture = new Fixture(socket -> {
            socket.getOutputStream().write(reply.getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
        })) {
            var result = BoundedHttpExchange.read(fixture.port(), "GET / HTTP/1.1\r\n\r\n", 8192, Duration.ofSeconds(2));
            assertEquals(reply.getBytes(StandardCharsets.US_ASCII).length, result.responseBytes());
            assertThrows(IllegalStateException.class, () -> result.requireNoMarkers(List.of(marker)));
        }
    }
    @Test
    void recordsRemoteEofAfterCompleteResponse() throws Exception {
        try(var fixture = new Fixture(socket -> {
            socket.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n\r\nsafe".getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
        })) {
            var result = BoundedHttpExchange.read(fixture.port(), "GET / HTTP/1.1\r\n\r\n", 8192, Duration.ofSeconds(2));
            assertEquals("remote_eof", result.termination());
            assertTrue(result.response().endsWith("safe"));
        }
    }
    @Test
    void totalDeadlineRejectsProgressiveHeaders() throws Exception {
        try(var fixture = new Fixture(socket -> {
            byte[] response = "HTTP/1.1 400 Bad Request\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
            for(byte value : response) {
                socket.getOutputStream().write(value); socket.getOutputStream().flush();
                Thread.sleep(25);
            }
        })) {
            assertThrows(SocketTimeoutException.class, () ->
                    BoundedHttpExchange.read(fixture.port(), "GET / HTTP/1.1\r\n\r\n", 8192, Duration.ofMillis(150)));
        }
    }
    @Test
    void responseByteLimitRejectsExcessBody() throws Exception {
        try(var fixture = new Fixture(socket -> {
            socket.getOutputStream().write(("HTTP/1.1 400 Bad Request\r\n\r\n"+"x".repeat(500)).getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
        })) {
            assertThrows(IOException.class, () ->
                    BoundedHttpExchange.read(fixture.port(), "GET / HTTP/1.1\r\n\r\n", 128, Duration.ofSeconds(2)));
        }
    }
}
