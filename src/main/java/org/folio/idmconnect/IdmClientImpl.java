package org.folio.idmconnect;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.idmconnect.Constants.MSG_IDM_URL_NOT_SET;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class IdmClientImpl implements IdmClient {

  private final String IDM_URL;
  private final String IDM_TOKEN;
  private final WebClient webClient;

  public IdmClientImpl(Context context) {
    IDM_URL = System.getenv("IDM_URL");
    IDM_TOKEN = System.getenv("IDM_TOKEN");
    webClient = WebClient.create(context.owner());
  }

  private String toBasicIsoDate(String dateString) {
    try {
      return LocalDate.parse(dateString).format(DateTimeFormatter.BASIC_ISO_DATE);
    } catch (NullPointerException | DateTimeException e) {
      return dateString;
    }
  }

  private Response createResponse(int status, String contentType, Object o) {
    return Response.status(status).header("Content-Type", contentType).entity(o).build();
  }

  private Response toResponse(HttpResponse<Buffer> bufferHttpResponse) {
    ResponseBuilder responseBuilder =
        Response.status(bufferHttpResponse.statusCode())
            .header("Content-Type", bufferHttpResponse.getHeader("Content-Type"))
            .entity(bufferHttpResponse.bodyAsString());
    return responseBuilder.build();
  }

  private HttpRequest<Buffer> createSearchIdmRequest(
      WebClient webClient,
      String idmUrl,
      String idmToken,
      String firstname,
      String lastname,
      String dateOfBirth) {
    HttpRequest<Buffer> bufferHttpRequest = webClient.getAbs(idmUrl);
    if (idmToken != null) {
      bufferHttpRequest.putHeader("Authorization", idmToken);
    }

    Stream.of(
            new String[] {"givenname", firstname},
            new String[] {"surname", lastname},
            new String[] {"date_of_birth", toBasicIsoDate(dateOfBirth)})
        .forEach(
            a -> {
              if (a[1] != null) {
                bufferHttpRequest.addQueryParam(a[0], a[1]);
              }
            });
    return bufferHttpRequest;
  }

  @Override
  public Future<Response> search(String firstName, String lastName, String dateOfBirth) {
    if (IDM_URL == null) {
      return succeededFuture(createResponse(500, "text/plain", MSG_IDM_URL_NOT_SET));
    }

    return createSearchIdmRequest(webClient, IDM_URL, IDM_TOKEN, firstName, lastName, dateOfBirth)
        .send()
        .map(this::toResponse);
  }
}
