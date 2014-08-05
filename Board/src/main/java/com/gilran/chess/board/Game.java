package com.gilran.chess.board;

import com.gilran.chess.board.Piece.Color;

/**
 * A chess game.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Game {
  /** The player playing white. */
  private String whitePlayer;
  /** The player playing black. */
  private String blackPlayer;
  /** The game position. */
  private Position position;

  public Game(String whitePlayer, String blackPlayer) {
    this.whitePlayer = whitePlayer;
    this.blackPlayer = blackPlayer;
    this.position = new Position();
  }

  /** Returns the name of the player playing the given color. */
  public String getPlayer(Piece.Color color) {
    return color == Piece.Color.WHITE ? getWhitePlayer() : getBlackPlayer();
  }

  /** Returns the name of the player playing white. */
  public String getWhitePlayer() {
    return whitePlayer;
  }

  /** Returns the name of the player playing black. */
  public String getBlackPlayer() {
    return blackPlayer;
  }

  /** Returns the game position. */
  public Position getPosition() {
    return position;
  }

  /** Returns the player who offered draw, or null if there is no draw offer. */
  public Color getOutstandingDrawOffer() {
    return position.getOutstandingDrawOffer();
  }

  /** Sets the player who offered draw. */
  public void setOutstandingDrawOffer(Color outstandingDrawOffer) {
    position.setOutstandingDrawOffer(outstandingDrawOffer);
  }
}
