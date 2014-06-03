package com.gilran.chess.board;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSortedSet;

import java.util.Collections;
import java.util.Set;

/**
 * A move on the chess board.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Move {
  /** The coordinate from and to which the piece moves. */
  private Coordinate from;
  private Coordinate to;

  /**
   * The coordinate that would be captured when this move is performed.
   * May be null, in which case, the move is not a capture.
   */
  private Coordinate capture;

  /**
   * A list of coordinates that must be unoccupied in order to make this move.
   */
  private Set<Coordinate> unoccupied;

  /**
   * A list of coordinates that must be unthreatened in order to make this move.
   */
  private Set<Coordinate> unthreatened;

  /** Is the move only legal as a capture? */
  private boolean captureOnly;

  /** Castling side of the move (null if this is not a castling move). */
  private CastlingRights.Side castlingSide;

  /**
   * The en passant target created by this move.
   * May be null, in which case the move does not create an en passant target.
   */
  private Coordinate enPassantTarget;

  /**
   * The piece to which a paw wass promoted in the move.
   * May be null, in which case the move was not a pawn promotion.
   */
  private Piece.Type promotionPiece;

  /** Constructor. */
  public Move(Coordinate from, Coordinate to) {
    this(
        from,
        to,
        to /* capture */,
        null /* unoccupied */,
        null /* unthreatened */,
        null /* castlingSide */,
        false /* captureOnly */,
        null /* enPassantTarget */);
  }

  /** Constructor that fit moves of most pieces. */
  public Move(Coordinate from, Coordinate to, Set<Coordinate> unoccupied) {
    this(
        from,
        to,
        to /* capture */,
        unoccupied,
        null /* unthreatened */,
        null /* castlingSide */,
        false /* captureOnly */,
        null /* enPassantTarget */);
  }

  /** Constructor that fits pawn moves. */
  public Move(
      Coordinate from,
      Coordinate to,
      Coordinate capture,
      Set<Coordinate> unoccupied,
      boolean captureOnly,
      Coordinate enPassantTarget) {
    this(
        from,
        to,
        capture,
        unoccupied,
        null /* unthreatened */,
        null /* castlingSide */,
        captureOnly,
        enPassantTarget);
  }

  /** Constructor that fits king moves. */
  public Move(
      Coordinate from,
      Coordinate to,
      Set<Coordinate> unoccupied,
      Set<Coordinate> unthreatened,
      CastlingRights.Side castlingSide) {
    this(
        from,
        to,
        castlingSide == null ? to : null /* capture */,
        unoccupied,
        unthreatened,
        castlingSide,
        false /* captureOnly */,
        null /* enPassantTarget */);
  }

  /** Constructor with all the options. */
  public Move(
      Coordinate from,
      Coordinate to,
      Coordinate capture,
      Set<Coordinate> unoccupied,
      Set<Coordinate> unthreatened,
      CastlingRights.Side castlingSide,
      boolean captureOnly,
      Coordinate enPassantTarget) {
    this.from = from;
    this.to = to;
    this.capture = capture;
    this.unoccupied = unoccupied == null
        ? Collections.<Coordinate>emptySet()
        : ImmutableSortedSet.copyOf(unoccupied);
    this.unthreatened = unthreatened == null
        ? Collections.<Coordinate>emptySet()
        : ImmutableSortedSet.copyOf(unthreatened);
    this.captureOnly = captureOnly;
    this.castlingSide = castlingSide;
    this.enPassantTarget = enPassantTarget;
    this.promotionPiece = null;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    if (!(other instanceof Move)) {
      return false;
    }
    Move otherMove = (Move) other;
    return
      this.from == otherMove.from &&
      this.to == otherMove.to &&
      this.capture == otherMove.capture &&
      this.unoccupied.equals(otherMove.unoccupied) &&
      this.unthreatened.equals(otherMove.unthreatened) &&
      this.castlingSide == otherMove.castlingSide &&
      this.captureOnly == otherMove.captureOnly &&
      this.enPassantTarget == otherMove.enPassantTarget;
  }

  public String toString() {
    if (getCastlingSide() == CastlingRights.Side.KING) {
      return "O-O";
    }
    if (getCastlingSide() == CastlingRights.Side.QUEEN) {
      return "O-O-O";
    }

    StringBuilder builder = new StringBuilder(5);
    builder.append(from.toString());
    builder.append(capture == null ? "-" : "x");
    builder.append(to.toString());

    // TODO(gilran): Remove this - this is for debug.
    builder.append(" (");
    builder.append(Joiner.on(", ").join(unoccupied));
    builder.append(")");

    return builder.toString();

    // TODO(gilran): Handle pawn promotion.
  }

  public Coordinate getFrom() { return from; }
  public void setFrom(Coordinate coordinate) { from = coordinate; }

  public Coordinate getTo() { return to; }
  public void setTo(Coordinate coordinate) { to = coordinate; }

  public Coordinate getCapture() { return capture; }
  public void setCapture(Coordinate coordinate) { capture = coordinate; }

  public Set<Coordinate> getUnoccupied() { return unoccupied; }
  public void setUnoccupied(Set<Coordinate> unoccupied) {
    this.unoccupied = ImmutableSortedSet.copyOf(unoccupied);
  }

  public Set<Coordinate> getUnthreatened() { return unthreatened; }
  public void setUnthreatened(Set<Coordinate> unthreatened) {
    this.unthreatened = ImmutableSortedSet.copyOf(unthreatened);
  }

  public boolean isCaptureOnly() { return captureOnly; }
  public void setCaptureOnly(boolean captureOnly) {
    this.captureOnly = captureOnly;
  }

  public CastlingRights.Side getCastlingSide() { return castlingSide; }
  public void setCastlingSide(CastlingRights.Side side) {
    this.castlingSide = side;
  }

  public Coordinate getEnPassantTarget() { return enPassantTarget; }
  public void setEnPassantTarget(Coordinate target) {
    this.enPassantTarget = target;
  }

  public Piece.Type getPromotionPiece() { return promotionPiece; }
  public void setPromotionPiece(Piece.Type promotionPiece) {
    this.promotionPiece = promotionPiece;
  }
}
