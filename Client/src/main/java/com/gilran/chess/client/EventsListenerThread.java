package com.gilran.chess.client;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.gilran.chess.Proto.EventsRequest;
import com.gilran.chess.Proto.EventsResponse;
import com.gilran.chess.Proto.GameEvent;
import com.gilran.chess.client.Client.LoggerAdapter;

public class EventsListenerThread extends Thread {
	public interface EventHandler {
		void handle(GameEvent event);
	}
	
	private int MAX_FAILED_ATTEMPTS = 25;
	
	private String sessionToken;
	private String gameId;
	private HttpGetter httpGetter;
	private EventHandler eventHandler;
	private AtomicBoolean active;
	
	public EventsListenerThread(
			String baseUrl,
			String sessionToken,
			String gameId,
			EventHandler handler,
			LoggerAdapter logger) {
		this.active = new AtomicBoolean(true);
    this.sessionToken = sessionToken;
    this.gameId = gameId;
    this.httpGetter = new HttpGetter(baseUrl, logger);
    this.eventHandler = handler;
  }
	
	@Override
  public void run() {
		int failedAttmpts = 0;
		int nextEventNumber = 0;
		while (active.get()) {
		  EventsResponse response = httpGetter.get(
		  		"getEvents",
		  		EventsRequest.newBuilder()
		  		.setSessionToken(sessionToken)
		  		.setGameId(gameId)
		  		.setMinEventNumber(nextEventNumber)
		  		.build(),
		  		EventsResponse.class);
		  if (response == null) {
		  	failedAttmpts++;
		  	if (failedAttmpts == MAX_FAILED_ATTEMPTS)
		  		break;
		  	continue;
		  }
		  failedAttmpts = 0;
		  List<GameEvent> events = response.getEventList();
		  nextEventNumber = events.get(events.size() - 1).getSerialNumber() + 1;
		  for (GameEvent event : events)
		  	eventHandler.handle(event);
		}
	}
	
	public void stopListening() {
		active.set(false);
	}
}