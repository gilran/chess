package com.gilran.chess.client;

import com.gilran.chess.JsonParser;
import com.gilran.chess.client.Client.LoggerAdapter;
import com.gilran.chess.client.Client.LoggerAdapter.Level;

import com.google.protobuf.Message;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;

/**
 * A class for managing HTTP GET requests.
 * <p>The HttpGetter hides the HTTP GET request by using only protobuf messages
 * in its public interface.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class HttpGetter {
  /** Logger. */
  private LoggerAdapter logger;
  /** The server base URL. */
  private String baseUrl;
  /** An http client. */
  private DefaultHttpClient httpClient = new DefaultHttpClient();

  /**
   * Constructor.
   *
   * @param baseUrl The base URL of the server.
   * @param logger The logger that should be used. If null, a default logger,
   *     using standard java logging is used.
   */
  public HttpGetter(String baseUrl, LoggerAdapter logger) {
    this.logger = logger == null ? new DefaultLogger() : logger;
    this.baseUrl = baseUrl;
    this.httpClient = new DefaultHttpClient();
  }

  /**
   * Does a GET request.
   *
   * @param methodName The name of the method to call.
   * @param request The request protobuf message.
   * @param responseType The type of the response.
   * @return The response.
   */
  public <T extends Message> T get(
      String methodName, Message request, Class<T> responseType) {
    HttpGet getRequest;
    try {
      String url = baseUrl + methodName + "?r=" + URLEncoder.encode(
          JsonParser.toJson(request), "UTF-8");
      getRequest = new HttpGet(url);
    } catch (Exception e) {
      logger.log(Level.ERROR, e.toString());
      return null;
    }
    getRequest.addHeader("accept", "application/json");

    HttpResponse response;
    try {
      response = httpClient.execute(getRequest);
    } catch (Exception e) {
      logger.log(Level.ERROR, e.toString());
      return null;
    }

    if (response.getStatusLine().getStatusCode() != 200) {
      logger.log(Level.ERROR, "Failed : HTTP error code : "
         + response.getStatusLine().getStatusCode());
      return null;
    }

    Message responseProto = null;
    try {
      responseProto = parseResponse(response, responseType);
    } catch (Exception e) {
      return null;
    }

    if (responseProto == null ||
        !responseType.isAssignableFrom(responseProto.getClass())) {
      return null;
    }
    return responseType.cast(responseProto);
  }

  /**
   * Parses an HttpResponse to a protobuf message of the given type.
   *
   * @param response The HTTP response.
   * @param type The response protobuf message type.
   * @return The response message.
   */
  private <T extends Message> Message parseResponse(
      HttpResponse response, Class<T> type) {
    BufferedReader br;
    try {
      br = new BufferedReader(
          new InputStreamReader((response.getEntity().getContent())));
    } catch (Exception e) {
      logger.log(Level.ERROR, "Failed to get response content: " + e);
      return null;
    }

    StringBuilder outputBuilder = new StringBuilder();
    String line;
    try {
      while ((line = br.readLine()) != null) {
        outputBuilder.append(line);
      }
    } catch (IOException e) {
      logger.log(Level.ERROR, "Failed to read response content: " + e);
      return null;
    }

    return JsonParser.toProto(outputBuilder.toString(), type);
  }
}
