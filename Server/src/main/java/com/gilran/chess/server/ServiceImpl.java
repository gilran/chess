package com.gilran.chess.server;

import com.gilran.chess.Proto.*;
import com.gilran.chess.board.ForsythEdwardsNotation;
import com.gilran.chess.board.Piece;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The chess service implementation.
 *
 * <p>Each public method in this class is mapped to the corresponding path of
 * the web service. The RequestHandler uses reflection in order to choose the
 * method.
 * <p>Due to this, each public method must have a Status return value and take
 * exactly 2 params, where the first is a proto message and the second is a
 * Callback.
 */
public class ServiceImpl {
  /** An interface for service methods callbacks. */
  public interface Callback {
    /**
     * Runs the callback.
     *
     * @param response The response proto message.
     */
    void run(Message response);
  }

  /** A class for a pending seek. */
  private static class PendingSeek {
    /** Random utility. */
    private static Random random = new Random();
    /** The seeking session. */
    private Session session;
    /** The seek request callback. */
    private Callback callback;

    public PendingSeek(Session session, Callback callback) {
      this.session = Preconditions.checkNotNull(session);
      this.callback = Preconditions.checkNotNull(callback);
    }

    public static void match(PendingSeek seek1, PendingSeek seek2) {
      boolean firstIsWhite = random.nextBoolean();
      String whitePlayer;
      String blackPlayer;
      if (firstIsWhite) {
        whitePlayer = seek1.session.getUsername();
        blackPlayer = seek2.session.getUsername();
      } else {
        whitePlayer = seek2.session.getUsername();
        blackPlayer = seek1.session.getUsername();
      }
      Game game = new Game(whitePlayer, blackPlayer);
      seek1.session.addGame(game);
      seek2.session.addGame(game);

      SeekResponse.Builder responseBuilder = SeekResponse.newBuilder();
      responseBuilder.setGameId(game.getId());
      responseBuilder.setWhite(whitePlayer);
      responseBuilder.setBlack(blackPlayer);
      SeekResponse response = responseBuilder.build();
      seek1.callback.run(response);
      seek2.callback.run(response);
    }
  }

  /** A map from session tokens to the sessions. */
  private Map<String, Session> sessions;
  /** A currently pending seek (null if there is no pending seek). */
  private PendingSeek pendingSeek;

  /** Constructs a new ServiceImpl. */
  public ServiceImpl() {
    sessions = Maps.newHashMap();
    pendingSeek = null;
  }

  public Status login(LoginRequest request, Callback callback) {
    Session session = new Session(request.getUsername());
    sessions.put(session.getToken(), session);
    callback.run(LoginResponse.newBuilder()
        .setSessionToken(session.getToken())
        .build());
    return Status.OK;
  }

  public synchronized Status seek(SeekRequest request, Callback callback) {
    Session session = sessions.get(request.getSessionToken());
    if (session == null) {
      return Status.INVALID_OR_EXPIRED_SESSION_TOKEN;
    }

    PendingSeek currentSeek = new PendingSeek(session, callback);

    if (pendingSeek == null) {
      pendingSeek = currentSeek;
      return Status.OK;
    }

    PendingSeek.match(currentSeek, pendingSeek);
    pendingSeek = null;
    return Status.OK;
  }
  
  private static class GameActionInfo {
    public Game game;
    Piece.Color playerColor;
    public Status status;
  }
  
  private <T> GameActionInfo getGameActionInfo(GameInfo gameInfo) {
    GameActionInfo gameActionInfo = new GameActionInfo();
    
    Session session = sessions.get(gameInfo.getSessionToken());
    if (session == null) {
      gameActionInfo.status = Status.INVALID_OR_EXPIRED_SESSION_TOKEN;
      return gameActionInfo;
    }
    
    Game game = session.getGame(gameInfo.getGameId());
    if (game == null) {
      gameActionInfo.status = Status.INVALID_GAME_ID;
      return gameActionInfo;
    }
    
    gameActionInfo.status = Status.OK;
    gameActionInfo.game = game;
    gameActionInfo.playerColor = session.getUsername() == game.getWhitePlayer()
        ? Piece.Color.WHITE : Piece.Color.BLACK;
    return gameActionInfo;
  }

  public Status move(MoveRequest request, Callback callback) {
    GameActionInfo gameActionInfo = getGameActionInfo(request.getGameInfo());
    if (gameActionInfo.status != Status.OK) {
      return gameActionInfo.status;
    }
    
    gameActionInfo.game.move(
        gameActionInfo.playerColor,
        request.getMove().getFrom(),
        request.getMove().getTo());
    callback.run(ErrorResponse.newBuilder().build());

    return Status.OK;
  }

  public Status getEvents(EventsRequest request, final Callback callback) {
    GameActionInfo gameActionInfo = getGameActionInfo(request.getGameInfo());
    if (gameActionInfo.status != Status.OK) {
      return gameActionInfo.status;
    }

    gameActionInfo.game.getEvents(
        request.getMinEventNumber(), new Game.EventsCallback() {
      @Override
      public void run(List<GameEvent> events) {
        callback.run(EventsResponse.newBuilder()
            .addAllEvent(events)
            .build());
      }
    });
    return Status.OK;
  }
  
  public Status resign(GameInfo request, final Callback callback) {
    GameActionInfo gameActionInfo = getGameActionInfo(request);
    if (gameActionInfo.status != Status.OK) {
      return gameActionInfo.status;
    }

    gameActionInfo.game.resign(gameActionInfo.playerColor);
    callback.run(ErrorResponse.newBuilder().build());
    return Status.OK;
  }

  public Status offerDraw(GameInfo request, final Callback callback) {
    GameActionInfo gameActionInfo = getGameActionInfo(request);
    if (gameActionInfo.status != Status.OK) {
      return gameActionInfo.status;
    }

    gameActionInfo.game.addDrawOffer(gameActionInfo.playerColor);
    callback.run(ErrorResponse.newBuilder().build());
    return Status.OK;
  }

  public Status declineDrawOffer(GameInfo request, final Callback callback) {
    GameActionInfo gameActionInfo = getGameActionInfo(request);
    if (gameActionInfo.status != Status.OK) {
      return gameActionInfo.status;
    }

    gameActionInfo.game.clearDrawOffer(gameActionInfo.playerColor);
    callback.run(ErrorResponse.newBuilder().build());
    return Status.OK;
  }
  
  public Status getPosition(GameInfo request, final Callback callback) {
    GameActionInfo gameActionInfo = getGameActionInfo(request);
    if (gameActionInfo.status != Status.OK) {
      return gameActionInfo.status;
    }
    String fen = new ForsythEdwardsNotation(gameActionInfo.game.getPosition())
        .toString();
    callback.run(PositionResponse.newBuilder().setFen(fen).build());
    return Status.OK;
  }
}

