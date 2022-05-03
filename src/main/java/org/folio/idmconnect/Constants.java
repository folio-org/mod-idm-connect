package org.folio.idmconnect;

import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_CONTRACT_URL;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_READER_NUMBER_URL;
import static org.folio.idmconnect.IdmClientConfig.ENVVAR_IDM_URL;

public class Constants {

  public static final String TABLE_NAME_CONTRACTS = "contract";
  public static final String BASE_PATH_CONTRACTS = "/idm-connect/contract"; // NOSONAR
  public static final String BASE_PATH_SEARCHIDM = "/idm-connect/searchidm"; // NOSONAR
  public static final String BASE_PATH_READER_NUMDER = "/idm-connect/ubreadernumber"; // NOSONAR
  public static final String PATH_BULK_DELETE = "/bulk-delete"; // NOSONAR
  public static final String MSG_IDM_URL_NOT_SET = createMsgEnvVarNotSet(ENVVAR_IDM_URL);
  public static final String MSG_IDM_CONTRACT_URL_NOT_SET =
      createMsgEnvVarNotSet(ENVVAR_IDM_CONTRACT_URL);
  public static final String MSG_IDM_READER_NUMBER_URL_NOT_SET =
      createMsgEnvVarNotSet(ENVVAR_IDM_READER_NUMBER_URL);

  private static String createMsgEnvVarNotSet(String envVarName) {
    return envVarName + " environment variable not set.";
  }

  private Constants() {}
}
