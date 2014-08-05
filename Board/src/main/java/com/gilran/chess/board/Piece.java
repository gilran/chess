package com.gilran.chess.board;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;

/**
 * A chess piece.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public abstract class Piece {
  /** An enum of piece types. */
  public static enum Type { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }

  /**
   * An enum of piece colors.
   * <p>The order of elements in the enum is important as notation classes count
   * on this order when creating notation strings.
   * */
  public static enum Color { WHITE, BLACK }

  /** The type of the piece. */
  protected Type type;

  /** The color of the piece. */
  protected Color color;

  /**
   * A map of legal moves from each coordinate on the board.
   * <p>The list is pre-calculated, and kept as a member of each of the static
   * pieces. Having that there is a small amount of piece types (6), two colors,
   * and 64 coordinates on the board, this data is not very big (6*2*64=768
   * moves total). It seems better to pre-calculate it and consume more memory
   * than to perform for each move an action that is O(P*N), with P being the
   * number of pieces on the board and P being the length of one edge of the
   * board.
   */
  protected ImmutableMap<Coordinate, ImmutableList<Move>> movesMap;

  /**
   * Static map of all pieces.
   * <p>The number of pieces is small, and all pieces can be held in memoery at
   * all times. This would prevent duplicate instances of the same piece, and
   * would save some garbage collection.
   */
  private static final ImmutableMap<Color, ImmutableMap<Type, Piece>> PIECES;
  static {
    ImmutableMap.Builder<Color, ImmutableMap<Type, Piece>> piecesBuilder =
        ImmutableMap.builder();
    for (Color color : Color.values()) {
      ImmutableMap.Builder<Type, Piece> colorPiecesBuilder =
          ImmutableMap.builder();
      for (Type type : Type.values()) {
        colorPiecesBuilder.put(type, newPiece(type, color));
      }
      piecesBuilder.put(color, colorPiecesBuilder.build());
    }
    PIECES = piecesBuilder.build();
  }

  public static Piece get(Type type, Color color) {
    return PIECES.get(color).get(type);
  }

  /** Piece creator. */
  private static Piece newPiece(Type type, Color color) {
    switch (type) {
      case PAWN:   return new Pawn(color);
      case ROOK:   return new Rook(color);
      case KNIGHT: return new Knight(color);
      case BISHOP: return new Bishop(color);
      case QUEEN:  return new Queen(color);
      case KING:   return new King(color);
      default: assert false;
    }
    return null;
  }

  /** Constructor. */
  protected Piece(Type type, Color color) {
    this.type = type;
    this.color = color;
  }

  /**
   * Initializer for the moves map.
   * <p>Each sub-class must call this method from its constructor.
   * <p>This method uses abstract method calculateMoves, therefore it is not
   * called from the constructor of the base-class.
   */
  protected void initMovesMap() {
    ImmutableMap.Builder<Coordinate, ImmutableList<Move>> movesMapBuilder =
        ImmutableMap.builder();
    for (int file = Coordinate.FIRST_FILE;
         file <= Coordinate.LAST_FILE;
         file++) {
      for (int rank = Coordinate.FIRST_RANK;
           rank <= Coordinate.LAST_RANK;
           rank++) {
        Coordinate coordinate = Coordinate.get(file, rank);
        movesMapBuilder.put(coordinate, calculateMoves(coordinate));
      }
    }
    movesMap = movesMapBuilder.build();
  }

  /** Returns the piece type. */
  public Type getType() { return type; }
  /** Returns the piece color. */
  public Color getColor() { return color; }

  /**
   * Returns a list of all the possible moves that this piece can make from the
   * given coordinate. This list of moves does not take into consideration the
   * position of other pieces of the board, and can be used as a list of
   * candidate moves to check against a specific position.
   * @param from The coordinate from which the moves are made.
   * @return A list of all possible moves from the give coordinate.
   */
  public List<Move> moves(Coordinate from) {
    if (movesMap == null) {
      initMovesMap();
    }
    return movesMap.get(from);
  }

  /**
   * Calcualtes the legal moves of the piece from the given coordinate.
   * The returned moves are all of the piece's legal moves, regardless of the
   * board position, as the piece does not know the board position. It is the
   * role of Position to remove from the returned list any moves that are
   * illegal in the specific board position.
   * */
  protected abstract ImmutableList<Move> calculateMoves(Coordinate from);

  /** Returns the other (opposite) color from the one given. */
  public static Color otherColor(Color color) {
    return color == Color.WHITE ? Color.BLACK : Color.WHITE;
  }

  /**
   * Creates a new move and adds it to the list builder.
   * If one of the coordinates is null, a move is not created or added.
   * @param from The from coordinate of the move.
   * @param to The to coordinate of the move.
   * @param listBuilder The list builder to which the move is added.
   */
  protected static void addMove(
      Coordinate from, Coordinate to, ImmutableList.Builder<Move> listBuilder) {
    if (from == null || to == null) {
      return;
    }
    listBuilder.add(new Move(from, to));
  }

  /**
   * Adds moves to the moves list builder, where each move is to one (files,
   * ranks) step from the previous move's to, and must have all previous moves'
   * to coordinates unoccupied.
   * @param from The current piece coordinate.
   * @param files The number of files per move.
   * @param ranks The number of ranks per move.
   * @param listBuilder The list builder to which the moves are added.
   */
  protected static void addMovesSeries(
      Coordinate from,
      int files,
      int ranks,
      ImmutableList.Builder<Move> listBuilder) {
    Coordinate to = from.add(files, ranks);
    List<Coordinate> unoccupied = Lists.newArrayList();
    while (to != null) {
      Move move = new Move(from, to);
      move.setUnoccupied(Sets.newHashSet(unoccupied));
      listBuilder.add(move);
      unoccupied.add(to);
      to = to.add(files, ranks);
    }
  }

  /** A chess pawn. */
  private static class Pawn extends Piece {
    private static Map<Color, Integer> startingRank = ImmutableMap.of(
        Color.WHITE, Coordinate.FIRST_RANK + 1,
        Color.BLACK, Coordinate.LAST_RANK - 1);

    /** Constructor. */
    public Pawn(Color color) {
      super(Type.PAWN, color);
    }

    @Override
    public ImmutableList<Move> calculateMoves(Coordinate from) {
      if (from.getRank() == Coordinate.FIRST_RANK ||
          from.getRank() == Coordinate.LAST_RANK) {
        return ImmutableList.of();
      }

      ImmutableList.Builder<Move> listBuilder = ImmutableList.builder();

      int direction = this.getColor() == Color.WHITE ? 1 : -1;

      // The standard move.
      Coordinate to1 = from.add(0, direction);
      listBuilder.add(new Move(
            from,
            to1,
            null /* capture */,
            ImmutableSet.of(to1),
            false /* captureOnly */,
            null /* enPassantTarget */));

      // The double move from the starting position.
      if (from.getRank() == startingRank.get(getColor())) {
        Coordinate to2 = to1.add(0, direction);
        listBuilder.add(new Move(
              from,
              to2,
              null /* capture */,
              ImmutableSet.of(to1, to2),
              false /* captureOnly */,
              to1 /* enPassantTarget */));
      }

      // Captures.
      for (int filesToAdd : ImmutableList.of(-1, 1)) {
        Coordinate to = from.add(filesToAdd, direction);
        if (to != null) {
          Move capture = new Move(from, to);
          capture.setCaptureOnly(true);
          listBuilder.add(capture);
        }
      }

      return listBuilder.build();
    }
  }

  /** A chess rook. */
  private static class Rook extends Piece {
    /** Constructor. */
    public Rook(Color color) {
      super(Type.ROOK, color);
    }

    @Override
    public ImmutableList<Move> calculateMoves(Coordinate from) {
      ImmutableList.Builder<Move> listBuilder = ImmutableList.builder();

      addMovesSeries(from, -1, 0, listBuilder);
      addMovesSeries(from, 1, 0, listBuilder);
      addMovesSeries(from, 0, -1, listBuilder);
      addMovesSeries(from, 0, 1, listBuilder);

      return listBuilder.build();
    }
  }

  /** A chess knight. */
  private static class Knight extends Piece {
    /** Constructor. */
    public Knight(Color color) {
      super(Type.KNIGHT, color);
    }

    @Override
    public ImmutableList<Move> calculateMoves(Coordinate from) {
      ImmutableList.Builder<Move> listBuilder = ImmutableList.builder();

      addMove(from, from.add(1, 2), listBuilder);
      addMove(from, from.add(2, 1), listBuilder);
      addMove(from, from.add(2, -1), listBuilder);
      addMove(from, from.add(1, -2), listBuilder);
      addMove(from, from.add(-1, -2), listBuilder);
      addMove(from, from.add(-2, -1), listBuilder);
      addMove(from, from.add(-2, 1), listBuilder);
      addMove(from, from.add(-1, 2), listBuilder);

      return listBuilder.build();
    }
  }

  /** A chess bishop. */
  private static class Bishop extends Piece {
    /** Constructor. */
    public Bishop(Color color) {
      super(Type.BISHOP, color);
    }

    @Override
    public ImmutableList<Move> calculateMoves(Coordinate from) {
      ImmutableList.Builder<Move> listBuilder = ImmutableList.builder();

      addMovesSeries(from, -1, -1, listBuilder);
      addMovesSeries(from, -1, 1, listBuilder);
      addMovesSeries(from, 1, -1, listBuilder);
      addMovesSeries(from, 1, 1, listBuilder);

      return listBuilder.build();
    }
  }

  /** A chess queen. */
  private static class Queen extends Piece {
    /** Constructor. */
    public Queen(Color color) {
      super(Type.QUEEN, color);
    }

    @Override
    public ImmutableList<Move> calculateMoves(Coordinate from) {
      ImmutableList.Builder<Move> listBuilder = ImmutableList.builder();

      addMovesSeries(from, -1, 0, listBuilder);
      addMovesSeries(from, 1, 0, listBuilder);
      addMovesSeries(from, 0, -1, listBuilder);
      addMovesSeries(from, 0, 1, listBuilder);
      addMovesSeries(from, -1, -1, listBuilder);
      addMovesSeries(from, -1, 1, listBuilder);
      addMovesSeries(from, 1, -1, listBuilder);
      addMovesSeries(from, 1, 1, listBuilder);

      return listBuilder.build();
    }
  }

  /** A chess king. */
  private static class King extends Piece {
    /** Constructor. */
    public King(Color color) {
      super(Type.KING, color);
    }

    @Override
    public ImmutableList<Move> calculateMoves(Coordinate from) {
      // This map is kept here because:
      // 1. This method is called only once, so there's no reason that this will
      //    no be method local.
      // 2. It is dengerous to statically initialize this map, as the
      //    coordinates are also statically initialized, so we may encounter
      //    static initialization order issues.
      ImmutableMap<Piece.Color, Coordinate> initialPosition = ImmutableMap.of(
          Piece.Color.WHITE, Coordinate.get("e1"),
          Piece.Color.BLACK, Coordinate.get("e8"));

      ImmutableList.Builder<Move> listBuilder = ImmutableList.builder();

      addMove(from, from.add(-1, -1), listBuilder);
      addMove(from, from.add(-1, 0), listBuilder);
      addMove(from, from.add(-1, 1), listBuilder);
      addMove(from, from.add(0, -1), listBuilder);
      addMove(from, from.add(0, 1), listBuilder);
      addMove(from, from.add(1, -1), listBuilder);
      addMove(from, from.add(1, 0), listBuilder);
      addMove(from, from.add(1, 1), listBuilder);

      if (from == initialPosition.get(getColor())) {
        // Castling queen side.
        Coordinate to = from.add(-2, 0);
        Coordinate middle = from.add(-1, 0);
        Move queenSideCastle = new Move(from, to);
        queenSideCastle.setCapture(null);
        queenSideCastle.setUnoccupied(Sets.newHashSet(middle, to));
        queenSideCastle.setUnthreatened(Sets.newHashSet(from, middle));
        queenSideCastle.setCastlingSide(CastlingRights.Side.QUEEN);
        listBuilder.add(queenSideCastle);

        // Castling king side.
        to = from.add(2, 0);
        middle = from.add(1, 0);
        Move kingSideCastle = new Move(from, to);
        kingSideCastle.setCapture(null);
        kingSideCastle.setUnoccupied(ImmutableSet.of(middle, to));
        kingSideCastle.setUnthreatened(Sets.newHashSet(from, middle));
        kingSideCastle.setCastlingSide(CastlingRights.Side.KING);
        listBuilder.add(kingSideCastle);
      }

      return listBuilder.build();
    }
  }
}
