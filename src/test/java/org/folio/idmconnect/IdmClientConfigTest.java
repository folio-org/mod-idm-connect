package org.folio.idmconnect;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.idmconnect.IdmClientConfig.Builder;
import org.junit.Test;

public class IdmClientConfigTest {
  @Test
  public void testBuilderAndCreateFromEnvVars() {
    final String idmUrl = "http://localhost:1234/search";
    final String idmContractUrl = "http://localhost:1234/contracts";
    final String idmReaderNumberUrl = "http://localhost:1234/readernumber";
    final String idmToken = "token123";

    IdmClientConfig config =
        new Builder()
            .idmTrustAll(false)
            .idmToken(idmToken)
            .idmUrl(idmUrl)
            .idmContractUrl(idmContractUrl)
            .idmReaderNumberUrl(idmReaderNumberUrl)
            .build();

    assertThat(config.getIdmUrl()).isEqualTo(idmUrl);
    assertThat(config.getIdmContractUrl()).isEqualTo(idmContractUrl);
    assertThat(config.getIdmToken()).isEqualTo(idmToken);
    assertThat(config.getIdmReaderNumberUrl()).isEqualTo(idmReaderNumberUrl);
    assertThat(config.isIdmTrustAll()).isFalse();

    assertThat(IdmClientConfig.createFromEnvVars()).usingRecursiveComparison().isEqualTo(config);

    assertThat(new IdmClientConfig.Builder().build().isIdmTrustAll()).isFalse();
  }
}
