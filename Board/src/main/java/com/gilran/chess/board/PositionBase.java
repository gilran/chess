package com.gilran.chess.board;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * A base class for chess position and position-like (such as annotations)
 * classes.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
/* package */ class PositionBase {
  /** The pieces placement. */
  protected PiecesPlacement piecesPlacement;

  /** The player whos turn it is to make a move. */
  protected Piece.Color activePlayer;

  /**
   * The castling rights of each side.
   * For each side, the inner map will have KING_SIDE and QUEEN_SIDE as keys,
   * and a boolean indicating if the side is holding the right to castle in that
   * direction.
   */
  protected CastlingRights castlingRights;

  /**
   * The number of halfmoves since the last capture or pawn advance. This is
   * used to determine if a draw can be claimed under the fifty-move rule.
   */
  protected int halfMovesClock;

  /**
   * The target square for an en passant capture. If null, the last move was not
   * a 2 squares pawn advance.
   */
  protected Coordinate enPassantTarget;

  /**
   * The number of the current (full) move. A game starts at move 1 (so this is
   * 1-based).
   */
  protected int currentMove;

  /** The positions of the kings. This is used to test for checks. */
  protected Map<Piece.Color, Coordinate> kingPosition;

  /** Default constructor. */
  protected PositionBase() {
    piecesPlacement = new PiecesPlacement();
    activePlayer = Piece.Color.WHITE;
    castlingRights = new CastlingRights();
    halfMovesClock = 0;
    enPassantTarget = null;
    currentMove = 1;
    kingPosition = null;
  }

  /** Copy constructor. */
  protected PositionBase(PositionBase other) {
    piecesPlacement = other.getPiecesPlacement();
    activePlayer = other.getActivePlayer();
    castlingRights = other.getCastlingRights();
    halfMovesClock = other.getHalfMovesClock();
    enPassantTarget = other.getEnPassantTarget();
    currentMove = other.getCurrentMove();
    kingPosition = other.getKingPosition();
  }

  /**
   * Retruns a copy of the pieces placement.
   * <p>A copy is returned in order to prevent clients from changing the
   * position's internal pieces placement.
   * */
  public PiecesPlacement getPiecesPlacement() {
    return new PiecesPlacement(piecesPlacement);
  }

  /** Returns the color whos turn it is to make a move. */
  public Piece.Color getActivePlayer() { return activePlayer; }

  /**
   * Returns a copy of the castling rights.
   * <p>A copy is returned in order to prevent clients from changing the
   * position's internal castling rights.
   */
  /* package */ CastlingRights getCastlingRights() {
    return new CastlingRights(castlingRights);
  }

  /** Returns the number of halfmoves since the last capture or pawn advance. */
  /* package */ int getHalfMovesClock() { return halfMovesClock; }

  /**
   * Returns the target square for an en passant capture. If null, the last move
   * was not a 2 squares pawn advance.
   */
  /* package */ Coordinate getEnPassantTarget() { return enPassantTarget; }

  /**
   * Returns the number of the current (full) move. A game starts at move 1 (so
   * this is 1-based).
   */
  /* package */ int getCurrentMove() { return currentMove; }

  /**
   * Returns a copy of the king position map.
   * <p>A copy is returned in order to prevent clients from changing the
   * position's internal king position map.
   */
  /* package */ Map<Piece.Color, Coordinate> getKingPosition() {
    return Maps.newEnumMap(kingPosition);
  }

  /** Returns the piece at the given coordinate, or null if there isn't one. */
  /* package */ Piece at(Coordinate coordinate) {
    return piecesPlacement.at(coordinate);
  }
}
