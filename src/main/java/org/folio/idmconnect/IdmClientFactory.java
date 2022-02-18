package org.folio.idmconnect;

public class IdmClientFactory {

  private IdmClientFactory() {}

  public static IdmClient create() {
    return new IdmClientImpl();
  }
}
