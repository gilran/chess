package com.gilran.chess.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.gilran.chess.JsonParser;
import com.gilran.chess.Proto.*;
import com.gilran.chess.client.Client.LoggerAdapter.Level;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;

public class Client {
	public interface LoggerAdapter {
		enum Level { DEBUG, INFO, WARNING, ERROR }
		void log(Level level, String message);
	}
	
	private static class DefaultLogger implements LoggerAdapter {
		static final Map<Level, java.util.logging.Level> LEVELS =
				ImmutableMap.<Level, java.util.logging.Level>builder()
				.put(Level.DEBUG, java.util.logging.Level.FINE)
				.put(Level.INFO, java.util.logging.Level.INFO)
				.put(Level.WARNING, java.util.logging.Level.WARNING)
				.put(Level.ERROR, java.util.logging.Level.SEVERE)
				.build();
		private Logger logger = Logger.getLogger(
				Thread.currentThread().getStackTrace()[1].getClassName());
		public void log(Level level, String message) {
			logger.log(LEVELS.get(level), message);
		}
	}
	
	static private DefaultHttpClient httpClient = new DefaultHttpClient();
	
	private LoggerAdapter logger;
	private String baseUrl;
	private String username;
	private String sessionToken;
	private String gameId;
	private Thread eventsListenerThread;
	
	public Client(String baseUrl, LoggerAdapter logger) {
		this.logger = logger == null ? new DefaultLogger() : logger;
		this.baseUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/");
	}
	
	public Client(String baseUrl) {
		this(baseUrl, null);
	}
	
	private <T extends Message> Message parseResponse(HttpResponse response, Class<T> type) {
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
	
	private <T extends Message> T doGet(
			String methodName, Message request, Class<T> responseType) {
		HttpGet getRequest;
		try {
			String url = baseUrl + methodName + "?r=" + URLEncoder.encode(JsonParser.toJson(request), "UTF-8"); 
			logger.log(Level.DEBUG, url);
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
	
	public LoginResponse login(String username) {
		this.username = username;
		logger.log(Level.DEBUG, "Logging in as user: " + this.username);
		LoginResponse response = doGet(
				"login",
				LoginRequest.newBuilder().setUsername(this.username).build(),
				LoginResponse.class);
		if (response == null)
			return null;
		sessionToken = response.getSessionToken();
		logger.log(Level.DEBUG, "Logged in. Session token: " + sessionToken);
		return response;
	}
	
	public SeekResponse seek() {
		Preconditions.checkNotNull(sessionToken);
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
		Preconditions.checkNotNull(sessionToken);
		Preconditions.checkNotNull(gameId);
		return doGet(
				"move",
				MoveRequest.newBuilder()
						.setSessionToken(sessionToken)
						.setGameId(gameId)
						.setMove(MoveProto.newBuilder().setFrom(from).setTo(to)).build(),
				MoveResponse.class);
	}
	
	public interface EventHandler {
		void handle(GameEvent event);
	}
	private class EventsListenerThread extends Thread {
		private int lastEventNumber;
		private EventHandler eventHandler;
		
		public EventsListenerThread(EventHandler handler) {
	    lastEventNumber = 0;
	    eventHandler = handler;
    }
		
		@Override
    public void run() {
			while (true) {
			  EventsResponse response = doGet(
			  		"getEvents",
			  		EventsRequest.newBuilder()
			  		.setSessionToken(sessionToken)
			  		.setGameId(gameId)
			  		.setMinEventNumber(lastEventNumber + 1)
			  		.build(),
			  		EventsResponse.class);
			  if (response == null)
			  	continue;
			  for (GameEvent event : response.getEventList())
			  	eventHandler.handle(event);
			}
		}
	}
	
	public void startListeningToEvents(EventHandler handler) {
		Preconditions.checkNotNull(sessionToken);
		Preconditions.checkNotNull(gameId);
		eventsListenerThread = new EventsListenerThread(handler);
		eventsListenerThread.start();
	}
}
