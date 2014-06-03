package com.gilran.chess.board;

/**
 * A coordinate on the chess board.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Coordinate implements Comparable<Coordinate> {
  /** The number of files. */
  public static final int FILES = 8;

  /** The number of ranks. */
  public static final int RANKS = 8;

  /** The index of the first file. */
  public static final int FIRST_FILE = 0;

  /** The index of the first rank. */
  public static final int FIRST_RANK = 0;

  /** The index of the last file. */
  public static final int LAST_FILE = FILES - 1;

  /** The index of the last rank. */
  public static final int LAST_RANK = RANKS - 1;

  /** The file index. */
  private int file;

  /** The rank index. */
  private int rank;

  /**
   * Static matrix of all coordinats.
   * <p>The number of coordinates is small, and all coordinates can be held in
   * memoery at all times. This would prevent duplicate instances of the same
   * coordinate, and would save some garbage collection.
   */
  private static final Coordinate COORDINATES[][];
  static {
    Coordinate tmpCoordinates[][] = new Coordinate[FILES][RANKS];
    for (int file = FIRST_FILE; file <= LAST_FILE; file++) {
      for (int rank = FIRST_RANK; rank <= LAST_RANK; rank++) {
        tmpCoordinates[file][rank] = new Coordinate(file, rank);
      }
    }
    COORDINATES = tmpCoordinates;
  }

  /** Returns the coordinate at the given file and rank. */
  public static Coordinate get(int file, int rank) {
    if (!isValidFile(file) || !isValidRank(rank)) {
      return null;
    }
    return COORDINATES[file][rank];
  }

  /**
   * Returns a coordinate by its name, or null if the name is invalid.
   * <p>A coordinate name is the algebric notation name of the square, where the
   * file is in lower-case. For example: "e4".
   */
  public static Coordinate get(String name) {
    if (name.length() != 2) {
      return null;
    }
    Integer file = fileIndex(name.charAt(0));
    Integer rank = rankIndex(name.charAt(1));
    if (file == null || rank == null) {
      return null;
    }
    return get(file, rank);
  }

  /** Constructor from file and rank. */
  private Coordinate(int file, int rank) {
    this.file = file;
    this.rank = rank;
  }

  /** Returns the file. */
  public int getFile() { return file; }

  /** Returns the rank. */
  public int getRank() { return rank; }

  /**
   * Tries to add the given files and ranks to the coodinate.
   *
   * @param files The number of files to add (may be negative).
   * @param ranks The number of ranks to add (may be negative).
   * @return The coordinate with the added files and ranks, or null if the new
   *     coordinate is outside the board.
   */
  public Coordinate add(int files, int ranks) {
    return get(this.file + files, this.rank + ranks);
  }

  /** Returns the name of the coordinate. */
  public String name() {
    return "" + fileName() + rankName();
  }

  /** * Returns true iff the file index is valid. */
  private static boolean isValidFile(int file) {
    return FIRST_FILE <= file && file <= LAST_FILE;
  }

  /** Returns true iff the rank index is valid. */
  private static boolean isValidRank(int rank) {
    return FIRST_RANK <= rank && rank <= LAST_RANK;
  }

  /** Returns the name of the file. */
  private char fileName() {
    return (char) ('a' + file);
  }

  /** Returns the name of the rank. */
  private char rankName() {
    return (char) ('1' + rank);
  }

  /**
   * Returns the file index for the given file name, or null if the name is
   * invalid.
   */
  private static Integer fileIndex(char file) {
    int index = (int) (file - 'a');
    if (!isValidFile(index)) {
      return null;
    }
    return index;
  }

  /** Returns the rank index for the given rank name. */
  private static Integer rankIndex(char rank) {
    if (!Character.isDigit(rank)) {
      return null;
    }
    int index = Character.digit(rank, 10) - 1;
    if (!isValidRank(index)) {
      return null;
    }
    return index;
  }

  public String toString() {
    return name();
  }

  public int compareTo(Coordinate other) {
    if (this.file != other.file) {
      return this.file < other.file ? -1 : 1;
    }
    if (this.rank != other.rank) {
      return this.rank < other.rank ? -1 : 1;
    }
    return 0;
  }
}
