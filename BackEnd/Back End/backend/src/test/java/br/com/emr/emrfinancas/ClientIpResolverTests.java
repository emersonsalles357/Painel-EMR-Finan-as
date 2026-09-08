package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.security.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTests {
    @Test
    void productionUsesRenderHeaderInsteadOfProxyAddress() {
        ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");
        MockHttpServletRequest request = requestFrom("10.0.0.8");
        request.addHeader("CF-Connecting-IP", "203.0.113.24");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.24");
    }

    @Test
    void ignoresSpoofableForwardedHeaders() {
        ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");
        MockHttpServletRequest request = requestFrom("10.0.0.8");
        request.addHeader("X-Forwarded-For", "203.0.113.99");
        request.addHeader("Forwarded", "for=203.0.113.99");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.8");
    }

    @Test
    void malformedOrChainedTrustedHeaderFallsBackToSocketPeer() {
        ClientIpResolver resolver = new ClientIpResolver("CF-Connecting-IP");
        MockHttpServletRequest request = requestFrom("10.0.0.8");
        request.addHeader("CF-Connecting-IP", "203.0.113.24, 198.51.100.2");

        assertThat(resolver.resolve(request)).isEqualTo("10.0.0.8");
    }

    @Test
    void environmentsWithoutTrustedEdgeIgnoreInjectedCloudflareHeader() {
        ClientIpResolver resolver = new ClientIpResolver("");
        MockHttpServletRequest request = requestFrom("198.51.100.80");
        request.addHeader("CF-Connecting-IP", "203.0.113.24");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.80");
    }

    private MockHttpServletRequest requestFrom(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
