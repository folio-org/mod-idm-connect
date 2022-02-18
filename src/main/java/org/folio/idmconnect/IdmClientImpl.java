package org.folio.idmconnect;

import static io.vertx.core.Future.succeededFuture;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.idmconnect.Constants.ENVVAR_IDM_CONTRACT_URL;
import static org.folio.idmconnect.Constants.ENVVAR_IDM_TOKEN;
import static org.folio.idmconnect.Constants.ENVVAR_IDM_URL;
import static org.folio.idmconnect.Constants.MSG_IDM_CONTRACT_URL_NOT_SET;
import static org.folio.idmconnect.Constants.MSG_IDM_URL_NOT_SET;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.tools.utils.VertxUtils;

public class IdmClientImpl implements IdmClient {

  private final String idmUrl;
  private final String idmContractUrl;
  private final String idmToken;
  private final WebClient webClient;

  public IdmClientImpl() {
    idmUrl = System.getenv(ENVVAR_IDM_URL);
    idmContractUrl = System.getenv(ENVVAR_IDM_CONTRACT_URL);
    idmToken = System.getenv(ENVVAR_IDM_TOKEN);
    webClient = WebClient.create(VertxUtils.getVertxFromContextOrNew());
  }

  private String toBasicIsoDate(String dateString) {
    try {
      return LocalDate.parse(dateString).format(BASIC_ISO_DATE);
    } catch (NullPointerException | DateTimeException e) {
      return dateString;
    }
  }

  private Response createResponse(int status, String contentType, Object o) {
    return Response.status(status).header(CONTENT_TYPE, contentType).entity(o).build();
  }

  private Response toResponse(HttpResponse<Buffer> bufferHttpResponse) {
    ResponseBuilder responseBuilder =
        Response.status(bufferHttpResponse.statusCode())
            .header(CONTENT_TYPE, bufferHttpResponse.getHeader(CONTENT_TYPE))
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
      bufferHttpRequest.putHeader(AUTHORIZATION, idmToken);
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
    return Optional.ofNullable(idmUrl)
        .map(
            url ->
                createSearchIdmRequest(
                        webClient, idmUrl, idmToken, firstName, lastName, dateOfBirth)
                    .send()
                    .map(this::toResponse))
        .orElse(succeededFuture(createResponse(500, TEXT_PLAIN, MSG_IDM_URL_NOT_SET)));
  }

  @Override
  public Future<Response> putContract(Contract contract) {
    return Optional.ofNullable(idmContractUrl)
        .map(url -> webClient.putAbs(url).sendJson(contract).map(this::toResponse))
        .orElse(succeededFuture(createResponse(500, TEXT_PLAIN, MSG_IDM_CONTRACT_URL_NOT_SET)));
  }

  @Override
  public Future<Response> postContract(Contract contract) {
    return Optional.ofNullable(idmContractUrl)
        .map(url -> webClient.postAbs(url).sendJson(contract).map(this::toResponse))
        .orElse(succeededFuture(createResponse(500, TEXT_PLAIN, MSG_IDM_CONTRACT_URL_NOT_SET)));
  }
}
