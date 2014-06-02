package com.gilran.chess.server;

import com.gilran.chess.board.Position;
import com.gilran.chess.Proto.*;
import com.google.common.collect.Lists;

import java.util.Collections;
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
  List<GameEvent> events;

  public Game(String whitePlayer, String blackPlayer) {
		this.id = UUID.randomUUID().toString();
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.position = new Position();
    this.events = Lists.newArrayList();
  }

  public String getId() { return id; }
  public String getWhitePlayer() { return whitePlayer; }
  public String getBlackPlayer() { return blackPlayer; }
  public Position getPosition() { return position; }
  
  public synchronized GameEvent addEvent(GameEvent.Builder eventBuilder) {
  	eventBuilder.setSerialNumber(events.size());
  	GameEvent event = eventBuilder.build();
  	events.add(event);
  	return event;
  }
  
  public synchronized List<GameEvent> getEvents(int minEvent) {
  	if (minEvent >= events.size())
  		return Collections.emptyList();
  	return events.subList(minEvent, events.size());
  }
}
