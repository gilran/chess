package com.gilran.chess.server;

import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Move;
import com.gilran.chess.board.Piece.Color;
import com.gilran.chess.board.Position;
import com.gilran.chess.Proto.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Game {
  /** The game id. */
  private String id;
  /** The players. */
  private String whitePlayer;
  private String blackPlayer;
  /** The game position. */
  private Position position;
  /** The game events. */
  private List<GameEvent> events;
  private Multimap<Integer, EventsCallback> pendingEventCallbaks;

  public Game(String whitePlayer, String blackPlayer) {
    this.id = UUID.randomUUID().toString();
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.position = new Position();
    this.events = Lists.newArrayList();
    this.pendingEventCallbaks = ArrayListMultimap.create();
  }

  public String getId() { return id; }
  public String getWhitePlayer() { return whitePlayer; }
  public String getBlackPlayer() { return blackPlayer; }
  public Position getPosition() { return position; }
  
  public synchronized GameEvent addEvent(GameEvent.Builder eventBuilder) {
    eventBuilder.setSerialNumber(events.size());
    GameEvent event = eventBuilder.build();
    events.add(event);
    Collection<EventsCallback> eventCallbacks =
        pendingEventCallbaks.get(event.getSerialNumber());
    if (eventCallbacks != null) {
      List<GameEvent> eventsList = ImmutableList.of(event);
      for (EventsCallback callback : eventCallbacks)
        callback.run(eventsList);
    }
    return event;
  }
  
  public interface EventsCallback {
    void run(List<GameEvent> events);
  }
  public synchronized void getEvents(int minEvent, EventsCallback callback) {
    if (minEvent >= events.size()) {
      pendingEventCallbaks.put(minEvent, callback);
      return;
    }
    callback.run(events.subList(minEvent, events.size()));
  }
  
  public Status move(Color playerColor, String from, String to) {
    Coordinate fromCoordinate = Coordinate.get(from);
    Coordinate toCoordinate = Coordinate.get(to);
    if (from == null || to == null)
      return Status.INVALID_MOVE;
    if (position.getActivePlayer() != playerColor)
      return Status.NOT_YOUR_TURN;
    List<Move> moves = position.move(fromCoordinate, toCoordinate);
    if (moves.isEmpty())
      return Status.ILLEGAL_MOVE;
    
    GameEvent.Builder eventBuilder = GameEvent.newBuilder();
    eventBuilder.setType(GameEvent.Type.MOVE_MADE);
    eventBuilder.setStatus(position.getStatus());
    for (Move move : moves) {
      eventBuilder.addMove(MoveProto.newBuilder()
          .setFrom(move.getFrom().name())
          .setTo(move.getTo().name()));
    }
    addEvent(eventBuilder);
    
    switch (position.getStatus()) {
      case BLACK_CHECKMATED:
      case BLACK_STALEMATED:
      case WHITE_CHECKMATED:
      case WHITE_STALEMATED:
      case HALFMOVE_CLOCK_EXPIRED:
      case INSUFFICIENT_MATERIAL:
      case THREEFOLD_REPETITION:
        addEvent(GameEvent.newBuilder()
            .setType(GameEvent.Type.GAME_ENDED)
            .setStatus(position.getStatus()));
      default:
        break;
    }
    
    return Status.OK;
  }
}
