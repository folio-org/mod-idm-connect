package org.folio.idmconnect;

public class Constants {

  public static final String TABLE_NAME_CONTRACTS = "contract";
  public static final String BASE_PATH_CONTRACTS = "/idm-connect/contract"; // NOSONAR
  public static final String BASE_PATH_SEARCHIDM = "/idm-connect/searchidm"; // NOSONAR
  public static final String PATH_BULK_DELETE = "/bulk-delete"; // NOSONAR
  public static final String MSG_IDM_URL_NOT_SET = "IDM_URL environment variable not set.";
  public static final String MSG_IDM_CONTRACT_URL_NOT_SET =
      "IDM_CONTRACT_URL environment variable not set.";

  private Constants() {}
}
