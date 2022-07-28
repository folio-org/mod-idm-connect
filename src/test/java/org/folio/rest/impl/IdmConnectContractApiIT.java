package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static io.vertx.core.Future.succeededFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.folio.idmconnect.Constants.PATH_BULK_DELETE;
import static org.folio.utils.TestConstants.HOST;
import static org.folio.utils.TestConstants.IDM_TOKEN;
import static org.folio.utils.TestConstants.PATH_ID;
import static org.folio.utils.TestConstants.TENANT;
import static org.folio.utils.TestConstants.setupRestAssured;
import static org.folio.utils.TestEntities.DRAFT;
import static org.folio.utils.TestEntities.PENDING;
import static org.folio.utils.TestEntities.TRANSMISSION_ERROR;
import static org.folio.utils.TestEntities.TRANSMISSION_ERROR_EDIT;
import static org.folio.utils.TestEntities.UPDATED;

import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.BulkDeleteRequest;
import org.folio.rest.jaxrs.model.BulkDeleteResponse;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.Contract.Status;
import org.folio.rest.jaxrs.model.Contracts;
import org.folio.rest.jaxrs.model.Personal;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.folio.utils.TenantUtil;
import org.folio.utils.TestEntities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class IdmConnectContractApiIT {

  private static final String CONTRACT_JSON = "examplecontract.json";
  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static TenantUtil tenantUtil;
  private static Contract sampleContract;
  private static Contract expectedContract;

  @BeforeClass
  public static void beforeClass(TestContext context) throws IOException {
    String jsonStr =
        Resources.toString(Resources.getResource(CONTRACT_JSON), StandardCharsets.UTF_8);
    sampleContract = Json.decodeValue(jsonStr, Contract.class);
    expectedContract = Json.decodeValue(jsonStr, Contract.class).withStatus(Status.DRAFT);

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
        given().body(sampleContract).post().then().statusCode(201).extract().as(Contract.class);
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
                              .hasFieldOrProperty("version")
                              .usingRecursiveComparison()
                              .ignoringFields("id", "metadata", "version")
                              .isEqualTo(expectedContract),
                      atIndex(0));
              assertThat(contracts.getTotalRecords()).isEqualTo(1);
            });

    // GET by id
    Contract getByIdResult =
        given()
            .get(PATH_ID, postResult.getId())
            .then()
            .statusCode(200)
            .extract()
            .as(Contract.class);
    assertThat(getByIdResult).usingRecursiveComparison().isEqualTo(postResult);

    // PUT modified entity
    given()
        .body(getByIdResult.withStatus(Status.DRAFT))
        .put(PATH_ID, getByIdResult.getId())
        .then()
        .statusCode(204);

    // GET by id
    Contract getByIdResult2 =
        given()
            .get(PATH_ID, postResult.getId())
            .then()
            .statusCode(200)
            .extract()
            .as(Contract.class);
    assertThat(getByIdResult2)
        .usingRecursiveComparison()
        .ignoringFields("status", "metadata.updatedDate", "version")
        .isEqualTo(postResult);
    assertThat(getByIdResult2.getStatus()).isEqualTo(Status.DRAFT);

    // DELETE
    given().delete(PATH_ID, getByIdResult.getId()).then().statusCode(204);

    // GET
    Contracts getResult2 = given().get().then().statusCode(200).extract().as(Contracts.class);
    assertThat(getResult2)
        .satisfies(
            contracts -> {
              assertThat(contracts.getContracts()).isEmpty();
              assertThat(contracts.getTotalRecords()).isZero();
            });

    // GET by id
    given().get(PATH_ID, postResult.getId()).then().statusCode(404);

    // DELETE by id
    given().delete(PATH_ID, postResult.getId()).then().statusCode(404);
  }

  @Test
  public void testThatPutNonExistingEntityReturns404() {
    given().body(sampleContract).put(PATH_ID, UUID.randomUUID().toString()).then().statusCode(404);
  }

  @Test
  public void testThatPutWithChangedLibraryCardReturns422() {
    Contract postResult =
        given().body(sampleContract).post().then().statusCode(201).extract().as(Contract.class);
    given()
        .body(postResult.withLibraryCard("123"))
        .put(PATH_ID, postResult.getId())
        .then()
        .statusCode(422);
  }

  @Test
  public void testThatPostWithInvalidContractReturns422() {
    given().body(new Contract().withPersonal(new Personal())).post().then().statusCode(422);
  }

  @Test
  public void testThatPutWithInvalidContractReturns422() {
    // POST
    Contract postResult =
        given().body(sampleContract).post().then().statusCode(201).extract().as(Contract.class);

    // PUT invalid contract
    given()
        .body(postResult.withPersonal(new Personal()))
        .put(PATH_ID, postResult.getId())
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
        .setupTenant(true) // load sample data
        .map(
            v -> {
              String NON_EXISTING_ID = "d4ef9cd7-e57c-4708-bf5a-fba64f622e82";
              String INVALID_ID = "'5c551294-e387-4de2-92d2-5cfe4fa8788d'";
              List<String> uuids =
                  List.of(
                      DRAFT.getId(),
                      UPDATED.getId(),
                      TRANSMISSION_ERROR.getId(),
                      TRANSMISSION_ERROR_EDIT.getId(),
                      PENDING.getId(),
                      NON_EXISTING_ID, // not present in sample data
                      INVALID_ID // invalid UUID
                      );
              uuids.stream()
                  .limit(5)
                  .forEach(id -> given().get(PATH_ID, id).then().statusCode(200));

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
                            .containsExactlyInAnyOrder(NON_EXISTING_ID, INVALID_ID);
                      });

              uuids.stream()
                  .limit(6)
                  .forEach(id -> given().get(PATH_ID, id).then().statusCode(404));
              return succeededFuture();
            })
        .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testThatUpdateWithConflictFails() {
    // POST a contract
    Contract version1 =
        given().body(sampleContract).post().then().statusCode(201).extract().as(Contract.class);
    final String id = version1.getId();

    // Update contract
    given().body(version1.withStatus(Status.UPDATED)).put(PATH_ID, id).then().statusCode(204);

    // Update contract with old _version
    given().body(version1.withStatus(Status.DRAFT)).put(PATH_ID, id).then().statusCode(409);
  }

  @Test
  public void testThatDeleteIsOnlyPossibleForDraftStatus(TestContext context) {
    tenantUtil
        .setupTenant(true) // load sample data
        .map(
            v -> {
              for (TestEntities entity : TestEntities.values()) {
                int expectedStatusCode = entity.getInitialStatus().equals(Status.DRAFT) ? 204 : 400;
                given().delete(PATH_ID, entity.getId()).then().statusCode(expectedStatusCode);
              }
              return succeededFuture();
            })
        .onComplete(context.asyncAssertSuccess());
  }
}
