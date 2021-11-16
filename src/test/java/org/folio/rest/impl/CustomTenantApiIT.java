package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.rest.impl.Constants.BASE_PATH_CONTRACTS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.JacksonCodec;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.Contracts;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CustomTenantApiIT {

  private static final String HOST = "http://localhost";
  private static final String TENANT = "diku";
  private static final Map<String, String> OKAPI_HEADERS = Map.of("x-okapi-tenant", TENANT);
  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static List<Contract> exampleContracts;
  private static TenantUtil tenantUtil;

  @BeforeClass
  public static void beforeClass(TestContext context) throws IOException {
    int port = NetworkUtils.nextFreePort();
    RestAssured.reset();
    RestAssured.baseURI = HOST;
    RestAssured.port = port;
    RestAssured.defaultParser = Parser.JSON;
    RestAssured.requestSpecification =
        new RequestSpecBuilder()
            .setBasePath(BASE_PATH_CONTRACTS)
            .addHeaders(OKAPI_HEADERS)
            .addHeader("Content-Type", "application/json")
            .build();

    String exampleContractsStr =
        Resources.toString(Resources.getResource("examplecontracts.json"), StandardCharsets.UTF_8);
    exampleContracts = JacksonCodec.decodeValue(exampleContractsStr, new TypeReference<>() {});

    tenantUtil =
        new TenantUtil(
            new TenantClient(HOST + ":" + port, TENANT, "someToken", WebClient.create(vertx)));
    PostgresClient.setPostgresTester(new PostgresTesterContainer());

    DeploymentOptions options =
        new DeploymentOptions().setConfig(new JsonObject().put("http.port", port));

    vertx.deployVerticle(RestVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @AfterClass
  public static void afterClass() {
    RestAssured.reset();
  }

  @After
  public void tearDown(TestContext context) {
    tenantUtil.teardownTenant().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testWithoutLoadSampleAttribute(TestContext context) {
    Async async = context.async();
    tenantUtil
        .setupTenant(new TenantAttributes().withModuleTo(ModuleName.getModuleVersion()))
        .onComplete(context.asyncAssertSuccess(h -> async.complete()));
    async.awaitSuccess();

    Contracts getResult = given().get().then().extract().as(Contracts.class);
    assertThat(getResult)
        .satisfies(
            contracts -> {
              assertThat(contracts.getTotalRecords()).isZero();
              assertThat(contracts.getContracts()).isEmpty();
            });
  }

  @Test
  public void testWithLoadSampleAttribute(TestContext context) {
    Async async = context.async();
    tenantUtil
        .setupTenant(
            new TenantAttributes()
                .withModuleTo(ModuleName.getModuleVersion())
                .withParameters(List.of(new Parameter().withKey("loadSample").withValue("true"))))
        .onComplete(context.asyncAssertSuccess(h -> async.complete()));
    async.awaitSuccess();

    Contracts getResult = given().get().then().extract().as(Contracts.class);
    assertThat(getResult)
        .satisfies(
            contracts -> {
              assertThat(contracts.getTotalRecords()).isEqualTo(20);
              assertThat(contracts.getContracts()).hasSize(10);
              assertThat(contracts.getContracts())
                  .usingRecursiveFieldByFieldElementComparatorIgnoringFields("metadata")
                  .isSubsetOf(exampleContracts);
            });
  }
}
