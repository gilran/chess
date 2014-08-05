package com.gilran.chess.client;

import com.gilran.chess.Proto.*;
import com.gilran.chess.Proto.GameEvent.Type;
import com.gilran.chess.client.Client.LoggerAdapter.Level;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;

/**
 * A Chess client.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Client {
  /**
   * An interface for a logger.
   * This interface lets the user of this class choose which logging system to
   * use.
   */
  public interface LoggerAdapter {
    /** Log levels. */
    enum Level { DEBUG, INFO, WARNING, ERROR }

    /** Logs the given message with the given log level. */
    void log(Level level, String message);
  }

  /** The logger. */
  private LoggerAdapter logger;
  /** The server base URL. */
  private String baseUrl;
  /** The local user name. */
  private String username;
  /** The session token of an active session. */
  private String sessionToken;
  /** The game identifier of an active game. */
  private String gameId;
  /** An event listener thread for listening to game events. */
  private EventsListenerThread eventsListenerThread;
  /** An http getter for sending GET requests to the server. */
  private HttpGetter httpGetter;

  /**
   * Constructor.
   *
   * @param baseUrl The base URL of the server.
   * @param logger The logger that should be used. If null, a default logger,
   *     using standard java logging is used.
   */
  public Client(String baseUrl, LoggerAdapter logger) {
    this.logger = logger == null ? new DefaultLogger() : logger;
    this.baseUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/");
    this.httpGetter = new HttpGetter(this.baseUrl, logger);
  }

  /**
   * Constructor.
   * The same as the previous constructor, but uses the default logger.
   */
  public Client(String baseUrl) {
    this(baseUrl, null);
  }

  /** Logs-in to the server. */
  public LoginResponse login(String username) {
    this.username = username;
    logger.log(Level.DEBUG, "Logging in as user: " + this.username);
    LoginResponse response = httpGetter.get(
        "login",
        LoginRequest.newBuilder().setUsername(this.username).build(),
        LoginResponse.class);
    if (response == null) {
      return null;
    }
    sessionToken = response.getSessionToken();
    logger.log(Level.DEBUG, "Logged in. Session token: " + sessionToken);
    return response;
  }

  /** Seeks a game. */
  public SeekResponse seek() {
    Preconditions.checkNotNull(sessionToken);
    SeekResponse response = httpGetter.get(
        "seek",
        SeekRequest.newBuilder().setSessionToken(sessionToken).build(),
        SeekResponse.class);
    if (response == null) {
      return null;
    }
    gameId = response.getGameId();
    return response;
  }

  /** Sends a move to the server. */
  public ErrorResponse move(String from, String to) {
    Preconditions.checkNotNull(sessionToken);
    Preconditions.checkNotNull(gameId);
    return httpGetter.get(
        "move",
        MoveRequest.newBuilder()
            .setGameInfo(GameInfo.newBuilder()
                .setSessionToken(sessionToken)
                .setGameId(gameId))
            .setMove(MoveProto.newBuilder().setFrom(from).setTo(to)).build(),
        ErrorResponse.class);
  }

  /** Calls a web-service method that takes GameInfo as the request. */
  public <T extends Message> T callSimpleMethod(
      String methodName, Class<T> type) {
    Preconditions.checkNotNull(sessionToken);
    Preconditions.checkNotNull(gameId);
    return httpGetter.get(
        methodName,
        GameInfo.newBuilder()
            .setSessionToken(sessionToken)
            .setGameId(gameId).build(),
        type);
  }

  /** Sends a resignation to the server. */
  public ErrorResponse resign() {
    return callSimpleMethod("resign", ErrorResponse.class);
  }

  /**
   * Sends a draw offer or draw acceptence to the server.
   * On the client side, a draw offer and accepting a draw is the same action.
   * If both players performed this action, draw is agreed.
   */
  public ErrorResponse offerOrAcceptDraw() {
    return callSimpleMethod("offerDraw", ErrorResponse.class);
  }

  /** Sends a drew offer decline to the server. */
  public ErrorResponse declineDrawOffer() {
    return callSimpleMethod("declineDrawOffer", ErrorResponse.class);
  }

  /** Asks the server to send the current position. */
  public PositionResponse getPosition() {
    return callSimpleMethod("getPosition", PositionResponse.class);
  }

  /** Starts listening to game events. */
  public void startListeningToEvents(final GameEventHandler handler) {
    if (eventsListenerThread != null) {
      return;
    }
    Preconditions.checkNotNull(sessionToken);
    Preconditions.checkNotNull(gameId);
    GameEventHandler handlerWithEndGame = new GameEventHandler() {
      @Override
      public void handle(GameEvent event) {
        if (event.getType() == Type.GAME_ENDED) {
          stopListeningToEvents();
        }
        handler.handle(event);
      }
    };
    eventsListenerThread = new EventsListenerThread(
        baseUrl, sessionToken, gameId, handlerWithEndGame, logger);
    eventsListenerThread.start();
  }

  /** Stops listening to game events. */
  public void stopListeningToEvents() {
    if (eventsListenerThread == null) {
      return;
    }
    eventsListenerThread.stopListening();
    eventsListenerThread = null;
  }
}
