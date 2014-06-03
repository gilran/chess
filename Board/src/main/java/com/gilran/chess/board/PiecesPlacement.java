package com.gilran.chess.board;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The placement of pieces on the board.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
/* package */ class PiecesPlacement
    implements Iterable<PiecesPlacement.PlacementEntry> {
  /** A map from a coordinate to the piece that occupies it. */
  private Map<Coordinate, Piece> coordinateToPiece;

  /**
   * A map from a piece to the coordinates that are occupied by that piece.
   */
  private Map<Piece, Set<Coordinate>> pieceToCoordinates;

  /** A single piece placement. */
  public static class PlacementEntry {
    private Piece piece;
    private Coordinate coordinate;

    public PlacementEntry(Piece piece, Coordinate coordinate) {
      this.piece = piece;
      this.coordinate = coordinate;
    }

    public Piece getPiece() { return piece; }
    public Coordinate getCoordinate() { return coordinate; }
  }

  /** An iterator over the PiecesPlacement. */
  public static class PlacementEntryIterator
      implements Iterator<PlacementEntry> {
    private Iterator<Entry<Coordinate, Piece>> internalIterator;

    public PlacementEntryIterator(PiecesPlacement piecePlacement) {
      internalIterator = piecePlacement.coordinateToPiece.entrySet().iterator();
    }

    public boolean hasNext() {
      return internalIterator.hasNext();
    }

    public PlacementEntry next() {
      Entry<Coordinate, Piece> entry = internalIterator.next();
      return new PlacementEntry(entry.getValue(), entry.getKey());
    }

    public void remove() {
      assert false;
    }
  }

  /** Creates a piece placement with no pieces. */
  public PiecesPlacement() {
    coordinateToPiece = Maps.newHashMap();
    pieceToCoordinates = Maps.newHashMap();
  }

  /** Copy constructor. */
  public PiecesPlacement(PiecesPlacement other) {
    coordinateToPiece = Maps.newHashMap(other.coordinateToPiece);
    pieceToCoordinates = Maps.newHashMap(other.pieceToCoordinates);
  }

  /** Returns an iterator. */
  public Iterator<PlacementEntry> iterator() {
    return new PlacementEntryIterator(this);
  }

  /**
   * Returns the piece at the given coordinate, or null if the coordinate is
   * not occupied.
   */
  public Piece at(Coordinate coordinate) {
    return coordinateToPiece.get(coordinate);
  }

  /** Returns true iff the given coordinate is occupied. */
  public boolean isOccupied(Coordinate coordinate) {
    return at(coordinate) != null;
  }

  /**
   * Returns true iff at least one of the coordinates in the list is occupied
   * by a piece.
   */
  public boolean anyOccupied(Iterable<Coordinate> coordinates) {
    for (Coordinate coordinate : coordinates) {
      if (isOccupied(coordinate)) {
        return true;
      }
    }
    return false;
  }

  /** Adds the given piece at the given coordinate. */
  public void add(Piece piece, Coordinate coordinate) {
    assert !isOccupied(coordinate);
    coordinateToPiece.put(coordinate, piece);
    Set<Coordinate> pieceCoordinates = pieceToCoordinates.get(piece);
    if (pieceCoordinates == null) {
      pieceCoordinates = Sets.newHashSet();
      pieceToCoordinates.put(piece, pieceCoordinates);
    }
    pieceCoordinates.add(coordinate);
  }

  /** Removes the given pieces from the given coordinate and returns it. */
  public Piece remove(Coordinate coordinate) {
    Piece piece = coordinateToPiece.remove(coordinate);
    pieceToCoordinates.get(piece).remove(coordinate);
    return piece;
  }

  /** Moves a pieces from the from-coordinate to the to-coordinate. */
  public void move(Coordinate from, Coordinate to) {
    add(remove(from), to);
  }
}

