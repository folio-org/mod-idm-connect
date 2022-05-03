package org.folio.idmconnect;

import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.folio.rest.tools.utils.VertxUtils;

public class IdmClientFactory {

  private static WebClient webClientInstance;

  private IdmClientFactory() {}

  public static IdmClient create() {
    IdmClientConfig idmClientConfig = IdmClientConfig.createFromEnvVars();

    return new IdmClientImpl(idmClientConfig, getWebClientInstance(idmClientConfig));
  }

  private static WebClient getWebClientInstance(IdmClientConfig idmClientConfig) {
    if (webClientInstance == null) {
      webClientInstance =
          WebClient.create(
              VertxUtils.getVertxFromContextOrNew(),
              new WebClientOptions().setTrustAll(idmClientConfig.isIdmTrustAll()));
    }
    return webClientInstance;
  }
}
