package org.folio.utils;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.idmconnect.Constants.BASE_PATH_CONTRACTS;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import java.util.Map;

public class TestConstants {

  public static final String HOST = "http://localhost";
  public static final String TENANT = "diku";
  public static final Map<String, String> OKAPI_HEADERS = Map.of("x-okapi-tenant", TENANT);
  public static final String IDM_TOKEN = "someToken";
  public static final String PATH_TRANSMIT = "/{id}/transmit";
  public static final String PATH_ID = "/{id}";

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

  private TestConstants() {}
}
