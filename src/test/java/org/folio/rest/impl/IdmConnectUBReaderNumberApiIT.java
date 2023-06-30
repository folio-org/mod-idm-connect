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
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.idmconnect.Constants.BASE_PATH_CONTRACTS;
import static org.folio.idmconnect.Constants.BASE_PATH_READER_NUMDER;
import static org.folio.idmconnect.Constants.MSG_IDM_READER_NUMBER_URL_NOT_SET;
import static org.folio.utils.TestConstants.CONNECTION_REFUSED;
import static org.folio.utils.TestConstants.HOST;
import static org.folio.utils.TestConstants.IDM_TOKEN;
import static org.folio.utils.TestConstants.OKAPI_HEADERS;
import static org.folio.utils.TestConstants.PATH_ID;
import static org.folio.utils.TestConstants.TENANT;
import static org.folio.utils.TestConstants.setupRestAssured;
import static org.hamcrest.Matchers.containsString;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import java.util.Map;
import org.folio.idmconnect.IdmClientConfig;
import org.folio.idmconnect.IdmClientFactory;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.folio.utils.TenantUtil;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class IdmConnectUBReaderNumberApiIT {

  private static final String MOCK_BASE_PATH = "/UBReaderNumber";
  private static final String UNILOGIN_KEY = "unilogin";
  private static final String READER_NUMBER_KEY = "UBReaderNumber";
  private static final String SAMPLE_ID = "465ce0b3-10cd-4da2-8848-db85b63a0a32";
  private static final String SAMPLE_UNILOGIN_VALUE = "mhb76lxa";
  private static final String SAMPLE_LIBRARYCARD_VALUE = "79254581";
  private static final String READER_NUMBER_VALUE = "12345";
  private static final String INVALID_UNILOGIN_VALUE = "7890";
  private static final ResponseDefinition successResponseDefinition =
      ResponseDefinitionBuilder.jsonResponse("{ \"result\": \"success\"}", 200);
  private static final ResponseDefinition failureResponseDefinition =
      ResponseDefinitionBuilder.jsonResponse("{ \"result\": \"failure\"}", 400);

  private static TenantUtil tenantUtil;
  private static String idmApiMockUrl;

  @ClassRule
  public static WireMockRule idmApiMock =
      new WireMockRule(new WireMockConfiguration().dynamicPort(), false);

  @BeforeClass
  public static void beforeClass(TestContext context) {
    Vertx vertx = VertxUtils.getVertxFromContextOrNew();
    int port = NetworkUtils.nextFreePort();
    idmApiMockUrl = idmApiMock.baseUrl() + MOCK_BASE_PATH;
    setupRestAssured(port, BASE_PATH_READER_NUMDER);

    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    PostgresClient.getInstance(vertx);

    tenantUtil =
        new TenantUtil(
            new TenantClient(HOST + ":" + port, TENANT, IDM_TOKEN, WebClient.create(vertx)));

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
                    equalTo(SAMPLE_UNILOGIN_VALUE),
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
            .withQueryParam(UNILOGIN_KEY, equalTo(SAMPLE_UNILOGIN_VALUE))
            .willReturn(like(successResponseDefinition)));
    idmApiMock.stubFor(
        delete(urlPathEqualTo(MOCK_BASE_PATH))
            .withHeader(AUTHORIZATION, equalTo(IDM_TOKEN))
            .withQueryParam(UNILOGIN_KEY, equalTo(INVALID_UNILOGIN_VALUE))
            .willReturn(like(failureResponseDefinition).withStatus(404)));
  }

  @AfterClass
  public static void afterClass() {
    RestAssured.reset();
  }

  @Before
  public void setUp(TestContext context) {
    tenantUtil.setupTenant(true).onComplete(context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    tenantUtil.teardownTenant().onComplete(context.asyncAssertSuccess());
  }

  private void assertThatLibraryCardEquals(String libraryCard) {
    assertThat(
            given()
                .basePath(BASE_PATH_CONTRACTS)
                .pathParam("id", SAMPLE_ID)
                .headers(OKAPI_HEADERS)
                .get(PATH_ID)
                .then()
                .statusCode(200)
                .extract()
                .as(Contract.class)
                .getLibraryCard())
        .isEqualTo(libraryCard);
  }

  @Test
  public void testPostUBReaderNumber() throws InterruptedException {
    IdmClientFactory.setIdmClientConfig(new IdmClientConfig.Builder().build());
    // missing url
    given().post().then().statusCode(500).body(Matchers.equalTo(MSG_IDM_READER_NUMBER_URL_NOT_SET));
    idmApiMock.verify(0, postRequestedFor(urlPathEqualTo(MOCK_BASE_PATH)));
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder().idmReaderNumberUrl(idmApiMockUrl).build());

    // missing token
    given().post().then().statusCode(401);
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder()
            .idmReaderNumberUrl(idmApiMockUrl)
            .idmToken(IDM_TOKEN)
            .build());

    // without query params // failure
    given()
        .post()
        .then()
        .statusCode(failureResponseDefinition.getStatus())
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(failureResponseDefinition.getBody()));
    assertThatLibraryCardEquals(SAMPLE_LIBRARYCARD_VALUE);

    // with query params // success
    given()
        .queryParams(
            Map.of(UNILOGIN_KEY, SAMPLE_UNILOGIN_VALUE, READER_NUMBER_KEY, READER_NUMBER_VALUE))
        .post()
        .then()
        .statusCode(successResponseDefinition.getStatus())
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(successResponseDefinition.getBody()));

    Thread.sleep(1500);
    assertThatLibraryCardEquals(READER_NUMBER_VALUE);
  }

  @Test
  public void testDeleteUBReaderNumber() throws InterruptedException {
    IdmClientFactory.setIdmClientConfig(new IdmClientConfig.Builder().build());
    // missing url
    given()
        .delete()
        .then()
        .statusCode(500)
        .body(Matchers.equalTo(MSG_IDM_READER_NUMBER_URL_NOT_SET));
    idmApiMock.verify(0, deleteRequestedFor(urlPathEqualTo(MOCK_BASE_PATH)));
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder().idmReaderNumberUrl(idmApiMockUrl).build());

    // missing token
    given().delete().then().statusCode(401);
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder()
            .idmReaderNumberUrl(idmApiMockUrl)
            .idmToken(IDM_TOKEN)
            .build());

    // with invalid params
    given()
        .queryParam(UNILOGIN_KEY, INVALID_UNILOGIN_VALUE)
        .delete()
        .then()
        .statusCode(404)
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(failureResponseDefinition.getBody()));
    assertThatLibraryCardEquals(SAMPLE_LIBRARYCARD_VALUE);

    // with query params // success
    given()
        .queryParam(UNILOGIN_KEY, SAMPLE_UNILOGIN_VALUE)
        .delete()
        .then()
        .statusCode(successResponseDefinition.getStatus())
        .contentType(APPLICATION_JSON)
        .body(Matchers.equalTo(successResponseDefinition.getBody()));

    Thread.sleep(1500);
    assertThatLibraryCardEquals(null);
  }

  @Test
  public void testIdmApiNotAvailable() {
    String unvailableUrl = HOST + ":" + NetworkUtils.nextFreePort();
    IdmClientFactory.setIdmClientConfig(
        new IdmClientConfig.Builder().idmReaderNumberUrl(unvailableUrl).build());
    given().post().then().statusCode(500).body(containsString(CONNECTION_REFUSED));
    given().delete().then().statusCode(500).body(containsString(CONNECTION_REFUSED));
  }
}
