package com.gilran.chess.board;

public class Game {
  /** The players. */
  private String whitePlayer;
  private String blackPlayer;
  /** The game position. */
  private Position position;

  public Game(String whitePlayer, String blackPlayer) {
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.position = new Position();
  }

  public String getPlayer(Piece.Color color) {
    return color == Piece.Color.WHITE ? getWhitePlayer() : getBlackPlayer();
  }
  public String getWhitePlayer() {
    return whitePlayer;
  }
  public String getBlackPlayer() {
    return blackPlayer;
  }
  public Position getPosition() {
    return position;
  }
}
