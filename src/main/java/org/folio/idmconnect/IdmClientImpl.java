package org.folio.idmconnect;

import static io.vertx.core.Future.succeededFuture;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.folio.idmconnect.Constants.MSG_IDM_CONTRACT_URL_NOT_SET;
import static org.folio.idmconnect.Constants.MSG_IDM_READER_NUMBER_URL_NOT_SET;
import static org.folio.idmconnect.Constants.MSG_IDM_URL_NOT_SET;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.folio.rest.jaxrs.model.Contract;

public class IdmClientImpl implements IdmClient {

  private final String idmUrl;
  private final String idmContractUrl;
  private final String idmToken;
  private final String idmReaderNumberUrl;
  private final WebClient webClient;

  public IdmClientImpl(IdmClientConfig config, WebClient webClient) {
    idmUrl = config.getIdmUrl();
    idmContractUrl = config.getIdmContractUrl();
    idmReaderNumberUrl = config.getIdmReaderNumberUrl();
    idmToken = config.getIdmToken();
    this.webClient = webClient;
  }

  private String toBasicIsoDate(String dateString) {
    try {
      return LocalDate.parse(dateString).format(BASIC_ISO_DATE);
    } catch (NullPointerException | DateTimeException e) {
      return dateString;
    }
  }

  private Response createResponse(Object o) {
    return Response.status(500).header(CONTENT_TYPE, TEXT_PLAIN).entity(o).build();
  }

  private Response toResponse(HttpResponse<Buffer> bufferHttpResponse) {
    ResponseBuilder responseBuilder =
        Response.status(bufferHttpResponse.statusCode())
            .header(CONTENT_TYPE, bufferHttpResponse.getHeader(CONTENT_TYPE))
            .entity(bufferHttpResponse.bodyAsString());
    return responseBuilder.build();
  }

  private HttpRequest<Buffer> createIdmRequest(HttpMethod httpMethod, String requestUri) {
    return createIdmRequest(httpMethod, requestUri, null);
  }

  private HttpRequest<Buffer> createIdmRequest(
      HttpMethod httpMethod, String requestUri, Map<String, Optional<String>> queryParams) {
    HttpRequest<Buffer> bufferHttpRequest = webClient.requestAbs(httpMethod, requestUri);
    if (idmToken != null) {
      bufferHttpRequest.putHeader(AUTHORIZATION, idmToken);
    }

    if (queryParams != null) {
      queryParams.forEach((k, v) -> v.ifPresent(s -> bufferHttpRequest.addQueryParam(k, s)));
    }
    return bufferHttpRequest;
  }

  private HttpRequest<Buffer> createIdmUBReaderNumberRequest(
      HttpMethod httpMethod, String requestUri, String unilogin, String readerNumber) {
    return createIdmRequest(
        httpMethod,
        requestUri,
        Map.of("unilogin", ofNullable(unilogin), "UBReaderNumber", ofNullable(readerNumber)));
  }

  @Override
  public Future<Response> search(String firstName, String lastName, String dateOfBirth) {
    return ofNullable(idmUrl)
        .map(
            url ->
                createIdmRequest(
                        HttpMethod.GET,
                        idmUrl,
                        Map.of(
                            "givenname",
                            ofNullable(firstName),
                            "surname",
                            ofNullable(lastName),
                            "date_of_birth",
                            ofNullable(toBasicIsoDate(dateOfBirth))))
                    .send()
                    .map(this::toResponse))
        .orElse(succeededFuture(createResponse(MSG_IDM_URL_NOT_SET)));
  }

  @Override
  public Future<Response> putContract(Contract contract) {
    return ofNullable(idmContractUrl)
        .map(url -> createIdmRequest(PUT, url).sendJson(contract).map(this::toResponse))
        .orElse(succeededFuture(createResponse(MSG_IDM_CONTRACT_URL_NOT_SET)));
  }

  @Override
  public Future<Response> postContract(Contract contract) {
    return ofNullable(idmContractUrl)
        .map(url -> createIdmRequest(POST, url).sendJson(contract).map(this::toResponse))
        .orElse(succeededFuture(createResponse(MSG_IDM_CONTRACT_URL_NOT_SET)));
  }

  @Override
  public Future<Response> postUBReaderNumber(String unilogin, String readerNumber) {
    return ofNullable(idmReaderNumberUrl)
        .map(
            url ->
                createIdmUBReaderNumberRequest(POST, url, unilogin, readerNumber)
                    .send()
                    .map(this::toResponse))
        .orElse(
            succeededFuture(createResponse(MSG_IDM_READER_NUMBER_URL_NOT_SET)));
  }

  @Override
  public Future<Response> deleteUBReaderNumber(String unilogin) {
    return ofNullable(idmReaderNumberUrl)
        .map(
            url ->
                createIdmUBReaderNumberRequest(DELETE, url, unilogin, null)
                    .send()
                    .map(this::toResponse))
        .orElse(succeededFuture(createResponse(MSG_IDM_READER_NUMBER_URL_NOT_SET)));
  }
}
