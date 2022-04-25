package org.folio.idmconnect;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getenv;

public class IdmClientConfig {

  public static final String ENVVAR_IDM_TOKEN = "IDM_TOKEN";
  public static final String ENVVAR_IDM_URL = "IDM_URL";
  public static final String ENVVAR_IDM_CONTRACT_URL = "IDM_CONTRACT_URL";
  public static final String ENVVAR_IDM_TRUST_ALL = "IDM_TRUST_ALL";

  private final String idmToken;
  private final String idmUrl;
  private final String idmContractUrl;
  private final boolean idmTrustAll;

  public static IdmClientConfig createFromEnvVars() {
    return new Builder()
        .idmUrl(getenv(ENVVAR_IDM_URL))
        .idmToken(getenv(ENVVAR_IDM_TOKEN))
        .idmContractUrl(getenv(ENVVAR_IDM_CONTRACT_URL))
        .idmTrustAll(parseBoolean(getenv(ENVVAR_IDM_TRUST_ALL)))
        .build();
  }

  private IdmClientConfig(Builder builder) {
    this.idmUrl = builder.idmUrl;
    this.idmToken = builder.idmToken;
    this.idmContractUrl = builder.idmContractUrl;
    this.idmTrustAll = builder.idmTrustAll;
  }

  public String getIdmToken() {
    return idmToken;
  }

  public String getIdmUrl() {
    return idmUrl;
  }

  public String getIdmContractUrl() {
    return idmContractUrl;
  }

  public boolean isIdmTrustAll() {
    return idmTrustAll;
  }

  public static class Builder {

    private String idmToken;
    private String idmUrl;
    private String idmContractUrl;
    private boolean idmTrustAll = false;

    public IdmClientConfig build() {
      return new IdmClientConfig(this);
    }

    public Builder idmToken(String idmToken) {
      this.idmToken = idmToken;
      return this;
    }

    public Builder idmUrl(String idmUrl) {
      this.idmUrl = idmUrl;
      return this;
    }

    public Builder idmContractUrl(String idmContractUrl) {
      this.idmContractUrl = idmContractUrl;
      return this;
    }

    public Builder idmTrustAll(boolean idmTrustAll) {
      this.idmTrustAll = idmTrustAll;
      return this;
    }
  }
}
