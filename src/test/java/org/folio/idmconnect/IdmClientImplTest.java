package org.folio.idmconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.utils.TestConstants.createProxySelector;

import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("IdmClientImpl Unit Tests")
class IdmClientImplTest {

  private static final ProxySelector originalProxySelector = ProxySelector.getDefault();
  private static final String TARGET_URL = "http://localhost:8080";

  @AfterAll
  static void restoreProxySelector() {
    ProxySelector.setDefault(originalProxySelector);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"ht!tp://invalid url with spaces"})
  @DisplayName("Should return empty Optional for invalid URLs")
  void testGetProxyOptionsReturnsEmptyForInvalidUrls(String url) {
    Optional<ProxyOptions> result = IdmClientImpl.getProxyOptions(url);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return empty Optional when no proxy is configured")
  void testGetProxyOptionsWithNoProxy() {
    ProxySelector.setDefault(createProxySelector(Proxy.NO_PROXY));

    Optional<ProxyOptions> result = IdmClientImpl.getProxyOptions(TARGET_URL);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Should return ProxyOptions when HTTP proxy is configured")
  void testGetProxyOptionsWithHttpProxy() {
    Proxy httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));
    ProxySelector.setDefault(createProxySelector(httpProxy));

    Optional<ProxyOptions> result = IdmClientImpl.getProxyOptions(TARGET_URL);

    assertThat(result).isPresent();
    assertThat(result.get().getHost()).isEqualTo("proxy.example.com");
    assertThat(result.get().getPort()).isEqualTo(8080);
    assertThat(result.get().getType()).isEqualTo(ProxyType.HTTP);
  }

  @Test
  @DisplayName("Should return empty Optional when SOCKS proxy is configured")
  void testGetProxyOptionsWithSocksProxy() {
    Proxy socksProxy =
        new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.example.com", 1080));
    ProxySelector.setDefault(createProxySelector(socksProxy));

    Optional<ProxyOptions> result = IdmClientImpl.getProxyOptions(TARGET_URL);

    assertThat(result).isEmpty();
  }
}
