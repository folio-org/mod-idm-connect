package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.folio.rest.impl.Constants.BASE_PATH_CONTRACTS;
import static org.folio.rest.impl.Constants.PATH_BULK_DELETE;

import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.parsing.Parser;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
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
import org.folio.rest.jaxrs.model.BulkDeleteRequest;
import org.folio.rest.jaxrs.model.BulkDeleteResponse;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.Contract.Status;
import org.folio.rest.jaxrs.model.Contracts;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class IdmConnectContractApiIT {

  private static final String HOST = "http://localhost";
  private static final String TENANT = "diku";
  private static final Map<String, String> OKAPI_HEADERS = Map.of("x-okapi-tenant", TENANT);
  private static final String CONTRACT_JSON = "examplecontract.json";
  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static TenantUtil tenantUtil;
  private static Contract expectedContract;

  @BeforeClass
  public static void beforeClass(TestContext context) throws IOException {
    String jsonStr =
        Resources.toString(Resources.getResource(CONTRACT_JSON), StandardCharsets.UTF_8);
    expectedContract = Json.decodeValue(jsonStr, Contract.class);

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

  @Before
  public void setUp(TestContext context) {
    tenantUtil
        .setupTenant(new TenantAttributes().withModuleTo(ModuleName.getModuleVersion()))
        .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    tenantUtil.teardownTenant().onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testThatWeCanGetPostPutAndDelete() {
    // POST
    Contract postResult =
        given().body(expectedContract).post().then().statusCode(201).extract().as(Contract.class);
    assertThat(postResult)
        .hasFieldOrProperty("id")
        .hasFieldOrProperty("metadata")
        .usingRecursiveComparison()
        .ignoringFields("id", "metadata", "version")
        .isEqualTo(expectedContract);

    // GET
    Contracts getResult = given().get().then().statusCode(200).extract().as(Contracts.class);
    assertThat(getResult)
        .satisfies(
            contracts -> {
              assertThat(contracts.getContracts())
                  .hasSize(1)
                  .satisfies(
                      actualContract ->
                          assertThat(actualContract)
                              .hasFieldOrProperty("id")
                              .hasFieldOrProperty("metadata")
                              .usingRecursiveComparison()
                              .ignoringFields("id", "metadata", "version")
                              .isEqualTo(expectedContract),
                      atIndex(0));
              assertThat(contracts.getTotalRecords()).isEqualTo(1);
            });

    // GET by id
    Contract getByIdResult =
        given()
            .pathParam("id", postResult.getId())
            .get("/{id}")
            .then()
            .statusCode(200)
            .extract()
            .as(Contract.class);
    assertThat(getByIdResult).usingRecursiveComparison().isEqualTo(postResult);

    // PUT modified entity
    given()
        .pathParam("id", getByIdResult.getId())
        .body(getByIdResult.withStatus(Status.UPDATED))
        .put("/{id}")
        .then()
        .statusCode(204);

    // GET by id
    Contract getByIdResult2 =
        given()
            .pathParam("id", postResult.getId())
            .get("/{id}")
            .then()
            .statusCode(200)
            .extract()
            .as(Contract.class);
    assertThat(getByIdResult2)
        .usingRecursiveComparison()
        .ignoringFields("status", "metadata.updatedDate", "version")
        .isEqualTo(postResult);
    assertThat(getByIdResult2.getStatus()).isEqualTo(Status.UPDATED);

    // DELETE
    given().pathParam("id", getByIdResult.getId()).delete("/{id}").then().statusCode(204);

    // GET
    Contracts getResult2 = given().get().then().statusCode(200).extract().as(Contracts.class);
    assertThat(getResult2)
        .satisfies(
            contracts -> {
              assertThat(contracts.getContracts()).isEmpty();
              assertThat(contracts.getTotalRecords()).isZero();
            });

    // GET by id
    given().pathParam("id", postResult.getId()).get("/{id}").then().statusCode(404);
  }

  @Test
  public void testThatPostWithInvalidContractReturns422() {
    given().body(new Contract().withPersonal(new Personal())).post().then().statusCode(422);
  }

  @Test
  public void testThatPutWithInvalidContractReturns422() {
    // POST
    Contract postResult =
        given().body(expectedContract).post().then().statusCode(201).extract().as(Contract.class);
    assertThat(postResult)
        .hasFieldOrProperty("id")
        .hasFieldOrProperty("metadata")
        .usingRecursiveComparison()
        .ignoringFields("id", "metadata", "version")
        .isEqualTo(expectedContract);

    // PUT invalid contract
    given()
        .pathParam("id", postResult.getId())
        .body(postResult.withPersonal(new Personal()))
        .put("/{id}")
        .then()
        .statusCode(422);
  }

  @Test
  public void testThatBulkDeleteWithInvalidRequestsFails() {
    given().post(PATH_BULK_DELETE).then().statusCode(400);
    given().body(new BulkDeleteRequest()).post(PATH_BULK_DELETE).then().statusCode(422);
  }

  @Test
  public void testThatBulkDeleteSucceeds(TestContext context) {
    tenantUtil
        .setupTenant( // load sample data
            new TenantAttributes()
                .withModuleTo(ModuleName.getModuleVersion())
                .withParameters(List.of(new Parameter().withKey("loadSample").withValue("true"))))
        .map(
            v -> {
              List<String> uuids =
                  List.of(
                      "465ce0b3-10cd-4da2-8848-db85b63a0a32",
                      "7f5473c0-e7c3-427c-9202-ba97a1385e50",
                      "8c08a4ee-e8ce-4fb2-823a-05393c429ee8",
                      "a11f000b-6dd7-48d9-b685-2e934a497047",
                      "6b842509-7ddf-4d43-b53a-c97443aa8bb5",
                      "d4ef9cd7-e57c-4708-bf5a-fba64f622e82", // not present in sample data
                      "'5c551294-e387-4de2-92d2-5cfe4fa8788d'" // invalid UUID
                      );
              uuids.stream()
                  .limit(5)
                  .forEach(id -> given().pathParam("id", id).get("/{id}").then().statusCode(200));

              assertThat(
                      given()
                          .body(new BulkDeleteRequest().withUuids(uuids))
                          .post(PATH_BULK_DELETE)
                          .then()
                          .statusCode(200)
                          .extract()
                          .as(BulkDeleteResponse.class))
                  .satisfies(
                      resp -> {
                        assertThat(resp.getRequested()).isEqualTo(7);
                        assertThat(resp.getDeleted()).isEqualTo(5);
                        assertThat(resp.getFailed()).isEqualTo(2);
                        assertThat(resp.getFailedItems())
                            .containsExactlyInAnyOrder(
                                "d4ef9cd7-e57c-4708-bf5a-fba64f622e82",
                                "'5c551294-e387-4de2-92d2-5cfe4fa8788d'");
                      });

              uuids.stream()
                  .limit(6)
                  .forEach(id -> given().pathParam("id", id).get("/{id}").then().statusCode(404));
              return Future.succeededFuture();
            })
        .onComplete(context.asyncAssertSuccess());
  }
}
