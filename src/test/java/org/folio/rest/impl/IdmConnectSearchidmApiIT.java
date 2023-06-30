package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.idmconnect.Constants.BASE_PATH_SEARCHIDM;
import static org.folio.idmconnect.Constants.MSG_IDM_URL_NOT_SET;
import static org.folio.utils.TestConstants.CONNECTION_REFUSED;
import static org.folio.utils.TestConstants.HOST;
import static org.folio.utils.TestConstants.IDM_TOKEN;
import static org.folio.utils.TestConstants.setupRestAssured;
import static org.hamcrest.Matchers.containsString;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Map;
import org.folio.idmconnect.IdmClientConfig;
import org.folio.idmconnect.IdmClientFactory;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class IdmConnectSearchidmApiIT {

  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static String IDM_MOCK_URL;

  @ClassRule
  public static WireMockRule idmApiMock =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  @BeforeClass
  public static void beforeClass(TestContext context) {
    int port = NetworkUtils.nextFreePort();
    setupRestAssured(port, BASE_PATH_SEARCHIDM);

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
            .withHeader(AUTHORIZATION, absent())
            .willReturn(aResponse().withStatus(401)));
  }

  @AfterClass
  public static void afterClass() {
    RestAssured.reset();
  }

  @Test
  public void testMissingUrl() {
    IdmClientFactory.setIdmClientConfig(new IdmClientConfig.Builder().build());
    assertThat(given().get().then().statusCode(500).extract().body().asString())
        .isEqualTo(MSG_IDM_URL_NOT_SET);
  }

  @Test
  public void testInvalidUrl() {
    IdmClientFactory.setIdmClientConfig(new IdmClientConfig.Builder().idmUrl("").build());
    given().get().then().statusCode(400);
  }

  @Test
  public void testMissingToken() {
    IdmClientFactory.setIdmClientConfig(new IdmClientConfig.Builder().idmUrl(IDM_MOCK_URL).build());
    given().get().then().statusCode(401);
  }

  @Test
  public void testMissingQueryParameters() {
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder().idmUrl(IDM_MOCK_URL).idmToken(IDM_TOKEN).build());
    given().get().then().statusCode(400);
  }

  @Test
  public void testRequestOk() {
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder().idmUrl(IDM_MOCK_URL).idmToken(IDM_TOKEN).build());
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

  @Test
  public void testIdmApiNotAvailable() {
    String unavailableUrl = HOST + ":" + NetworkUtils.nextFreePort();
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder().idmUrl(unavailableUrl).build());
    given().get().then().statusCode(500).body(containsString(CONNECTION_REFUSED));
  }
}
