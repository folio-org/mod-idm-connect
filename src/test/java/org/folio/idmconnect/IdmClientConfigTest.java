package org.folio.idmconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_CONTRACT_URL;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_TOKEN;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_TRUST_ALL;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_URL;

import org.folio.idmconnect.IdmClientConfig.Builder;
import org.junit.Rule;
import org.junit.Test;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

public class IdmClientConfigTest {
  @Rule public EnvironmentVariablesRule envs = new EnvironmentVariablesRule();

  @Test
  public void testBuilderAndCreateFromEnvVars() {
    final String idmUrl = "http://localhost:1234/search";
    final String idmContractUrl = "http://localhost:1234/contracts";
    final String idmToken = "token123";

    IdmClientConfig config =
        new Builder()
            .idmTrustAll(true)
            .idmToken(idmToken)
            .idmUrl(idmUrl)
            .idmContractUrl(idmContractUrl)
            .build();

    assertThat(config.getIdmUrl()).isEqualTo(idmUrl);
    assertThat(config.getIdmContractUrl()).isEqualTo(idmContractUrl);
    assertThat(config.getIdmToken()).isEqualTo(idmToken);
    assertThat(config.isIdmTrustAll()).isTrue();

    envs.set(ENVVAR_IDM_URL, idmUrl);
    envs.set(ENVVAR_IDM_CONTRACT_URL, idmContractUrl);
    envs.set(ENVVAR_IDM_TOKEN, idmToken);
    envs.set(ENVVAR_IDM_TRUST_ALL, "true");

    assertThat(IdmClientConfig.createFromEnvVars()).usingRecursiveComparison().isEqualTo(config);
  }

  @Test
  public void testThatisIdmTrustAllDefaultsToFalse() {
    assertThat(IdmClientConfig.createFromEnvVars().isIdmTrustAll()).isFalse();
    assertThat(new IdmClientConfig.Builder().build().isIdmTrustAll()).isFalse();
  }
}
