package org.folio.rest.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.idmconnect.Constants.ENVVAR_IDM_CONTRACT_URL;
import static org.folio.utils.TestConstants.HOST;
import static org.folio.utils.TestConstants.IDM_TOKEN;
import static org.folio.utils.TestConstants.PATH_ID;
import static org.folio.utils.TestConstants.PATH_TRANSMIT;
import static org.folio.utils.TestConstants.TENANT;
import static org.folio.utils.TestConstants.setupRestAssured;
import static org.folio.utils.TestEntities.DRAFT;
import static org.folio.utils.TestEntities.PENDING;
import static org.folio.utils.TestEntities.PENDING_EDIT;
import static org.folio.utils.TestEntities.TRANSMISSION_ERROR;
import static org.folio.utils.TestEntities.TRANSMISSION_ERROR_EDIT;
import static org.folio.utils.TestEntities.UPDATED;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.folio.utils.TenantUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

@RunWith(VertxUnitRunner.class)
public class IdmConnectContractApiTransmitIT {

  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static TenantUtil tenantUtil;

  @ClassRule public static EnvironmentVariablesRule envs = new EnvironmentVariablesRule();

  @ClassRule
  public static WireMockRule idmApiMock =
      new WireMockRule(new WireMockConfiguration().dynamicPort());

  @BeforeClass
  public static void beforeClass(TestContext context) {
    int port = NetworkUtils.nextFreePort();
    setupRestAssured(port);

    tenantUtil =
        new TenantUtil(
            new TenantClient(HOST + ":" + port, TENANT, IDM_TOKEN, WebClient.create(vertx)));
    PostgresClient.setPostgresTester(new PostgresTesterContainer());

    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));

    vertx.deployVerticle(RestVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @AfterClass
  public static void afterClass() {
    RestAssured.reset();
  }

  @Before
  public void setUp(TestContext context) {
    tenantUtil.setupTenant(true).onComplete(context.asyncAssertSuccess());
    idmApiMock.resetAll();
    envs.set(ENVVAR_IDM_CONTRACT_URL, idmApiMock.baseUrl());
  }

  @After
  public void tearDown(TestContext context) {
    tenantUtil.teardownTenant().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testStatusUpdateOnSuccessfulTransmit() {
    int expectedStatusCode = 200;
    stubFor(put(urlEqualTo("/")).willReturn(aResponse().withStatus(expectedStatusCode)));
    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(expectedStatusCode)));
    RequestPatternBuilder postRequestedFor = postRequestedFor(urlEqualTo("/"));
    RequestPatternBuilder putRequestedFor = putRequestedFor(urlEqualTo("/"));

    // draft
    given().get(PATH_TRANSMIT, DRAFT.getId()).then().statusCode(expectedStatusCode);
    verify(1, postRequestedFor);
    assertThat(given().get(PATH_ID, DRAFT.getId()).as(Contract.class).getStatus())
        .isEqualTo(DRAFT.getSucceededStatus());
    // updated
    given().get(PATH_TRANSMIT, UPDATED.getId()).then().statusCode(expectedStatusCode);
    verify(1, putRequestedFor);
    assertThat(given().get(PATH_ID, UPDATED.getId()).as(Contract.class).getStatus())
        .isEqualTo(UPDATED.getSucceededStatus());
    // pending
    given().get(PATH_TRANSMIT, PENDING.getId()).then().statusCode(expectedStatusCode);
    verify(2, putRequestedFor);
    assertThat(given().get(PATH_ID, PENDING.getId()).as(Contract.class).getStatus())
        .isEqualTo(PENDING.getSucceededStatus());
    // pending_edit
    given().get(PATH_TRANSMIT, PENDING_EDIT.getId()).then().statusCode(expectedStatusCode);
    verify(3, putRequestedFor);
    assertThat(given().get(PATH_ID, PENDING_EDIT.getId()).as(Contract.class).getStatus())
        .isEqualTo(PENDING_EDIT.getSucceededStatus());
    // transmission_error
    given().get(PATH_TRANSMIT, TRANSMISSION_ERROR.getId()).then().statusCode(expectedStatusCode);
    verify(2, postRequestedFor);
    assertThat(given().get(PATH_ID, TRANSMISSION_ERROR.getId()).as(Contract.class).getStatus())
        .isEqualTo(TRANSMISSION_ERROR.getSucceededStatus());
    // transmission_error_edit
    given()
        .get(PATH_TRANSMIT, TRANSMISSION_ERROR_EDIT.getId())
        .then()
        .statusCode(expectedStatusCode);
    verify(4, putRequestedFor);
    assertThat(given().get(PATH_ID, TRANSMISSION_ERROR_EDIT.getId()).as(Contract.class).getStatus())
        .isEqualTo(TRANSMISSION_ERROR_EDIT.getSucceededStatus());
  }

  private void assertSampleDataOnFailedTransmit(int expectedStatusCode) {
    RequestPatternBuilder postRequestedFor = postRequestedFor(urlEqualTo("/"));
    RequestPatternBuilder putRequestedFor = putRequestedFor(urlEqualTo("/"));

    // draft
    given().get(PATH_TRANSMIT, DRAFT.getId()).then().statusCode(expectedStatusCode);
    verify(1, postRequestedFor);
    assertThat(given().get(PATH_ID, DRAFT.getId()).as(Contract.class).getStatus())
        .isEqualTo(DRAFT.getFailedStatus());
    // updated
    given().get(PATH_TRANSMIT, UPDATED.getId()).then().statusCode(expectedStatusCode);
    verify(1, putRequestedFor);
    assertThat(given().get(PATH_ID, UPDATED.getId()).as(Contract.class).getStatus())
        .isEqualTo(UPDATED.getFailedStatus());
    // pending
    given().get(PATH_TRANSMIT, PENDING.getId()).then().statusCode(expectedStatusCode);
    verify(2, putRequestedFor);
    assertThat(given().get(PATH_ID, PENDING.getId()).as(Contract.class).getStatus())
        .isEqualTo(PENDING.getFailedStatus());
    // pending_edit
    given().get(PATH_TRANSMIT, PENDING_EDIT.getId()).then().statusCode(expectedStatusCode);
    verify(3, putRequestedFor);
    assertThat(given().get(PATH_ID, PENDING_EDIT.getId()).as(Contract.class).getStatus())
        .isEqualTo(PENDING_EDIT.getFailedStatus());
    // transmission_error
    given().get(PATH_TRANSMIT, TRANSMISSION_ERROR.getId()).then().statusCode(expectedStatusCode);
    verify(2, postRequestedFor);
    assertThat(given().get(PATH_ID, TRANSMISSION_ERROR.getId()).as(Contract.class).getStatus())
        .isEqualTo(TRANSMISSION_ERROR.getFailedStatus());
    // transmission_error_edit
    given()
        .get(PATH_TRANSMIT, TRANSMISSION_ERROR_EDIT.getId())
        .then()
        .statusCode(expectedStatusCode);
    verify(4, putRequestedFor);
    assertThat(given().get(PATH_ID, TRANSMISSION_ERROR_EDIT.getId()).as(Contract.class).getStatus())
        .isEqualTo(TRANSMISSION_ERROR_EDIT.getFailedStatus());
  }

  @Test
  public void testStatusUpdateOnFailedTransmit404() {
    stubFor(put(urlEqualTo("/")).willReturn(aResponse().withStatus(404)));
    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(404)));
    assertSampleDataOnFailedTransmit(404);
  }

  @Test
  public void testStatusUpdateOnFailedTransmit500() {
    stubFor(put(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    stubFor(
        post(urlEqualTo("/")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    assertSampleDataOnFailedTransmit(500);
  }

  @Test
  public void testStatusUpdateOnFailedTransmitNoEnvVar() {
    envs.set(ENVVAR_IDM_CONTRACT_URL, null);
    assertThat(System.getenv(ENVVAR_IDM_CONTRACT_URL)).isNull();
    int expectedStatusCode = 500;

    given().get(PATH_TRANSMIT, DRAFT.getId()).then().statusCode(expectedStatusCode);
    assertThat(given().get(PATH_ID, DRAFT.getId()).as(Contract.class).getStatus())
        .isEqualTo(DRAFT.getFailedStatus());
    given().get(PATH_TRANSMIT, UPDATED.getId()).then().statusCode(expectedStatusCode);
    assertThat(given().get(PATH_ID, UPDATED.getId()).as(Contract.class).getStatus())
        .isEqualTo(UPDATED.getFailedStatus());
  }
}
