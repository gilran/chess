package com.gilran.chess.server;

import com.gilran.chess.board.Position;
import java.util.UUID;

public class Game {
  /** The game id. */
  private String id;
  /** The players. */
  private String whitePlayer;
  private String blackPlayer;
	/** The game position. */
  private Position position;

  public Game(String whitePlayer, String blackPlayer) {
		this.id = UUID.randomUUID().toString();
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.position = new Position();
  }

  public String getId() { return id; }
  public String getWhitePlayer() { return whitePlayer; }
  public String getBlackPlayer() { return blackPlayer; }
  public Position getPosition() { return position; }
}
