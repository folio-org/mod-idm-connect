package org.folio.rest.impl;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static org.folio.idmconnect.Constants.JSONB_FIELD_LIBRARYCARD;
import static org.folio.idmconnect.Constants.JSONB_FIELD_UNILOGIN;
import static org.folio.idmconnect.Constants.TABLE_NAME_CONTRACTS;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import org.folio.idmconnect.IdmClient;
import org.folio.idmconnect.IdmClientFactory;
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
import org.folio.rest.tools.utils.TenantTool;

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
    PgUtil.postgresClient(vertxContext, okapiHeaders)
        .withTrans(
            conn ->
                conn.getByIdForUpdate(TABLE_NAME_CONTRACTS, id, Contract.class)
                    .flatMap(
                        c ->
                            transmitContract(c)
                                .transform(
                                    ar -> {
                                      if (ar.succeeded()) {
                                        return updateContractStatus(
                                                c, ar.result().getStatus() == 200, conn)
                                            .transform(
                                                v ->
                                                    succeededFuture(
                                                        Response.fromResponse(ar.result())
                                                            .build()));
                                      } else {
                                        return updateContractStatus(c, false, conn)
                                            .transform(
                                                v ->
                                                    succeededFuture(
                                                        GetIdmConnectContractTransmitByIdResponse
                                                            .respond500WithTextPlain(
                                                                ar.cause().getMessage())));
                                      }
                                    }))
                    .otherwise(GetIdmConnectContractTransmitByIdResponse::respond500WithTextPlain)
                    .onComplete(resp -> asyncResultHandler.handle(succeededFuture(resp.result()))));
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
    IdmClientFactory.create()
        .search(firstname, lastname, dateOfBirth)
        .onComplete(asyncResultHandler);
  }

  @Override
  public void postIdmConnectUbreadernumber(
      String unilogin,
      String uBReaderNumber,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    IdmClientFactory.create()
        .postUBReaderNumber(unilogin, uBReaderNumber)
        .onSuccess(
            resp -> {
              if (resp.getStatus() / 100 == 2) {
                updateContractLibraryCard(okapiHeaders, vertxContext, unilogin, uBReaderNumber);
              }
            })
        .onComplete(asyncResultHandler);
  }

  @Override
  public void deleteIdmConnectUbreadernumber(
      String unilogin,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    IdmClientFactory.create()
        .deleteUBReaderNumber(unilogin)
        .onSuccess(
            resp -> {
              if (resp.getStatus() / 100 == 2) {
                updateContractLibraryCard(okapiHeaders, vertxContext, unilogin, null);
              }
            })
        .onComplete(asyncResultHandler);
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

  private Future<Response> transmitContract(Contract contract) {
    if (contract.getStatus() == null) {
      contract.setStatus(Status.DRAFT);
    }
    IdmClient idmClient = IdmClientFactory.create();
    if (Stream.of(Status.DRAFT, Status.TRANSMISSION_ERROR)
        .anyMatch(status -> contract.getStatus().equals(status))) {
      return idmClient.postContract(contract);
    } else {
      return idmClient.putContract(contract);
    }
  }

  private Future<Void> updateContractStatus(Contract contract, boolean succeeded, Conn conn) {
    if (contract.getStatus() == null) {
      contract.setStatus(Status.DRAFT);
    }
    if (succeeded) {
      if (Stream.of(Status.DRAFT, Status.TRANSMISSION_ERROR)
          .anyMatch(status -> contract.getStatus().equals(status))) {
        contract.setStatus(Status.PENDING);
      } else {
        contract.setStatus(Status.PENDING_EDIT);
      }
    } else {
      if (Stream.of(Status.DRAFT, Status.TRANSMISSION_ERROR)
          .anyMatch(status -> contract.getStatus().equals(status))) {
        contract.setStatus(Status.TRANSMISSION_ERROR);
      } else {
        contract.setStatus(Status.TRANSMISSION_ERROR_EDIT);
      }
    }
    return conn.update(TABLE_NAME_CONTRACTS, contract, contract.getId())
        .flatMap(
            rs ->
                (rs.rowCount() == 1
                    ? succeededFuture()
                    : failedFuture("Updating status of " + contract.getId() + " failed")));
  }

  private Future<RowSet<Row>> updateContractLibraryCard(
      Map<String, String> okapiHeaders, Context vertxContext, String unilogin, String libraryCard) {
    PostgresClient pgClient = PgUtil.postgresClient(vertxContext, okapiHeaders);
    if (unilogin == null) {
      return failedFuture("unilogin cant be null");
    }

    String table =
        PostgresClient.convertToPsqlStandard(TenantTool.tenantId(okapiHeaders))
            + "."
            + TABLE_NAME_CONTRACTS;

    if (libraryCard != null) {
      return pgClient.execute(
          "UPDATE "
              + table
              + " SET jsonb = jsonb_set(jsonb, '{"
              + JSONB_FIELD_LIBRARYCARD
              + "}', $1) WHERE (jsonb->'"
              + JSONB_FIELD_UNILOGIN
              + "') = $2",
          Tuple.of(libraryCard, unilogin));
    } else {
      return pgClient.execute(
          "UPDATE "
              + table
              + " SET jsonb = jsonb - '"
              + JSONB_FIELD_LIBRARYCARD
              + "' WHERE (jsonb->'"
              + JSONB_FIELD_UNILOGIN
              + "') = $1",
          Tuple.of(unilogin));
    }
  }
}
