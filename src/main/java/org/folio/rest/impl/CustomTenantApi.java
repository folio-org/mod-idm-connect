package org.folio.rest.impl;

import static org.folio.idmconnect.Constants.TABLE_NAME_CONTRACTS;

import com.google.common.io.Resources;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.tools.utils.MetadataUtil;

public class CustomTenantApi extends TenantAPI {

  @Override
  Future<Integer> loadData(
      TenantAttributes attributes,
      String tenantId,
      Map<String, String> headers,
      Context vertxContext) {
    return super.loadData(attributes, tenantId, headers, vertxContext)
        .compose(
            superCount -> {
              if (attributes.getParameters().stream()
                  .anyMatch(p -> p.getKey().equals("loadSample") && p.getValue().equals("true"))) {
                List<Contract> contracts;
                try {
                  String contractsStr =
                      Resources.toString(
                          Resources.getResource("examplecontracts.json"), StandardCharsets.UTF_8);
                  contracts = List.of(Json.decodeValue(contractsStr, Contract[].class));
                  MetadataUtil.populateMetadata(contracts, headers);
                } catch (IOException | ReflectiveOperationException e) {
                  return Future.failedFuture(e);
                }

                return PgUtil.postgresClient(vertxContext, headers)
                    .saveBatch(TABLE_NAME_CONTRACTS, contracts)
                    .map(rs -> rs.size() + superCount);
              } else {
                return Future.succeededFuture(superCount);
              }
            });
  }
}
