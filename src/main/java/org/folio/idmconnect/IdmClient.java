package org.folio.idmconnect;

import io.vertx.core.Future;
import javax.ws.rs.core.Response;

public interface IdmClient {

  Future<Response> search(String firstName, String lastName, String dateOfBirth);
}
