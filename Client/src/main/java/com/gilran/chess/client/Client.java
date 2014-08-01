package com.gilran.chess.client;

import com.gilran.chess.Proto.*;
import com.gilran.chess.Proto.GameEvent.Type;
import com.gilran.chess.client.Client.LoggerAdapter.Level;
import com.google.common.base.Preconditions;
import com.google.protobuf.Message;

public class Client {
  public interface LoggerAdapter {
    enum Level { DEBUG, INFO, WARNING, ERROR }
    void log(Level level, String message);
  }

  private LoggerAdapter logger;
  private String baseUrl;
  private String username;
  private String sessionToken;
  private String gameId;
  private EventsListenerThread eventsListenerThread;
  private HttpGetter httpGetter;

  public Client(String baseUrl, LoggerAdapter logger) {
    this.logger = logger == null ? new DefaultLogger() : logger;
    this.baseUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/");
    this.httpGetter = new HttpGetter(this.baseUrl, logger);
  }

  public Client(String baseUrl) {
    this(baseUrl, null);
  }

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

  public ErrorResponse resign() {
    return callSimpleMethod("resign", ErrorResponse.class);
  }

  public ErrorResponse offerOrAcceptDraw() {
    return callSimpleMethod("offerDraw", ErrorResponse.class);
  }

  public ErrorResponse declineDrawOffer() {
    return callSimpleMethod("declineDrawOffer", ErrorResponse.class);
  }
  public PositionResponse getPosition() {
    return callSimpleMethod("getPosition", PositionResponse.class);
  }

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

  public void stopListeningToEvents() {
    if (eventsListenerThread == null) {
      return;
    }
    eventsListenerThread.stopListening();
    eventsListenerThread = null;
  }
}
