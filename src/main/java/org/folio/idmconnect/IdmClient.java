package org.folio.idmconnect;

import io.vertx.core.Future;
import javax.ws.rs.core.Response;
import org.folio.rest.jaxrs.model.Contract;

public interface IdmClient {

  Future<Response> search(String firstName, String lastName, String dateOfBirth);

  Future<Response> putContract(Contract contract);

  Future<Response> postContract(Contract contract);
}
