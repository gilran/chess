package com.gilran.chess.board;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * The castling rights a chess position.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
/* package */ class CastlingRights {
  /** An enum for castling sides.
   * <p>The order of elements in the enum is important as notation classes count
   * on this order when creating notation strings.
   */
  public static enum Side {
    /**  King-side castling. */
    KING,
    /**  Queen-side castling. */
    QUEEN,
  }

  /** A specific castling move information. */
  public static class Castle {
    private static final Map<Piece.Color, Map<Side, Castle>> CASTLES;
    static {
      ImmutableMap.Builder<Piece.Color, Map<Side, Castle>> outerBuilder =
          ImmutableMap.builder();
      for (Piece.Color color : Piece.Color.values()) {
        ImmutableMap.Builder<Side, Castle> innerBuilder =
            ImmutableMap.builder();
        for (Side side : Side.values()) {
          innerBuilder.put(side, new Castle(color, side));
        }
        outerBuilder.put(color, innerBuilder.build());
      }
      CASTLES = outerBuilder.build();
    }

    /** The color that's castling. */
    private final Piece.Color color;

    /** The castling side. */
    private final Side side;

    /** Constructor. */
    private Castle(Piece.Color color, Side side) {
      this.color = color;
      this.side = side;
    }

    /** Returns the Castle object for the given color and side. */
    public static Castle get(Piece.Color color, Side side) {
      return CASTLES.get(color).get(side);
    }

    /** Returns the color that's castling. */
    public Piece.Color getColor() { return color; }

    /** Returns the castling side. */
    public Side getSide() { return side; }
  }

  /**
   * The castling rights of each side.
   * For each side, the inner map will have KING_SIDE and QUEEN_SIDE as keys,
   * and a boolean indicating if the side is holding the right to castle in that
   * direction.
   * */
  private Map<Castle, Boolean> rightsMap;

  /** Creates castling rights, with all rights enabled. */
  CastlingRights() {
    this(true);
  }

  /** Creates castling rights, with all rights set to the given default. */
  CastlingRights(boolean defaultRight) {
    rightsMap = Maps.newHashMap();

    rightsMap.put(Castle.get(Piece.Color.WHITE, Side.KING), defaultRight);
    rightsMap.put(Castle.get(Piece.Color.WHITE, Side.QUEEN), defaultRight);
    rightsMap.put(Castle.get(Piece.Color.BLACK, Side.KING), defaultRight);
    rightsMap.put(Castle.get(Piece.Color.BLACK, Side.QUEEN), defaultRight);
  }

  /** Copy constructor. */
  CastlingRights(CastlingRights other) {
    rightsMap = Maps.newHashMap(other.rightsMap);
  }

  /**
   * Returns true iff the given color holds the right to castle to the given
   * side.
   */
  public boolean get(Piece.Color color, Side side) {
    return get(Castle.get(color, side));
  }

  /**
   * Returns true iff the the right to perform the given castle is held.
   */
  public boolean get(Castle castle) {
    return rightsMap.get(castle);
  }

  /** Sets the castling right for the given castle. */
  public void set(Castle castle, boolean right) {
    rightsMap.put(castle, right);
  }

  /** Revokes the right of the given color to castle to the given side. */
  public void revoke(Piece.Color color, Side side) {
    revoke(Castle.get(color, side));
  }

  /** Revokes the right to perform the given castle. */
  public void revoke(Castle castle) {
    set(castle, false);
  }
}
