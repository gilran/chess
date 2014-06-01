package com.gilran.chess.board;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

/**
 * A Forsyth-Edwards Notation (FEN) of a chess position.
 * <p>See <a href=http://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation>
 * Forsyth-Edwards Notation on wikipedia</a>.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class ForsythEdwardsNotation extends PositionBase {
  /** A map from piece to its name in FEN. */
  private static  ImmutableMap<Piece, Character> PIECE_TO_NAME;
  /** A map from a FEN piece name to the piece. */
  private static final ImmutableMap<Character, Piece> NAME_TO_PIECE;
  /** A map form a player color to its name in FEN. */
  private static final ImmutableMap<Piece.Color, String> COLOR_TO_NAME;
  /** A map from a FEN color name to the color. */
  private static final ImmutableMap<String, Piece.Color> NAME_TO_COLOR;
  /** A map from castling rights to their names in FEN. */
  private static final ImmutableMap<CastlingRights.Castle, Character>
  		CASTLE_TO_NAME;
  /** A map from FEN castling move name to the castling. */
  private static final ImmutableMap<Character, CastlingRights.Castle>
      NAME_TO_CASTLE;
  /** The starting position of a standard chess game. */
  public static final ForsythEdwardsNotation STARTING_POSITION;
  
  /** An exception that indicates that a FEN string is invalid. */
  public static class InvalidFENStringException extends Exception {
    private static final long serialVersionUID = 3178735674293499971L;
  }
  
  static {
    PIECE_TO_NAME =
        ImmutableMap.<Piece, Character>builder()
        .put(Piece.get(Piece.Type.PAWN, Piece.Color.WHITE), 'P')
        .put(Piece.get(Piece.Type.ROOK, Piece.Color.WHITE), 'R')
        .put(Piece.get(Piece.Type.KNIGHT, Piece.Color.WHITE), 'N')
        .put(Piece.get(Piece.Type.BISHOP, Piece.Color.WHITE), 'B')
        .put(Piece.get(Piece.Type.QUEEN, Piece.Color.WHITE), 'Q')
        .put(Piece.get(Piece.Type.KING, Piece.Color.WHITE), 'K')
        .put(Piece.get(Piece.Type.PAWN, Piece.Color.BLACK), 'p')
        .put(Piece.get(Piece.Type.ROOK, Piece.Color.BLACK), 'r')
        .put(Piece.get(Piece.Type.KNIGHT, Piece.Color.BLACK), 'n')
        .put(Piece.get(Piece.Type.BISHOP, Piece.Color.BLACK), 'b')
        .put(Piece.get(Piece.Type.QUEEN, Piece.Color.BLACK), 'q')
        .put(Piece.get(Piece.Type.KING, Piece.Color.BLACK), 'k')
        .build();
    NAME_TO_PIECE = createReverseMap(PIECE_TO_NAME);
    
    COLOR_TO_NAME =
        ImmutableMap.<Piece.Color, String>builder()
        .put(Piece.Color.WHITE, "w")
        .put(Piece.Color.BLACK, "b")
        .build();
    NAME_TO_COLOR = createReverseMap(COLOR_TO_NAME);
    
    CASTLE_TO_NAME = ImmutableMap.<CastlingRights.Castle, Character>builder()
        .put(CastlingRights.Castle.get(
              Piece.Color.WHITE, CastlingRights.Side.KING), 'K')
        .put(CastlingRights.Castle.get(
              Piece.Color.WHITE, CastlingRights.Side.QUEEN), 'Q')
        .put(CastlingRights.Castle.get(
              Piece.Color.BLACK, CastlingRights.Side.KING), 'k')
        .put(CastlingRights.Castle.get(
              Piece.Color.BLACK, CastlingRights.Side.QUEEN), 'q')
        .build();
    NAME_TO_CASTLE = createReverseMap(CASTLE_TO_NAME);
    
    ForsythEdwardsNotation tmpStartingPosition = null;
    try {
      tmpStartingPosition = new ForsythEdwardsNotation(
          "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    } catch (InvalidFENStringException e) {
      assert false;
    }
    STARTING_POSITION = tmpStartingPosition;
  }

  /** The notation string. */
  private String fenString;

  /** Creates the FEN of the given position. */
  public ForsythEdwardsNotation(Position position) {
    super(position);

    fenString = Joiner.on(" ").join(ImmutableList.<String>of(
          piecePlacementString(),
          COLOR_TO_NAME.get(getActivePlayer()),
          castlingRightsString(),
          getEnPassantTarget() == null ? "-" : getEnPassantTarget().name(),
          String.valueOf(getHalfMovesClock()),
          String.valueOf(getCurrentMove())));
  }

  /** Creates a FEN object from the given FEN string. */
  public ForsythEdwardsNotation(String fenString)
      throws InvalidFENStringException {
    this.fenString = fenString;

    String[] fenParts = fenString.split(" ");
    if (fenParts.length != 6) throw new InvalidFENStringException();

    parsePiecePlacement(fenParts[0]);

    activePlayer = NAME_TO_COLOR.get(fenParts[1]);
    if (activePlayer == null) throw new InvalidFENStringException();

    parseCastlingRights(fenParts[2]);

    if (fenParts[3].equals("-")) {
      enPassantTarget = null;
    } else {
      enPassantTarget = Coordinate.get(fenParts[3]);
      if (enPassantTarget == null) throw new InvalidFENStringException();
    }

    Integer tmpHalfMovesClock = Ints.tryParse(fenParts[4]);
    if (tmpHalfMovesClock == null) throw new InvalidFENStringException();
    halfMovesClock = tmpHalfMovesClock;

    Integer tmpCurrentMove = Ints.tryParse(fenParts[5]);
    if (tmpCurrentMove == null) throw new InvalidFENStringException();
    currentMove = tmpCurrentMove;
  }

  /** Returns the FEN string. */
  public String toString() { return fenString; }

  /**
   * Creates an immutable map with the keys of the given map as values, and the
   * values as keys.
   */
  // TODO(gilran): Seems reasonable that there would be some method is some
  //     standard library that does this. Find it and use it.
  static <K, V> ImmutableMap<V, K> createReverseMap(ImmutableMap<K, V> map) {
    ImmutableMap.Builder<V, K> builder = ImmutableMap.builder();
    for (Map.Entry<K, V> entry : map.entrySet()) {
      builder.put(entry.getValue(), entry.getKey());
    }
    return builder.build();
  }

  /** Returns the piece placement field of a FEN for the given position. */
  private String piecePlacementString() {
    ImmutableList.Builder<String> ranksBuilder = ImmutableList.builder();
    for (int rank = Coordinate.LAST_RANK;
         rank >= Coordinate.FIRST_RANK;
         rank--) {
      ranksBuilder.add(rankPiecePlacement(rank));
    }
    return Joiner.on("/").join(ranksBuilder.build());
  }

  /**
   * Sets the piece placement according to the given FEN piece placement
   * string.
   */
  private void parsePiecePlacement(String placementString)
      throws InvalidFENStringException {
    piecesPlacement = new PiecesPlacement();
    kingPosition = Maps.newEnumMap(Piece.Color.class);
    String[] rankStrings = placementString.split("/");
    Preconditions.checkArgument(
        rankStrings.length == Coordinate.RANKS, "Invalid FEN string");
    for (int rank = Coordinate.FIRST_RANK;
         rank <= Coordinate.LAST_RANK;
         rank++) {
      parseRankPiecePlacement(Coordinate.LAST_RANK - rank, rankStrings[rank]);
    }
  }

  /**
   * Returns the piece placement of the pieces in the given rank of the position
   * in FEN format.
   */
  private String rankPiecePlacement(int rank) {
    int emptySquares = 0;
    StringBuilder builder = new StringBuilder(Coordinate.FILES /* max size */);
    for (int file = Coordinate.FIRST_FILE;
         file <= Coordinate.LAST_FILE;
         file++) {
      Piece piece = piecesPlacement.at(Coordinate.get(file, rank));
      if (piece == null) {
        emptySquares++;
        continue;
      }

      if (emptySquares != 0) {
        builder.append(String.valueOf(emptySquares));
        emptySquares = 0;
      }

      builder.append(PIECE_TO_NAME.get(piece));
    }
    if (emptySquares != 0)
    	builder.append(String.valueOf(emptySquares));
    return builder.toString();
  }

  /**
   * Sets the piece placement of the given rank according to the given FEN rank
   * piece placement string.
   */
  private void parseRankPiecePlacement(int rank, String placementString)
      throws InvalidFENStringException {
    int file = Coordinate.FIRST_FILE;
    for (int i = 0; i < placementString.length(); i++) {
      if (file > Coordinate.LAST_FILE)
        throw new InvalidFENStringException();

      Character currentChar = placementString.charAt(i);
      Piece piece = NAME_TO_PIECE.get(currentChar);
      if (piece == null) {
        if (!Character.isDigit(currentChar))
          throw new InvalidFENStringException();
        file += Character.digit(currentChar, 10);
      } else {
        Coordinate coordinate = Coordinate.get(file, rank);
        piecesPlacement.add(piece, coordinate);
        if (piece.getType() == Piece.Type.KING) {
          kingPosition.put(piece.getColor(), coordinate);
        }
        file++;
      }
    }
    if (file != Coordinate.FILES)
      throw new InvalidFENStringException();
  }

  /** Returns the castling rights field of a FEN for the given postion. */
  private String castlingRightsString() {
    StringBuilder builder = new StringBuilder(4 /* max size */);
    for (Piece.Color color : Piece.Color.values()) {
      for (CastlingRights.Side side : CastlingRights.Side.values()) {
        if (castlingRights.get(color, side)) {
          builder.append(
              CASTLE_TO_NAME.get(CastlingRights.Castle.get(color, side)));
        }
      }
    }
    return builder.length() == 0 ? "-" : builder.toString();
  }

  /**
   * Sets the castling rights according to the given FEN castling rights string.
   */
  private void parseCastlingRights(String rightsString)
      throws InvalidFENStringException{
    castlingRights = new CastlingRights(false);

    if (rightsString == "-") return;

    for (int i = 0; i < rightsString.length(); i++) {
      CastlingRights.Castle castle = NAME_TO_CASTLE.get(rightsString.charAt(i));
      if (castle == null) throw new InvalidFENStringException();
      castlingRights.set(castle, true);
    }
  }
}
