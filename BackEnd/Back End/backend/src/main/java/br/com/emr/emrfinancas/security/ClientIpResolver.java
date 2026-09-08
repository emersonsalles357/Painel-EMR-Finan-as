package br.com.emr.emrfinancas.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ClientIpResolver {
    private static final String UNKNOWN_CLIENT = "unknown";

    private final String trustedHeader;

    public ClientIpResolver(@Value("${app.security.client-ip.trusted-header:}") String trustedHeader) {
        this.trustedHeader = trustedHeader == null ? "" : trustedHeader.trim();
    }

    public String resolve(HttpServletRequest request) {
        if (!trustedHeader.isEmpty()) {
            String forwardedAddress = normalizeIpLiteral(request.getHeader(trustedHeader));
            if (forwardedAddress != null) {
                return forwardedAddress;
            }
        }

        String remoteAddress = normalizeIpLiteral(request.getRemoteAddr());
        return remoteAddress != null ? remoteAddress : UNKNOWN_CLIENT;
    }

    private String normalizeIpLiteral(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String candidate = rawValue.trim();
        if (candidate.isEmpty() || candidate.length() > 45 || candidate.contains(",")) {
            return null;
        }

        if (candidate.indexOf(':') >= 0) {
            return normalizeIpv6(candidate);
        }
        return normalizeIpv4(candidate);
    }

    private String normalizeIpv4(String candidate) {
        String[] octets = candidate.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }

        StringBuilder normalized = new StringBuilder();
        for (int index = 0; index < octets.length; index++) {
            String octet = octets[index];
            if (octet.isEmpty() || octet.length() > 3 || !octet.chars().allMatch(Character::isDigit)) {
                return null;
            }
            int value;
            try {
                value = Integer.parseInt(octet);
            } catch (NumberFormatException exception) {
                return null;
            }
            if (value > 255) {
                return null;
            }
            if (index > 0) {
                normalized.append('.');
            }
            normalized.append(value);
        }
        return normalized.toString();
    }

    private String normalizeIpv6(String candidate) {
        if (!candidate.matches("[0-9A-Fa-f:.]+")) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(candidate);
            return address instanceof Inet6Address ? address.getHostAddress() : null;
        } catch (UnknownHostException exception) {
            return null;
        }
    }
}
