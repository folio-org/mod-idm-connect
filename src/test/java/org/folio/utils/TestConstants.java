package org.folio.utils;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.idmconnect.Constants.BASE_PATH_CONTRACTS;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.folio.rest.RestVerticle;

public class TestConstants {

  public static final String HOST = "http://localhost";
  public static final String TENANT = "diku";
  public static final Map<String, String> OKAPI_HEADERS = Map.of("x-okapi-tenant", TENANT);
  public static final String IDM_TOKEN = "someToken";
  public static final String PATH_TRANSMIT = "/{id}/transmit";
  public static final String PATH_ID = "/{id}";
  public static final String CONNECTION_REFUSED = "Connection refused";

  public static void setupRestAssured(int port) {
    setupRestAssured(port, BASE_PATH_CONTRACTS);
  }

  public static void setupRestAssured(int port, String basePath) {
    RestAssured.reset();
    RestAssured.baseURI = HOST;
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;
    RestAssured.requestSpecification =
        new RequestSpecBuilder()
            .setBasePath(basePath)
            .addHeaders(OKAPI_HEADERS)
            .addHeader(CONTENT_TYPE, APPLICATION_JSON)
            .build();
  }

  public static Future<String> deployRestVerticle(Vertx vertx, int port) {
    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    return vertx.deployVerticle(RestVerticle.class.getName(), options);
  }

  public static ProxySelector createProxySelector(Proxy proxy) {
    return new ProxySelector() {
      @Override
      public List<Proxy> select(URI uri) {
        return List.of(proxy);
      }

      @Override
      public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        // No-op for test
      }
    };
  }

  private TestConstants() {}
}
