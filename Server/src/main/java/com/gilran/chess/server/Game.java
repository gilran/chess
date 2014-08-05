package com.gilran.chess.server;

import com.gilran.chess.Proto.*;
import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Move;
import com.gilran.chess.board.Piece;
import com.gilran.chess.board.Piece.Color;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A chess game managed by the server.
 *
 * <p>The game adds information specific to managing a game by the server to the
 * information in chess.board.Game.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Game extends com.gilran.chess.board.Game {
  /** The game id. */
  private String id;
  /** The game events. */
  private List<GameEvent> events;
  /** Callbacks for pending getEvent calls. */
  private Multimap<Integer, EventsCallback> pendingEventCallbaks;

  /**
   * Constructor.
   *
   * @param whitePlayer The name of the player playing white.
   * @param blackPlayer The name of the player playing black.
   */
  public Game(String whitePlayer, String blackPlayer) {
    super(whitePlayer, blackPlayer);
    this.id = UUID.randomUUID().toString();
    this.events = Lists.newArrayList();
    this.pendingEventCallbaks = ArrayListMultimap.create();
  }

  /** Returns the game id. */
  public String getId() {
    return id;
  }

  /**
   * Adds an event to the game.
   *
   * <p>In order to prevent corruption of the game events list, the method is
   * synchronized.
   */
  public synchronized GameEvent addEvent(GameEvent.Builder eventBuilder) {
    eventBuilder.setSerialNumber(events.size());
    GameEvent event = eventBuilder.build();
    events.add(event);
    Collection<EventsCallback> eventCallbacks =
        pendingEventCallbaks.get(event.getSerialNumber());
    if (eventCallbacks != null) {
      List<GameEvent> eventsList = ImmutableList.of(event);
      for (EventsCallback callback : eventCallbacks) {
        callback.run(eventsList);
      }
    }
    return event;
  }

  /** A callback that takes game events. */
  public interface EventsCallback {
    void run(List<GameEvent> events);
  }

  /**
   * Calls the callback with the game events with serial number >= minEvent.
   *
   * <p>If there are no events with serial number >= minEvent, the callback is
   * kept until a new event is available, and calls the callback with the new
   * event.
   */
  public synchronized void getEvents(int minEvent, EventsCallback callback) {
    if (minEvent >= events.size()) {
      pendingEventCallbaks.put(minEvent, callback);
      return;
    }
    callback.run(events.subList(minEvent, events.size()));
  }

  /** Performs a move in the game. */
  public Status move(Color playerColor, String from, String to) {
    Coordinate fromCoordinate = Coordinate.get(from);
    Coordinate toCoordinate = Coordinate.get(to);
    if (from == null || to == null) {
      return Status.INVALID_MOVE;
    }
    if (getPosition().getActivePlayer() != playerColor) {
      return Status.NOT_YOUR_TURN;
    }
    List<Move> moves = getPosition().move(fromCoordinate, toCoordinate);
    if (moves.isEmpty()) {
      return Status.ILLEGAL_MOVE;
    }

    GameEvent.Builder eventBuilder = GameEvent.newBuilder();
    eventBuilder.setType(GameEvent.Type.MOVE_MADE);
    eventBuilder.setStatus(getPosition().getStatus());
    for (Move move : moves) {
      eventBuilder.addMove(MoveProto.newBuilder()
          .setFrom(move.getFrom().name())
          .setTo(move.getTo().name()));
    }
    addEvent(eventBuilder);

    switch (getPosition().getStatus()) {
      case BLACK_CHECKMATED:
      case BLACK_STALEMATED:
      case WHITE_CHECKMATED:
      case WHITE_STALEMATED:
      case HALFMOVE_CLOCK_EXPIRED:
      case INSUFFICIENT_MATERIAL:
      case THREEFOLD_REPETITION:
        addEvent(GameEvent.newBuilder()
            .setType(GameEvent.Type.GAME_ENDED)
            .setStatus(getPosition().getStatus()));
        break;
      default:
        break;
    }

    return Status.OK;
  }

  /** Applies game resignation by the given player. */
  public void resign(Piece.Color playerColor) {
    getPosition().setStatus(
        playerColor == Piece.Color.WHITE
            ? GameStatus.WHITE_RESIGNED
            : GameStatus.BLACK_RESIGNED);
    addEvent(GameEvent.newBuilder()
        .setType(GameEvent.Type.GAME_ENDED)
        .setStatus(getPosition().getStatus()));
  }

  /**
   * Adds a draw offer by the given player.
   *
   * <p>If draw offers by both players are added, the game ends due to draw by
   * agreement.
   */

  public void addDrawOffer(Piece.Color playerColor) {
    Piece.Color drawOffer = getOutstandingDrawOffer();
    GameEvent.Type eventType = null;
    if (drawOffer == null) {
      // No outstanding offer. This is an offer.
      setOutstandingDrawOffer(playerColor);
      eventType = playerColor == Piece.Color.WHITE
          ? GameEvent.Type.WHITE_OFFERED_DRAW
          : GameEvent.Type.BLACK_OFFERED_DRAW;
    } else if (drawOffer == playerColor) {
      // There is an outstanding offer from this player. Nothing needs to be
      // done.
      return;
    } else if (drawOffer != playerColor) {
      // There is an outstanding offer from the other player - accepting the
      // draw.
      getPosition().setStatus(GameStatus.DRAW_BY_AGREEMENT);
      eventType = GameEvent.Type.GAME_ENDED;
    }
    addEvent(GameEvent.newBuilder()
        .setType(Preconditions.checkNotNull(eventType))
        .setStatus(getPosition().getStatus()));
  }

  /** Removes an existing draw offer. */
  public void clearDrawOffer(Piece.Color playerColor) {
    if (getOutstandingDrawOffer() == null) {
      // There is no outstanding draw offer. Nothing needs to be done.
      return;
    }

    GameEvent.Type eventType =
        playerColor == getOutstandingDrawOffer()
            ? GameEvent.Type.DRAW_OFFER_WITHDRAWN
            : GameEvent.Type.DRAW_OFFER_DECLINED;
    setOutstandingDrawOffer(null);

    addEvent(GameEvent.newBuilder()
        .setType(eventType)
        .setStatus(getPosition().getStatus()));
  }
}
