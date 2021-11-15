package org.folio.rest.impl;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.TenantAttributes;

public class TenantUtil {

  private final TenantClient tenantClient;

  public TenantUtil(TenantClient tenantClient) {
    this.tenantClient = tenantClient;
  }

  private String getIdFromResponseBody(HttpResponse<Buffer> response) {
    return response.bodyAsJsonObject().getString("id");
  }

  private String getCompleteFromResponseBody(HttpResponse<Buffer> response) {
    return response.bodyAsJsonObject().getString("complete");
  }

  private Future<Void> handlePostTenantResponse(HttpResponse<Buffer> response) {
    if (response.statusCode() == 204) {
      return Future.succeededFuture();
    }
    if (response.statusCode() == 201) {
      return tenantClient
          .getTenantByOperationId(getIdFromResponseBody(response), 5000)
          .compose(this::handleGetTenantByOperationIdResponse);
    }
    return Future.failedFuture("postTenant response returned " + response.statusCode());
  }

  private Future<Void> handleGetTenantByOperationIdResponse(HttpResponse<Buffer> response) {
    if (response == null) {
      return Future.succeededFuture();
    }
    if (response.statusCode() == 200
        && "true".equals(response.bodyAsJsonObject().getString("complete"))) {
      return tenantClient
          .deleteTenantByOperationId(getIdFromResponseBody(response))
          .compose(this::handleDeleteTenantByOperationIdResponse);
    }
    return Future.failedFuture(
        String.format(
            "getTenantByOperationId response returned %s with complete=%s",
            response.statusCode(), getCompleteFromResponseBody(response)));
  }

  private Future<Void> handleDeleteTenantByOperationIdResponse(HttpResponse<Buffer> response) {
    if (response == null || response.statusCode() == 204) {
      return Future.succeededFuture();
    }
    return Future.failedFuture("deleteTenantByOperationId returned " + response.statusCode());
  }

  public Future<Void> setupTenant(TenantAttributes tenantAttributes) {
    return tenantClient.postTenant(tenantAttributes).compose(this::handlePostTenantResponse);
  }

  public Future<Void> teardownTenant() {
    return tenantClient
        .postTenant(new TenantAttributes().withPurge(true))
        .compose(this::handlePostTenantResponse);
  }
}
