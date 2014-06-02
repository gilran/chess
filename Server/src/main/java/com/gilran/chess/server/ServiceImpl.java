package com.gilran.chess.server;

import java.util.List;
import java.util.Map;
import java.util.Random;

import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Move;
import com.gilran.chess.board.Position;
import com.gilran.chess.board.Piece.Color;
import com.gilran.chess.Proto.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;

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
     * @param The response proto message.
     */
    void Run(Message response);
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
      seek1.callback.Run(response);
      seek2.callback.Run(response);
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
    callback.Run(LoginResponse.newBuilder()
        .setSessionToken(session.getToken())
        .build());
    return Status.OK;
  }

  public synchronized Status seek(SeekRequest request, Callback callback) {
    Session session = sessions.get(request.getSessionToken());
    if (session == null)
      return Status.INVALID_OR_EXPIRED_SESSION_TOKEN;
    
    PendingSeek currentSeek = new PendingSeek(session, callback);
    
    if (pendingSeek == null) {
      pendingSeek = currentSeek;
      return Status.OK;
    }

    PendingSeek.match(currentSeek, pendingSeek);
    pendingSeek = null;
    return Status.OK;
  }

  public Status move(MoveRequest request, Callback callback) {
    Session session = sessions.get(request.getSessionToken());
    if (session == null)
      return Status.INVALID_OR_EXPIRED_SESSION_TOKEN;
    
    Game game = session.getGame(request.getGameId());
    if (game == null)
      return Status.INVALID_GAME_ID;
    
    Color playerColor = session.getUsername() == game.getWhitePlayer()
        ? Color.WHITE : Color.BLACK;
    game.move(
        playerColor, request.getMove().getFrom(), request.getMove().getTo());
    callback.Run(MoveResponse.newBuilder().build());
    
    return Status.OK;
  }
  
  public Status getEvents(EventsRequest request, final Callback callback) {
    Session session = sessions.get(request.getSessionToken());
    if (session == null)
      return Status.INVALID_OR_EXPIRED_SESSION_TOKEN;
    
    Game game = session.getGame(request.getGameId());
    if (game == null)
      return Status.INVALID_GAME_ID;

    game.getEvents(request.getMinEventNumber(), new Game.EventsCallback() {
      @Override
      public void run(List<GameEvent> events) {
        callback.Run(EventsResponse.newBuilder()
            .addAllEvent(events)
            .build());
      }
    });
    return Status.OK;
  }
}

