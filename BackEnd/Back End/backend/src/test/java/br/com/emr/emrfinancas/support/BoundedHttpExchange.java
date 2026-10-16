package br.com.emr.emrfinancas.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test-only loopback transport; never used by application classes. */
public final class BoundedHttpExchange {
    private BoundedHttpExchange() {}
    public record Result(String response, int responseBytes, String termination, long elapsedMillis) {
        public String firstLine() { return response.split("\r\n", 2)[0]; }
        public void requireNoMarkers(List<String> markers) {
            if(markers.stream().anyMatch(response::contains)) throw new IllegalStateException("synthetic marker echoed");
        }
    }
    public static Result read(int port, String request, int byteLimit, Duration deadline) throws IOException {
        long started = System.nanoTime();
        byte[] bytes = request.getBytes(StandardCharsets.US_ASCII);
        if(bytes.length > 8192 || byteLimit < 1 || deadline.isNegative() || deadline.isZero()) throw new IllegalArgumentException("invalid bound");
        long budget = deadline.toNanos();
        var closedByDeadline = new AtomicBoolean();
        var watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "test-loopback-deadline");
            thread.setDaemon(true);
            return thread;
        });
        try(Socket socket = new Socket()) {
            var timeout = watchdog.schedule(() -> {
                closedByDeadline.set(true);
                try { socket.close(); } catch(IOException ignored) { /* Closing test socket only. */ }
            }, Math.max(1, budget-(System.nanoTime()-started)), TimeUnit.NANOSECONDS);
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", port), remainingMillis(started, budget));
                socket.getOutputStream().write(bytes);
                socket.getOutputStream().flush();
                var response = new ByteArrayOutputStream();
                String termination = "remote_eof";
                try {
                    while(true) {
                        socket.setSoTimeout(remainingMillis(started, budget));
                        int next = socket.getInputStream().read();
                        if(next == -1) break;
                        if(response.size() == byteLimit) throw new IOException("bounded response exceeded");
                        response.write(next);
                    }
                } catch(SocketTimeoutException timedOut) {
                    throw timedOut;
                } catch(SocketException reset) {
                    if(closedByDeadline.get() || System.nanoTime()-started >= budget)
                        throw new SocketTimeoutException("total loopback deadline exceeded");
                    termination = "remote_reset";
                }
                remainingMillis(started, budget);
                return new Result(response.toString(StandardCharsets.UTF_8), response.size(),
                        termination, (System.nanoTime()-started)/1_000_000L);
            } finally { timeout.cancel(false); }
        } finally { watchdog.shutdownNow(); }
    }
    private static int remainingMillis(long started, long budget) throws SocketTimeoutException {
        long remaining = budget-(System.nanoTime()-started);
        if(remaining <= 0) throw new SocketTimeoutException("total loopback deadline exceeded");
        return (int)Math.min(Integer.MAX_VALUE, Math.max(1, (remaining+999_999)/1_000_000));
    }
}
