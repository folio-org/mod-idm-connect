package org.folio.idmconnect;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.net.Proxy.Type.HTTP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.utils.TestConstants.createProxySelector;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

@ExtendWith(VertxExtension.class)
@DisplayName("IdmClientImpl Integration Tests")
class IdmClientImplIT {

  private static final String TEST_GIVEN_NAME = "John";
  private static final String TEST_SURNAME = "Doe";
  private static final String TEST_DATE_OF_BIRTH = "1990-01-15";
  private static final String TEST_DATE_OF_BIRTH_FORMATTED = "19900115";

  @RegisterExtension
  static WireMockExtension targetServer =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @RegisterExtension
  static WireMockExtension proxyServer =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private static final ProxySelector originalProxySelector = ProxySelector.getDefault();
  private static WebClient webClient;
  private static IdmClient idmClient;

  @BeforeAll
  static void beforeAll(Vertx vertx) {
    webClient = WebClient.create(vertx, new WebClientOptions());
    IdmClientConfig config =
        new IdmClientConfig.Builder()
            .idmUrl("http://localhost:" + targetServer.getPort() + "/search")
            .idmToken("test-token")
            .build();
    idmClient = new IdmClientImpl(config, webClient);
  }

  @AfterAll
  static void afterAll() {
    ProxySelector.setDefault(originalProxySelector);
    if (webClient != null) {
      webClient.close();
    }
  }

  @BeforeEach
  void setup() {
    targetServer.stubFor(get(urlPathEqualTo("/search")).willReturn(aResponse().withStatus(200)));
    proxyServer.stubFor(
        get(urlMatching(".*"))
            .willReturn(aResponse().proxiedFrom("http://localhost:" + targetServer.getPort())));
  }

  @Test
  @DisplayName("Should route requests through proxy when configured")
  void proxyIsUsedWhenConfigured() throws Exception {
    Proxy proxy = new Proxy(HTTP, new InetSocketAddress("localhost", proxyServer.getPort()));
    ProxySelector.setDefault(createProxySelector(proxy));

    Response response = makeSearchRequest();

    assertThat(response.getStatus()).isEqualTo(200);
    verifySearchRequest(proxyServer);
    verifySearchRequest(targetServer);
  }

  @Test
  @DisplayName("Should connect directly when no proxy is configured")
  void noProxyIsUsedWhenNotConfigured() throws Exception {
    ProxySelector.setDefault(createProxySelector(Proxy.NO_PROXY));

    Response response = makeSearchRequest();

    assertThat(response.getStatus()).isEqualTo(200);
    verifySearchRequest(targetServer);
    assertThat(proxyServer.getAllServeEvents()).isEmpty();
  }

  private Response makeSearchRequest() throws Exception {
    return idmClient
        .search(TEST_GIVEN_NAME, TEST_SURNAME, TEST_DATE_OF_BIRTH)
        .toCompletionStage()
        .toCompletableFuture()
        .get();
  }

  private void verifySearchRequest(WireMockExtension server) {
    server.verify(
        getRequestedFor(urlPathEqualTo("/search"))
            .withQueryParam("givenname", equalTo(TEST_GIVEN_NAME))
            .withQueryParam("surname", equalTo(TEST_SURNAME))
            .withQueryParam("date_of_birth", equalTo(TEST_DATE_OF_BIRTH_FORMATTED)));
  }
}
