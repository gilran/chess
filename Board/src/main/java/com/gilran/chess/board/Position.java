package com.gilran.chess.board;

import com.gilran.chess.Proto.GameStatus;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A position on the chess board.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Position extends PositionBase {
  // private static final Logger LOGGER = Logger.getLogger(
  //    Thread.currentThread().getStackTrace()[0].getClassName());

  /** Rook starting positions. Used for castling. */
  private static final Map<Piece.Color, Map<CastlingRights.Side, Coordinate>>
      ROOK_INITIAL_POSITION;
  static {
    ROOK_INITIAL_POSITION = ImmutableMap.<
        Piece.Color, Map<CastlingRights.Side, Coordinate>>builder()
        .put(
            Piece.Color.WHITE,
            ImmutableMap.<CastlingRights.Side, Coordinate>builder()
            .put(CastlingRights.Side.KING, Coordinate.get("h1"))
            .put(CastlingRights.Side.QUEEN, Coordinate.get("a1"))
            .build())
        .put(
            Piece.Color.BLACK,
            ImmutableMap.<CastlingRights.Side, Coordinate>builder()
            .put(CastlingRights.Side.KING, Coordinate.get("h8"))
            .put(CastlingRights.Side.QUEEN, Coordinate.get("a8"))
            .build())
        .build();
  }

  /**
   * The legal moves in the position.
   * <p>The outermost map's key is the color of one side, and the value is the
   * legal moves for pieces of that color.
   * <p>The second level map's key is the from-square of the move, and the value
   * is the legal moves from that square.
   * <p>The innermost map's key is the to-square of the move, and the value is
   * the move object itself.
   */
  private Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>> legalMoves;

  /** The piece to which pawns are promoted for each side. */
  private Map<Piece.Color, Piece.Type> promotionPieceType;

  /**
   * A map of previous game positions.
   * <p>The map is from the FEN representation of the position to the number of
   * times it was seen.
   * <p>The map is used to test for threefold repetition, thus every time a
   * piece is captured it may be cleaned, as all previous positions will not be
   * seen again.
   */
  private Map<String, Integer> previousPositions;

  /** The game's status. */
  private GameStatus status;

  /** Constructs a board with the starting position of a standard chess game. */
  public Position() {
    this(ForsythEdwardsNotation.STARTING_POSITION);
  }

  /** Constructs a board with a the position of the given PositionBase. */
  public Position(PositionBase other) {
    super(other);
    previousPositions = Maps.newHashMap();
    updateLegalMoves();
    updateStatus();
  }
  
  public Set<Coordinate> getLegalMoves(Coordinate from) {
    Piece piece = getPiecesPlacement().at(from);
    if (piece == null) {
      return Collections.emptySet();
    }
    Map<Coordinate, Move> movesMap = legalMoves.get(piece.getColor()).get(from);
    if (movesMap == null) {
      return Collections.emptySet();
    }
    return movesMap.keySet();
  }

  /** Returns the piece type to which pawns are promoted for the given side. */
  public Piece.Type getPromotionPieceType(Piece.Color color) {
    return promotionPieceType.get(color);
  }

  /**
   * Sets the piece type to which pawns are promoted for the given side.
   * The promotion piece type cannot be a pawn.
   * */
  public void setPromotionPieceType(
      Piece.Color color, Piece.Type promotionPieceType) {
    assert promotionPieceType != Piece.Type.PAWN;
    this.promotionPieceType.put(color, promotionPieceType);
  }

  /** Returns the game's status. */
  public GameStatus getStatus() { return status; }

  /**
   * Reverts moves, without any legallity tests.
   * <p>Handles more than one move, because castling involves two moves.
   */
  private void revert(List<Move> moves, Piece capturedPiece) {
    for (Move move : moves) {
      Piece piece = at(move.getTo());
      piecesPlacement.move(move.getTo(), move.getFrom());
      if (move.getCapture() != null) {
        assert capturedPiece != null;
        piecesPlacement.add(capturedPiece, move.getCapture());
      }

      if (move.getPromotionPiece() != null) {
        piecesPlacement.add(
            Piece.get(Piece.Type.PAWN, piece.getColor()), move.getFrom());
      } else if (piece.getType() == Piece.Type.KING) {
        kingPosition.put(piece.getColor(), move.getFrom());
      }
    }
  }

  /**
   * Applies moves in the position.
   * <p> Only updates piece locations, without updating any other property of
   * the position. This way, it is possible to easily revert the move, and thus
   * simulate moves in order to test whether the move is illegal due to being
   * checked after the move. The option to revert is also the reason for
   * returning the captured piece.
   *
   * @param move The move to apply.
   * @return The piece that was captured in the move (may be null if no piece
   *     was captured).
   */
  private Piece apply(List<Move> moves) {
    Piece capturedPiece = null;
    for (Move move : moves) {
      Piece piece = at(move.getFrom());

      if (move.getCapture() != null) {
        if (piecesPlacement.isOccupied(move.getCapture())) {
          capturedPiece = piecesPlacement.remove(move.getCapture());
        } else {
          move.setCapture(null);
        }
      }
      piecesPlacement.move(move.getFrom(), move.getTo());

      if (piece.getType() == Piece.Type.PAWN &&
          (move.getTo().getRank() == Coordinate.FIRST_RANK ||
           move.getTo().getRank() == Coordinate.LAST_RANK)) {
        move.setPromotionPiece(promotionPieceType.get(piece.getColor()));
        piecesPlacement.remove(move.getTo());
        piecesPlacement.add(
            Piece.get(move.getPromotionPiece(), piece.getColor()),
            move.getTo());
      } else if (piece.getType() == Piece.Type.KING) {
        kingPosition.put(piece.getColor(), move.getTo());
      }
    }
    return capturedPiece;
  }

  /**
   * Makes a move in the position.
   *
   * @param from The coordinate from which the piece moves.
   * @param to The coordinate to which the piece moves.
   * @return The moves perfomed. If empty, the requested move is not legal in
   *     the position. In case of castling, both the king move and the rook move
   *     are returned.
   */
  public List<Move> move(Coordinate from, Coordinate to) {
    List<Move> moves = Lists.newArrayList();

    if (!legalMoves.get(activePlayer).containsKey(from) ||
        !legalMoves.get(activePlayer).get(from).containsKey(to)) {
      return moves;
    }

    Move theMove = legalMoves.get(activePlayer).get(from).get(to);
    moves.add(theMove);

    Piece piece = at(from);
    boolean isCapture =
        theMove.getCapture() != null && at(theMove.getCapture()) != null;
    if (!isCapture) {
      theMove.setCapture(null);
    }

    if (theMove.getCastlingSide() != null) {
      moves.add(completeCastle(theMove));
    }

    apply(moves);
    updateCastlingRights(theMove, piece);

    enPassantTarget = theMove.getEnPassantTarget();
    activePlayer = Piece.otherColor(activePlayer);
    if (activePlayer == Piece.Color.WHITE) {
      currentMove++;
    }
    if (isCapture || piece.getType() == Piece.Type.PAWN) {
      halfMovesClock = 0;
    } else {
      halfMovesClock++;
    }

    updateLegalMoves();
    updateStatus();

    return moves;
  }

  /**
   * Completes a castling move.
   *
   * @param move The king's castling move.
   * @return The rook's castling move.
   */
  private Move completeCastle(Move move) {
    Piece piece = at(move.getFrom());
    Preconditions.checkState(piece.getType() == Piece.Type.KING);
    Coordinate from =
        ROOK_INITIAL_POSITION.get(piece.getColor()).get(move.getCastlingSide());
    Coordinate to =
        move.getTo().add(
            move.getCastlingSide() == CastlingRights.Side.KING ? -1 : 1, 0);
    return new Move(from, to);
  }

  /**
   * Makes updates to castling rights caused by a move.
   *
   * @param move The move made.
   * @param piece The moved piece.
   */
  private void updateCastlingRights(Move move, Piece piece) {
    switch (piece.getType()) {
      case KING:
        castlingRights.revoke(piece.getColor(), CastlingRights.Side.KING);
        castlingRights.revoke(piece.getColor(), CastlingRights.Side.QUEEN);
        break;
      case ROOK:
        Map<CastlingRights.Side, Coordinate> rooks =
            ROOK_INITIAL_POSITION.get(piece.getColor());
        if (move.getFrom() == rooks.get(CastlingRights.Side.KING)) {
          castlingRights.revoke(piece.getColor(), CastlingRights.Side.KING);
        } else if (move.getFrom() == rooks.get(CastlingRights.Side.QUEEN)) {
          castlingRights.revoke(piece.getColor(), CastlingRights.Side.QUEEN);
        }
        break;
      default:
        break;
    }
  }

  /** Updates the legal moves map. */
  private void updateLegalMoves() {
    legalMoves = this.createCandidateMoves();
    removeThreatened(Piece.Color.WHITE);
    removeThreatened(Piece.Color.BLACK);
    removeChecked(Piece.Color.WHITE);
    removeChecked(Piece.Color.BLACK);
  }

  /**
   * Returns true iff the current position was seen for the third time.
   * <p>Updates the previous positions map for use in the next tests for
   * threefold repetition.
   * */
  private boolean isThreefoldRepetition() {
    String fen = new ForsythEdwardsNotation(this).toString();
    Integer numberOfTimesPositionWasSeen = previousPositions.get(fen);
    if (numberOfTimesPositionWasSeen == null) {
      previousPositions.put(fen, 1);
      return false;
    }

    numberOfTimesPositionWasSeen++;
    return numberOfTimesPositionWasSeen == 3;
  }

  /** Creates candidate moves for all the pieces in the position. */
  private Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>>
      createCandidateMoves() {
    Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>> moves =
        Maps.newEnumMap(Piece.Color.class);
    moves.put(
        Piece.Color.WHITE,
        Maps.<Coordinate, Map<Coordinate, Move>>newHashMap());
    moves.put(
        Piece.Color.BLACK,
        Maps.<Coordinate, Map<Coordinate, Move>>newHashMap());

    for (PiecesPlacement.PlacementEntry entry : piecesPlacement) {
      Coordinate from = entry.getCoordinate();
      Piece piece = entry.getPiece();
      Map<Coordinate, Map<Coordinate, Move>> sideMoves =
          moves.get(piece.getColor());
      List<Move> movesList = Lists.newLinkedList(piece.moves(from));
      if (removeIllegalMoves(movesList)) {
        continue;
      }
      Map<Coordinate, Move> coordinateMoves = Maps.newHashMap();
      for (Move move : movesList) {
        coordinateMoves.put(move.getTo(), move);
      }
      sideMoves.put(from, coordinateMoves);
    }

    return moves;
  }

  /**
   * Removes the illegal moves from the candidate moves list.
   *
   * @param candidates A list of candidate moves.
   * @return true iff all moves were removed.
   */
   private boolean removeIllegalMoves (List<Move> candidates) {
    Iterator<Move> it = candidates.iterator();
    while (it.hasNext()) {
      Move move = it.next();
      if (!isLegalMove(move)) {
        it.remove();
      }
    }
    return candidates.size() == 0;
  }

   /**
   * Tests if a move is valid.
   * <p>Assumes that the move is valid by the piece rules, and only checks the
   * move against the position.
   * <p>Ignores the active player, as legal moves of both sides may be checked
   * using this method, regardless of who's the active player (in order to test
   * for checks).
   *
   * @param move A move.
   * @return false if the given move is legal in this position. May return true
   *     for illegal moves, as the move is considered a candidate move that the
   *     piece returned.
   */
  private boolean isLegalMove(Move move) {
    Piece piece = at(move.getFrom());

    // No piece at the from-coordinate.
    if (piece == null) {
      return false;
    }

    Piece capturedPiece = at(move.getTo());
    boolean capture = false;
    if (capturedPiece != null) {
      // Trying to capture a piece of the same color.
      if (piece.getColor() == capturedPiece.getColor()) {
        return false;
      }
      capture = true;
      move.setCapture(move.getTo());
    }

    // The move is only legal as a capture, but there is no piece to capture at
    // the to-squeare.
    if (move.isCaptureOnly() && !capture) {
      if (move.getTo().equals(getEnPassantTarget())) {
        move.setCapture(
            getEnPassantTarget().add(
                0, piece.getColor() == Piece.Color.WHITE ? -1 : 1));
        if (!piecesPlacement.isOccupied(move.getCapture())) {
          return false;
        }
      } else {
        return false;
      }
    }

    // If the move is a castle, but the castling right was lost, the move is
    // illegal.
    if (move.getCastlingSide() != null) {
      if (!castlingRights.get(piece.getColor(), move.getCastlingSide())) {
        return false;
      }
    }

    // A square that must be unoccupied for this move to be legal is occupied.
    if (piecesPlacement.anyOccupied(move.getUnoccupied())) {
      return false;
    }

    return true;
  }

  /**
   * Removes from the legal moves map for the given color moves that are illegal
   * due to threatened coordinates.
   * */
  private void removeThreatened(Piece.Color color) {
    Piece.Color otherColor = Piece.otherColor(color);
    Map<Coordinate, Map<Coordinate, Move>> sideMoves = legalMoves.get(color);
    Iterator<Map<Coordinate, Move>> fromIt = sideMoves.values().iterator();
    while (fromIt.hasNext()) {
      Map<Coordinate, Move> toMap = fromIt.next();
      Iterator<Move> toIt = toMap.values().iterator();
      while (toIt.hasNext()) {
        Move move = toIt.next();
        // A square that must be unthreatened for this move to be legal is
        // threatened.
        if (anyThreatened(move.getUnthreatened(), otherColor, legalMoves)) {
          toIt.remove();
        }
      }
      if (toMap.isEmpty()) {
        fromIt.remove();
      }
    }
  }

  /**
   * Tests if any of the given coordinates are threatened by a piece of the
   * given color.
   *
   * @param coordinates The coordinates.
   * @param color The color of the threatening side.
   * @param movesMap The moves map of moves to use as legal moves.
   * @return true iff at least one of the coordinates is threatened by pieces of
   *     the given color.
   */
  private boolean anyThreatened(
      Iterable<Coordinate> coordinates,
      Piece.Color color,
      Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>> movesMap) {
    for (Coordinate coordinate : coordinates) {
      if (isThreatened(coordinate, color, movesMap)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tests if a coordinate is threatened by the active player.
   *
   * @param coordinate The coordinate.
   * @param color The color of the threatning side.
   * @param movesMap The moves map of moves to use as legal moves.
   * @return true iff the coordinate is threatened.
   */
  private boolean isThreatened(
      Coordinate coordinate,
      Piece.Color color,
      Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>> movesMap) {
    Map<Coordinate, Map<Coordinate, Move>> sideMoves = movesMap.get(color);
    for (Map<Coordinate, Move> toMap : sideMoves.values()) {
      if (toMap.containsKey(coordinate)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tests if the king of the given color is checked.
   *
   * @param color The color of the king to test for checks.
   * @param movesMap The map of moves to use as legal moves.
   * @return true iff the king is checked.
   */
  private boolean isChecked(
      Piece.Color color,
      Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>> movesMap) {
    return isThreatened(
        kingPosition.get(color), Piece.otherColor(color), movesMap);
  };

  /**
   * Removes from the legal moves map for the given color moves that are illegal
   * because making them will put the king in a check.
   */
  private void removeChecked(Piece.Color color) {
    Map<Coordinate, Map<Coordinate, Move>> sideMoves = legalMoves.get(color);
    Iterator<Map<Coordinate, Move>> fromIt = sideMoves.values().iterator();
    while (fromIt.hasNext()) {
      Map<Coordinate, Move> toMap = fromIt.next();
      Iterator<Move> toIt = toMap.values().iterator();
      while (toIt.hasNext()) {
        Move move = toIt.next();

        // Simulating the moves and testing for check.
        List<Move> moveAsList = ImmutableList.of(move);
        Piece capturedPiece = apply(moveAsList);
        Map<Piece.Color, Map<Coordinate, Map<Coordinate, Move>>> moves =
            createCandidateMoves();
        boolean checked = isChecked(color, moves);
        revert(moveAsList, capturedPiece);

        if (checked) {
          toIt.remove();
        }
      }
      if (toMap.isEmpty()) {
        fromIt.remove();
      }
    }
  }

  /** Updates the position status (testing for end-game conditions). */
  private void updateStatus() {
    boolean check = isChecked(getActivePlayer(), legalMoves);
    boolean hasLegalMoves = !legalMoves.get(getActivePlayer()).isEmpty();

    if (getHalfMovesClock() >= 100) {
      status = GameStatus.HALFMOVE_CLOCK_EXPIRED;
      return;
    }

    if (isThreefoldRepetition()) {
      status = GameStatus.THREEFOLD_REPETITION;
      return;
    }

    // TODO(gilran): Handle INSUFFICIENT_MATERIAL.

    switch (getActivePlayer()) {
      case WHITE:
        if (hasLegalMoves) {
          status = check ? GameStatus.WHITE_CHECKED : GameStatus.WHITE_TO_MOVE;
        } else {
          status =
            check ? GameStatus.WHITE_CHECKMATED : GameStatus.WHITE_STALEMATED;
        }
        return;
      case BLACK:
        if (hasLegalMoves) {
          status = check ? GameStatus.BLACK_CHECKED : GameStatus.BLACK_TO_MOVE;
        } else {
          status =
            check ? GameStatus.BLACK_CHECKMATED : GameStatus.BLACK_STALEMATED;
        }
        return;
    }
  }
}
