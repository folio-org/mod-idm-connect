package org.folio.utils;

import org.folio.rest.jaxrs.model.Contract.Status;

public enum TestEntities {
  DRAFT(
      Status.DRAFT,
      "465ce0b3-10cd-4da2-8848-db85b63a0a32",
      Status.TRANSMISSION_ERROR,
      Status.PENDING),
  UPDATED(
      Status.UPDATED,
      "7f5473c0-e7c3-427c-9202-ba97a1385e50",
      Status.TRANSMISSION_ERROR_EDIT,
      Status.PENDING_EDIT),
  TRANSMISSION_ERROR(
      Status.TRANSMISSION_ERROR,
      "961dad38-bdd2-4886-ab55-392df4ccfe39",
      Status.TRANSMISSION_ERROR,
      Status.PENDING),
  TRANSMISSION_ERROR_EDIT(
      Status.TRANSMISSION_ERROR_EDIT,
      "d4927c21-1bbb-4be0-905d-8b4fa02ccc42",
      Status.TRANSMISSION_ERROR_EDIT,
      Status.PENDING_EDIT),
  PENDING(
      Status.PENDING,
      "066e5034-8403-4e51-99db-8378d3239a14",
      Status.TRANSMISSION_ERROR_EDIT,
      Status.PENDING_EDIT),
  PENDING_EDIT(
      Status.PENDING_EDIT,
      "5fd84d19-8c6c-45b8-bd79-69b90b2e35d5",
      Status.TRANSMISSION_ERROR_EDIT,
      Status.PENDING_EDIT);

  TestEntities(Status initialStatus, String id, Status failedStatus, Status succeededStatus) {
    this.initialStatus = initialStatus;
    this.id = id;
    this.failedStatus = failedStatus;
    this.succeededStatus = succeededStatus;
  }

  private final Status initialStatus;
  private final String id;
  private final Status failedStatus;
  private final Status succeededStatus;

  public Status getInitialStatus() {
    return initialStatus;
  }

  public String getId() {
    return id;
  }

  public Status getFailedStatus() {
    return failedStatus;
  }

  public Status getSucceededStatus() {
    return succeededStatus;
  }
}
