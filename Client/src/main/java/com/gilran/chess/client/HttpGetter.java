package com.gilran.chess.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.gilran.chess.JsonParser;
import com.gilran.chess.client.Client.LoggerAdapter;
import com.gilran.chess.client.Client.LoggerAdapter.Level;
import com.google.protobuf.Message;

public class HttpGetter {
  private LoggerAdapter logger;
  private String baseUrl;
  private DefaultHttpClient httpClient = new DefaultHttpClient();

  public HttpGetter(String baseUrl, LoggerAdapter logger) {
    this.logger = logger == null ? new DefaultLogger() : logger;
    this.baseUrl = baseUrl;
    this.httpClient = new DefaultHttpClient();
  }

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

    if (!responseType.isAssignableFrom(responseProto.getClass()))
      return null;
    return responseType.cast(responseProto);
  }

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