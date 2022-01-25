package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.idmconnect.Constants.TABLE_NAME_CONTRACTS;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import org.folio.idmconnect.IdmClientImpl;
import org.folio.okapi.common.GenericCompositeFuture;
import org.folio.rest.jaxrs.model.BulkDeleteRequest;
import org.folio.rest.jaxrs.model.BulkDeleteResponse;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.Contract.Status;
import org.folio.rest.jaxrs.model.Contracts;
import org.folio.rest.jaxrs.resource.IdmConnect;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;

public class IdmConnectApi implements IdmConnect {

  @Override
  public void getIdmConnectContract(
      String query,
      int offset,
      int limit,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.get(
        TABLE_NAME_CONTRACTS,
        Contract.class,
        Contracts.class,
        query,
        offset,
        limit,
        okapiHeaders,
        vertxContext,
        GetIdmConnectContractResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postIdmConnectContract(
      String lang,
      Contract entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.post(
        TABLE_NAME_CONTRACTS,
        entity.withStatus(Status.DRAFT),
        okapiHeaders,
        vertxContext,
        PostIdmConnectContractResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getIdmConnectContractById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.getById(
        TABLE_NAME_CONTRACTS,
        Contract.class,
        id,
        okapiHeaders,
        vertxContext,
        GetIdmConnectContractByIdResponse.class,
        asyncResultHandler);
  }

  private Future<DeleteIdmConnectContractByIdResponse> deleteContract(
      Contract contract, Conn conn) {
    if (contract == null) {
      return succeededFuture(
          DeleteIdmConnectContractByIdResponse.respond404WithTextPlain("Not found"));
    }
    if (contract.getStatus().equals(Status.DRAFT)) {
      return conn.delete(TABLE_NAME_CONTRACTS, contract.getId())
          .map(
              rs ->
                  rs.rowCount() == 0
                      ? DeleteIdmConnectContractByIdResponse.respond404WithTextPlain("Not found")
                      : DeleteIdmConnectContractByIdResponse.respond204());
    } else {
      return succeededFuture(
          DeleteIdmConnectContractByIdResponse.respond400WithTextPlain(
              "Not allowed to delete contract with status != draft."));
    }
  }

  @Override
  public void deleteIdmConnectContractById(
      String id,
      String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    PgUtil.postgresClient(vertxContext, okapiHeaders)
        .withTrans(
            conn ->
                conn.getByIdForUpdate(TABLE_NAME_CONTRACTS, id, Contract.class)
                    .flatMap(contract -> deleteContract(contract, conn)))
        .onSuccess(deleteResponse -> asyncResultHandler.handle(succeededFuture(deleteResponse)))
        .onFailure(
            t ->
                asyncResultHandler.handle(
                    succeededFuture(
                        DeleteIdmConnectContractByIdResponse.respond500WithTextPlain(t))));
  }

  @Override
  public void getIdmConnectContractTransmitById(
      String id,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    asyncResultHandler.handle(
        succeededFuture(GetIdmConnectContractTransmitByIdResponse.respond200()));
  }

  @Override
  public void postIdmConnectContractBulkDelete(
      BulkDeleteRequest entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {

    PostgresClient pgClient = PgUtil.postgresClient(vertxContext, okapiHeaders);

    List<Future<String>> results =
        entity.getUuids().stream()
            .map(
                id ->
                    pgClient
                        .delete(TABLE_NAME_CONTRACTS, id)
                        .transform(
                            ar -> {
                              if (ar.failed() || ar.result().rowCount() == 0) {
                                return succeededFuture(id);
                              } else {
                                return succeededFuture(null);
                              }
                            }))
            .collect(Collectors.toList());

    CompositeFuture join = GenericCompositeFuture.join(results);
    join.onComplete(
        ar -> {
          List<String> failedUUIDs =
              join.<String>list().stream().filter(Objects::nonNull).collect(Collectors.toList());
          asyncResultHandler.handle(
              succeededFuture(
                  PostIdmConnectContractBulkDeleteResponse.respond200WithApplicationJson(
                      new BulkDeleteResponse()
                          .withRequested(entity.getUuids().size())
                          .withDeleted(entity.getUuids().size() - failedUUIDs.size())
                          .withFailed(failedUUIDs.size())
                          .withFailedItems(failedUUIDs))));
        });
  }

  @Override
  public void putIdmConnectContractById(
      String id,
      String lang,
      Contract entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.put(
        TABLE_NAME_CONTRACTS,
        entity,
        id,
        okapiHeaders,
        vertxContext,
        PutIdmConnectContractByIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getIdmConnectSearchidm(
      String firstname,
      String lastname,
      String dateOfBirth,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    new IdmClientImpl(vertxContext)
        .search(firstname, lastname, dateOfBirth)
        .onComplete(asyncResultHandler);
  }
}
