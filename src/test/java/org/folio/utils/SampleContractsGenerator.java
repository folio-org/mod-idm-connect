package org.folio.utils;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collector;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.folio.rest.jaxrs.model.Address;
import org.folio.rest.jaxrs.model.Contract;
import org.folio.rest.jaxrs.model.Contract.Status;
import org.folio.rest.jaxrs.model.Personal;

public class SampleContractsGenerator {

  private static final String[] mailExtensions = {
    "googlemail.com", "yahoo.com", "hotmail.com", "aol.com", "icloud.com"
  };

  private static String createUniLogin() {
    return RandomStringUtils.randomAlphabetic(3).toLowerCase()
        + RandomStringUtils.randomNumeric(2)
        + RandomStringUtils.randomAlphabetic(3).toLowerCase();
  }

  private static Status createStatus() {
    return Status.values()[RandomUtils.nextInt(0, Status.values().length)];
  }

  private static LocalDate createBeginDate() {
    return LocalDate.of(2010, 1, 1).plusDays(RandomUtils.nextInt(0, 4018));
  }

  private static String createAddressLine2() {
    if (RandomUtils.nextInt(0, 10) <= 3) {
      return RandomStringUtils.randomAlphabetic(1).toUpperCase()
          + "/"
          + RandomStringUtils.randomNumeric(1);
    }
    return null;
  }

  private static String createTitle() {
    int i = RandomUtils.nextInt(0, 10);
    if (i <= 2) return "Prof.";
    if (i <= 4) return "Dr.";
    return null;
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    WebClient.create(vertx)
        .getAbs("https://randomname.de/")
        .addQueryParam("format", "json")
        .addQueryParam("count", "20")
        .addQueryParam("email", String.join(",", mailExtensions))
        .send()
        .map(
            bufferHttpResponse ->
                bufferHttpResponse.bodyAsJsonArray().stream()
                    .map(JsonObject::mapFrom)
                    .map(
                        jo -> {
                          LocalDate beginDate = createBeginDate();
                          return new Contract()
                              .withPersonal(
                                  new Personal()
                                      .withAddress(
                                          new Address()
                                              .withAddressLine1(
                                                  jo.getJsonObject("location")
                                                          .getJsonObject("street")
                                                          .getString("name")
                                                      + " "
                                                      + jo.getJsonObject("location")
                                                          .getJsonObject("street")
                                                          .getString("number"))
                                              .withAddressLine2(createAddressLine2())
                                              .withCity(
                                                  jo.getJsonObject("location").getString("city"))
                                              .withZipCode(
                                                  jo.getJsonObject("location").getString("zip"))
                                              .withCountry("Germany"))
                                      .withFirstName(jo.getString("firstname"))
                                      .withLastName(jo.getString("lastname"))
                                      .withDateOfBirth(
                                          LocalDate.parse(
                                                  jo.getString("birthday"),
                                                  DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                              .toString())
                                      .withEmail(jo.getString("email").toLowerCase())
                                      .withAcademicTitle(createTitle()))
                              .withComment("A comment.")
                              .withId(UUID.randomUUID().toString())
                              .withUniLogin(createUniLogin())
                              .withStatus(createStatus())
                              .withLibraryCard(RandomStringUtils.randomNumeric(8))
                              .withBeginDate(beginDate.toString())
                              .withEndDate(beginDate.plusYears(2).toString());
                        })
                    .collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::addAll)))
        .onSuccess(
            jsonArray -> {
              String jsonStr = Json.encodePrettily(jsonArray);
              System.out.println(jsonStr);
              try {
                Files.writeString(
                    Paths.get("./examplecontracts.json"), jsonStr, StandardOpenOption.CREATE);
              } catch (IOException e) {
                System.err.println("error writing to file: " + e.getMessage());
              }
              vertx.close();
            })
        .onFailure(
            t -> {
              System.err.println(t.getMessage());
              vertx.close();
            });
  }
}
