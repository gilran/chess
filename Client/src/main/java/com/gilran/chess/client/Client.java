package com.gilran.chess.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.gilran.chess.JsonParser;
import com.gilran.chess.Proto.*;
import com.google.protobuf.Message;

public class Client {
	private static final Logger LOGGER = Logger.getLogger(
			Thread.currentThread().getStackTrace()[0].getClassName());
	
	static private DefaultHttpClient httpClient = new DefaultHttpClient();
	
	private String baseUrl;
	private String username;
	private String sessionToken;
	private String gameId;
	
	public Client(String baseUrl, String username) {
		this.baseUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/");
		this.username = username;
	}
	
	private <T extends Message> Message parseResponse(HttpResponse response, Class<T> type) {
		BufferedReader br;
		try {
			br = new BufferedReader(
			    new InputStreamReader((response.getEntity().getContent())));
		} catch (Exception e) {
			LOGGER.severe("Failed to get response content: " + e);
			return null;
		}
		
		StringBuilder outputBuilder = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null) {
				outputBuilder.append(line);
			}
		} catch (IOException e) {
			LOGGER.severe("Failed to read response content: " + e);
			return null;
		}
		
		return JsonParser.toProto(outputBuilder.toString(), type);
	}
	
	private <T extends Message> T doGet(
			String methodName, Message request, Class<T> responseType) {
		HttpGet getRequest;
		try {
			String url = baseUrl + methodName + "?r=" + URLEncoder.encode(JsonParser.toJson(request), "UTF-8"); 
			LOGGER.info(url);
			getRequest = new HttpGet(url);
		} catch (Exception e) {
			LOGGER.severe(e.toString());
			return null;
		}
		getRequest.addHeader("accept", "application/json");
		
		HttpResponse response;
		try {
			response = httpClient.execute(getRequest);
		} catch (Exception e) {
			LOGGER.severe(e.toString());
			return null;
		}
		
		if (response.getStatusLine().getStatusCode() != 200) {
			LOGGER.severe("Failed : HTTP error code : "
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
	
	public LoginResponse login() {
		LoginResponse response = doGet(
				"login",
				LoginRequest.newBuilder().setUsername(username).build(),
				LoginResponse.class);
		if (response == null)
			return null;
		sessionToken = response.getSessionToken();
		return response;
	}
	
	public SeekResponse seek() {
		SeekResponse response = doGet(
				"seek",
				SeekRequest.newBuilder().setSessionToken(sessionToken).build(),
				SeekResponse.class);
		if (response == null)
			return null;
		gameId = response.getGameId();
		return response;
	}
	
	public MoveResponse move(String from, String to) {
		return doGet(
				"move",
				MoveRequest.newBuilder()
						.setSessionToken(sessionToken)
						.setGameId(gameId)
						.setMove(MoveProto.newBuilder().setFrom(from).setTo(to)).build(),
				MoveResponse.class);
	}
}
