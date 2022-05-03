package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.like;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.folio.idmconnect.Constants.BASE_PATH_READER_NUMDER;
import static org.folio.idmconnect.Constants.MSG_IDM_READER_NUMBER_URL_NOT_SET;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_READER_NUMBER_URL;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_TOKEN;
import static org.folio.utils.TestConstants.CONNECTION_REFUSED;
import static org.folio.utils.TestConstants.HOST;
import static org.folio.utils.TestConstants.IDM_TOKEN;
import static org.folio.utils.TestConstants.setupRestAssured;
import static org.hamcrest.Matchers.containsString;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.Map;
import org.folio.rest.RestVerticle;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

@RunWith(VertxUnitRunner.class)
public class IdmConnectUBReaderNumberApiIT {

  private static final String MOCK_BASE_PATH = "/UBReaderNumber";
  private static final String UNILOGIN_KEY = "unilogin";
  private static final String UNILOGIN_VALUE = "abc123de";
  private static final String READER_NUMBER_KEY = "UBReaderNumber";
  private static final String READER_NUMBER_VALUE = "12345";
  private static final String READER_NUMBER_VALUE_INVALID = "7890";
  private static String idmApiMockUrl;

  private static final ResponseDefinition successResponseDefinition =
      ResponseDefinitionBuilder.jsonResponse("{ \"result\": \"success\"}", 200);

  private static final ResponseDefinition failureResponseDefinition =
      ResponseDefinitionBuilder.jsonResponse("{ \"result\": \"failure\"}", 400);

  @ClassRule
  public static WireMockRule idmApiMock =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  @Rule public EnvironmentVariablesRule envs = new EnvironmentVariablesRule();

  @BeforeClass
  public static void beforeClass(TestContext context) {
    Vertx vertx = VertxUtils.getVertxFromContextOrNew();
    int port = NetworkUtils.nextFreePort();
    idmApiMockUrl = idmApiMock.baseUrl() + MOCK_BASE_PATH;
    setupRestAssured(port, BASE_PATH_READER_NUMDER);

    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));
    vertx.deployVerticle(RestVerticle.class.getName(), options, context.asyncAssertSuccess());

    idmApiMock.stubFor(
        any(urlPathEqualTo(MOCK_BASE_PATH))
            .withHeader(AUTHORIZATION, equalTo(IDM_TOKEN))
            .withQueryParams(Map.of(UNILOGIN_KEY, absent(), READER_NUMBER_KEY, absent()))
            .willReturn(like(failureResponseDefinition)));
    idmApiMock.stubFor(
        any(urlPathEqualTo(MOCK_BASE_PATH))
            .withHeader(AUTHORIZATION, equalTo(IDM_TOKEN))
            .withQueryParams(
                Map.of(
                    UNILOGIN_KEY,
                    equalTo(UNILOGIN_VALUE),
                    READER_NUMBER_KEY,
                    equalTo(READER_NUMBER_VALUE)))
            .willReturn(like(successResponseDefinition)));
    idmApiMock.stubFor(
        any(urlPathEqualTo(MOCK_BASE_PATH))
            .withHeader(AUTHORIZATION, absent())
            .willReturn(aResponse().withStatus(401)));
    idmApiMock.stubFor(
        delete(urlPathEqualTo(MOCK_BASE_PATH))
            .withHeader(AUTHORIZATION, equalTo(IDM_TOKEN))
            .withQueryParam(UNILOGIN_KEY, equalTo(UNILOGIN_VALUE))
            .willReturn(like(successResponseDefinition)));
    idmApiMock.stubFor(
        delete(urlPathEqualTo(MOCK_BASE_PATH))
            .withHeader(AUTHORIZATION, equalTo(IDM_TOKEN))
            .withQueryParam(UNILOGIN_KEY, equalTo(READER_NUMBER_VALUE_INVALID))
            .willReturn(like(failureResponseDefinition).withStatus(404)));
  }

  @Test
  public void testPostUBReaderNumber() {
    // missing url
    given().post().then().statusCode(500).body(Matchers.equalTo(MSG_IDM_READER_NUMBER_URL_NOT_SET));
    idmApiMock.verify(0, postRequestedFor(urlPathEqualTo(MOCK_BASE_PATH)));
    envs.set(ENVVAR_IDM_READER_NUMBER_URL, idmApiMockUrl);

    // missing token
    given().post().then().statusCode(401);
    envs.set(ENVVAR_IDM_TOKEN, IDM_TOKEN);

    // with query params // success
    given()
        .queryParams(Map.of(UNILOGIN_KEY, UNILOGIN_VALUE, READER_NUMBER_KEY, READER_NUMBER_VALUE))
        .post()
        .then()
        .statusCode(successResponseDefinition.getStatus())
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(successResponseDefinition.getBody()));

    // without query params // failure
    given()
        .post()
        .then()
        .statusCode(failureResponseDefinition.getStatus())
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(failureResponseDefinition.getBody()));
  }

  @Test

  public void testDeleteUBReaderNumber() {
    // missing url
    given()
        .delete()
        .then()
        .statusCode(500)
        .body(Matchers.equalTo(MSG_IDM_READER_NUMBER_URL_NOT_SET));
    idmApiMock.verify(0, deleteRequestedFor(urlPathEqualTo(MOCK_BASE_PATH)));
    envs.set(ENVVAR_IDM_READER_NUMBER_URL, idmApiMockUrl);

    // missing token
    given().delete().then().statusCode(401);
    envs.set(ENVVAR_IDM_TOKEN, IDM_TOKEN);

    // with query params // success
    given()
        .queryParam(UNILOGIN_KEY, UNILOGIN_VALUE)
        .delete()
        .then()
        .statusCode(successResponseDefinition.getStatus())
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(successResponseDefinition.getBody()));

    // with invalid params
    given()
        .queryParam(UNILOGIN_KEY, READER_NUMBER_VALUE_INVALID)
        .delete()
        .then()
        .statusCode(404)
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(failureResponseDefinition.getBody()));
  }

  @Test
  public void testIdmApiNotAvailable() {
    String unvailableUrl = HOST + ":" + NetworkUtils.nextFreePort();
    envs.set(ENVVAR_IDM_READER_NUMBER_URL, unvailableUrl);
    given().post().then().statusCode(500).body(containsString(CONNECTION_REFUSED));
    given().delete().then().statusCode(500).body(containsString(CONNECTION_REFUSED));
  }
}
