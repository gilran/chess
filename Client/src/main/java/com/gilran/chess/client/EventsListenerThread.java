package com.gilran.chess.client;

import com.gilran.chess.Proto.EventsRequest;
import com.gilran.chess.Proto.EventsResponse;
import com.gilran.chess.Proto.GameEvent;
import com.gilran.chess.Proto.GameInfo;
import com.gilran.chess.client.Client.LoggerAdapter;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A thread that listens to game events.
 * The listener polls the server for events.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class EventsListenerThread extends Thread {
  /** The maximum failed attempts before the listener stops polling. */
  private static final int MAX_FAILED_ATTEMPTS = 25;

  /** The session token. */
  private String sessionToken;
  /** The game identifier. */
  private String gameId;
  /** And http getter for sending requests to the server. */
  private HttpGetter httpGetter;
  /** The event handler for the returned events. */
  private GameEventHandler eventHandler;
  /** Indicates whether the listener is active or not. */
  private AtomicBoolean active;

  /**
   * Constructor.
   *
   * @param baseUrl The base URL of the server.
   * @param sessionToken The session token of the user's session.
   * @param gameId The identifier of the game for which we are listening for
   *     events.
   * @param handler An event handler that will be invoked for each recieved
   *     event.
   * @param logger The logger that should be used. If null, a default logger,
   *     using standard java logging is used.
   */
  public EventsListenerThread(
      String baseUrl,
      String sessionToken,
      String gameId,
      GameEventHandler handler,
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
          .setGameInfo(GameInfo.newBuilder()
              .setSessionToken(sessionToken)
              .setGameId(gameId))
          .setMinEventNumber(nextEventNumber)
          .build(),
          EventsResponse.class);
      if (response == null) {
        failedAttmpts++;
        if (failedAttmpts == MAX_FAILED_ATTEMPTS) {
          break;
        }
        continue;
      }
      failedAttmpts = 0;
      List<GameEvent> events = response.getEventList();
      nextEventNumber = events.get(events.size() - 1).getSerialNumber() + 1;
      for (GameEvent event : events) {
        eventHandler.handle(event);
      }
    }
  }

  /** Stops the listener. */
  public void stopListening() {
    active.set(false);
  }
}
