package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.rest.impl.Constants.BASE_PATH_SEARCHIDM;
import static org.folio.rest.impl.Constants.MSG_IDM_URL_NOT_SET;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Map;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

@RunWith(VertxUnitRunner.class)
public class IdmConnectSearchidmApiIT {

  @ClassRule
  public static WireMockRule idmApiMock =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  @Rule public EnvironmentVariablesRule envs = new EnvironmentVariablesRule();

  private static final String HOST = "http://localhost";
  private static final String TENANT = "diku";
  private static final Map<String, String> OKAPI_HEADERS = Map.of("x-okapi-tenant", TENANT);
  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static final String IDM_TOKEN = "someToken";
  private static String IDM_MOCK_URL;

  @BeforeClass
  public static void beforeClass(TestContext context) {
    int port = NetworkUtils.nextFreePort();
    RestAssured.reset();
    RestAssured.defaultParser = Parser.JSON;
    RestAssured.requestSpecification =
        new RequestSpecBuilder()
            .setBaseUri(HOST)
            .setPort(port)
            .setBasePath(BASE_PATH_SEARCHIDM)
            .addHeaders(OKAPI_HEADERS)
            .build();

    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, context.asyncAssertSuccess());

    IDM_MOCK_URL = idmApiMock.baseUrl() + BASE_PATH_SEARCHIDM;
    idmApiMock.stubFor(
        get(urlPathEqualTo(BASE_PATH_SEARCHIDM))
            .withQueryParams(
                Map.of("givenname", absent(), "surname", absent(), "date_of_birth", absent()))
            .willReturn(aResponse().withStatus(400)));
    idmApiMock.stubFor(
        get(urlPathEqualTo(BASE_PATH_SEARCHIDM))
            .withQueryParams(
                Map.of(
                    "givenname",
                    equalTo("John"),
                    "surname",
                    equalTo("Doe"),
                    "date_of_birth",
                    equalTo("19981224")))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("[]")));
    idmApiMock.stubFor(
        get(urlPathEqualTo(BASE_PATH_SEARCHIDM))
            .withHeader("Authorization", absent())
            .willReturn(aResponse().withStatus(401)));
  }

  @AfterClass
  public static void afterClass() {
    RestAssured.reset();
  }

  @Test
  public void testMissingUrl() {
    assertThat(System.getenv("IDM_URL")).isNull();
    assertThat(given().get().then().statusCode(500).extract().body().asString())
        .isEqualTo(MSG_IDM_URL_NOT_SET);
  }

  @Test
  public void testInvalidUrl() {
    envs.set("IDM_URL", "");
    assertThat(System.getenv("IDM_URL")).isNotNull();
    given().get().then().statusCode(400);
  }

  @Test
  public void testMissingToken() {
    envs.set("IDM_URL", IDM_MOCK_URL);
    assertThat(System.getenv("IDM_TOKEN")).isNull();
    given().get().then().statusCode(401);
  }

  @Test
  public void testMissingQueryParameters() {
    envs.set("IDM_URL", IDM_MOCK_URL);
    envs.set("IDM_TOKEN", IDM_TOKEN);
    given().get().then().statusCode(400);
  }

  @Test
  public void testRequestOk() {
    envs.set("IDM_URL", IDM_MOCK_URL);
    envs.set("IDM_TOKEN", IDM_TOKEN);
    assertThat(
            given()
                .queryParams(
                    Map.of("firstName", "John", "lastName", "Doe", "dateOfBirth", "1998-12-24"))
                .get()
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .asString())
        .isEqualTo("[]");
  }
}
