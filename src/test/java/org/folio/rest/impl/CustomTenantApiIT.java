package org.folio.rest.impl;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.utils.TestConstants.HOST;
import static org.folio.utils.TestConstants.IDM_TOKEN;
import static org.folio.utils.TestConstants.TENANT;
import static org.folio.utils.TestConstants.deployRestVerticle;
import static org.folio.utils.TestConstants.setupRestAssured;

import com.google.common.io.Resources;
import io.restassured.RestAssured;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.Contracts;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.ModuleName;
import org.folio.rest.tools.utils.NetworkUtils;
import org.folio.rest.tools.utils.VertxUtils;
import org.folio.utils.TenantUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CustomTenantApiIT {

  private static final Vertx vertx = VertxUtils.getVertxFromContextOrNew();
  private static List<Contract> exampleContracts;
  private static TenantUtil tenantUtil;

  @BeforeClass
  public static void beforeClass(TestContext context) throws IOException {
    int port = NetworkUtils.nextFreePort();
    setupRestAssured(port);

    String exampleContractsStr =
        Resources.toString(Resources.getResource("examplecontracts.json"), StandardCharsets.UTF_8);
    exampleContracts = Arrays.asList(Json.decodeValue(exampleContractsStr, Contract[].class));

    tenantUtil =
        new TenantUtil(
            new TenantClient(HOST + ":" + port, TENANT, IDM_TOKEN, WebClient.create(vertx)));
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    PostgresClient.getInstance(vertx);

    deployRestVerticle(vertx, port).onComplete(context.asyncAssertSuccess());
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
    tenantUtil
        .setupTenant(new TenantAttributes().withModuleTo(ModuleName.getModuleVersion()))
        .map(
            v -> {
              Contracts getResult = given().get().then().extract().as(Contracts.class);
              assertThat(getResult)
                  .satisfies(
                      contracts -> {
                        assertThat(contracts.getTotalRecords()).isZero();
                        assertThat(contracts.getContracts()).isEmpty();
                      });
              return Future.succeededFuture();
            })
        .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testWithLoadSampleAttribute(TestContext context) {
    tenantUtil
        .setupTenant(
            new TenantAttributes()
                .withModuleTo(ModuleName.getModuleVersion())
                .withParameters(List.of(new Parameter().withKey("loadSample").withValue("true"))))
        .map(
            v -> {
              Contracts getResult = given().get().then().extract().as(Contracts.class);
              assertThat(getResult)
                  .satisfies(
                      contracts -> {
                        assertThat(contracts.getTotalRecords()).isEqualTo(20);
                        assertThat(contracts.getContracts()).hasSize(10);
                        assertThat(contracts.getContracts())
                            .usingRecursiveFieldByFieldElementComparatorIgnoringFields(
                                "metadata", "version")
                            .isSubsetOf(exampleContracts);
                      });
              return Future.succeededFuture();
            })
        .onComplete(context.asyncAssertSuccess());
  }
}
