package com.gilran.chess.android;

import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Move;
import com.gilran.chess.board.Piece;
import com.gilran.chess.board.PiecesPlacement.PlacementEntry;
import com.gilran.chess.board.Position;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;
import java.util.Map;

public class SquareAdapter extends BaseAdapter {
  private static enum HightlightColor { GREEN, YELLOW }
  private static enum SquareColor { LIGHT, DARK }

  private static final Map<SquareColor, Integer> SQUARE_BACKGROUND;
  private static final Map<HightlightColor, Map<SquareColor, Integer>>
      HIGHLIGHTED_SQUARE_BACKGROUND;
  private static final Map<Piece, Integer> PIECE_IMAGE;

  static {
    SQUARE_BACKGROUND =
        ImmutableMap.<SquareAdapter.SquareColor, Integer>builder()
        .put(SquareColor.LIGHT, R.drawable.light_square)
        .put(SquareColor.DARK, R.drawable.dark_square)
        .build();
    Map<SquareAdapter.SquareColor, Integer> greenSquareBackground =
        ImmutableMap.<SquareAdapter.SquareColor, Integer>builder()
        .put(SquareColor.LIGHT, R.drawable.green_light_square)
        .put(SquareColor.DARK, R.drawable.green_dark_square)
        .build();
    Map<SquareAdapter.SquareColor, Integer> yellowSquareBackground =
        ImmutableMap.<SquareAdapter.SquareColor, Integer>builder()
        .put(SquareColor.LIGHT, R.drawable.yellow_light_square)
        .put(SquareColor.DARK, R.drawable.yellow_dark_square)
        .build();
    HIGHLIGHTED_SQUARE_BACKGROUND = ImmutableMap.of(
        HightlightColor.GREEN, greenSquareBackground,
        HightlightColor.YELLOW, yellowSquareBackground);

    PIECE_IMAGE = ImmutableMap.<Piece, Integer>builder()
        .put(Piece.get(Piece.Type.PAWN, Piece.Color.WHITE),
             R.drawable.white_pawn)
        .put(Piece.get(Piece.Type.ROOK, Piece.Color.WHITE),
             R.drawable.white_rook)
        .put(Piece.get(Piece.Type.KNIGHT, Piece.Color.WHITE),
             R.drawable.white_knight)
        .put(Piece.get(Piece.Type.BISHOP, Piece.Color.WHITE),
             R.drawable.white_bishop)
        .put(Piece.get(Piece.Type.QUEEN, Piece.Color.WHITE),
             R.drawable.white_queen)
        .put(Piece.get(Piece.Type.KING, Piece.Color.WHITE),
             R.drawable.white_king)
        .put(Piece.get(Piece.Type.PAWN, Piece.Color.BLACK),
             R.drawable.black_pawn)
        .put(Piece.get(Piece.Type.ROOK, Piece.Color.BLACK),
             R.drawable.black_rook)
        .put(Piece.get(Piece.Type.KNIGHT, Piece.Color.BLACK),
             R.drawable.black_knight)
        .put(Piece.get(Piece.Type.BISHOP, Piece.Color.BLACK),
             R.drawable.black_bishop)
        .put(Piece.get(Piece.Type.QUEEN, Piece.Color.BLACK),
             R.drawable.black_queen)
        .put(Piece.get(Piece.Type.KING, Piece.Color.BLACK),
             R.drawable.black_king)
        .build();
  }

  private Context context;
  private View square[][];
  private Piece.Color orientation;
  private Multimap<HightlightColor, Coordinate> highlightedSquares;

  public SquareAdapter(Context context) {
    this.context = context;
    this.orientation = Piece.Color.WHITE;
    this.highlightedSquares = ArrayListMultimap.create();
    createSquares();
  }

  private void createSquares() {
    square = new View[Coordinate.FILES][Coordinate.RANKS];
    LayoutInflater layoutInflater =
        (LayoutInflater) context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE);
    for (int f = Coordinate.FIRST_FILE; f <= Coordinate.LAST_FILE; ++f) {
      for (int r = Coordinate.FIRST_RANK; r <= Coordinate.LAST_RANK; ++r) {
        View squareView = layoutInflater.inflate(R.layout.square, null);
        ImageView backgroundView =
            (ImageView) squareView.findViewById(R.id.square_background);
        backgroundView.setImageResource(
            SQUARE_BACKGROUND.get(getSquareColor(f, r)));
        square[f][r] = squareView;
      }
    }
  }

  @Override
  public int getCount() {
    return Coordinate.FILES * Coordinate.RANKS;
  }

  @Override
  public Object getItem(int position) {
    int column = position % Coordinate.FILES;
    int row = Coordinate.LAST_RANK - (position / Coordinate.RANKS);
    return square[column][row];
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    int column = position % Coordinate.FILES;
    int row = Coordinate.LAST_RANK - (position / Coordinate.RANKS);

    return square[column][row];
  }

  private SquareColor getSquareColor(int file, int rank) {
    return (file + rank) % 2 == 0 ? SquareColor.DARK : SquareColor.LIGHT;
  }

  private SquareColor getSquareColor(Coordinate coordinate) {
    return getSquareColor(coordinate.getFile(), coordinate.getRank());
  }

  public Coordinate getCoordinate(int position) {
    Preconditions.checkArgument(0 <= position && position <= getCount());
    int file = -1;
    int rank = -1;
    switch (orientation) {
      case WHITE:
        file = position % 8;
        rank = Coordinate.LAST_RANK - (position / 8);
        break;
      case BLACK:
        file = Coordinate.LAST_FILE - (position % 8);
        rank = position / 8;
        break;
    }
    return Preconditions.checkNotNull(Coordinate.get(file, rank));
  }

  private int getPosition(Coordinate coordinate) {
    int position = -1;
    switch (orientation) {
      case WHITE:
        position =
            (Coordinate.LAST_RANK - coordinate.getRank()) * Coordinate.FILES +
            coordinate.getFile();
        break;
      case BLACK:
        position =
            coordinate.getRank() * Coordinate.FILES +
            Coordinate.LAST_FILE - coordinate.getFile();
        break;
    }

    Preconditions.checkState(position >= 0);
    return position;
  }

  private ImageView getPieceImageView(Coordinate coordinate) {
    View square = (View) getItem(getPosition(coordinate));
    return (ImageView) square.findViewById(R.id.piece);
  }

  private ImageView getBackgroundImageView(Coordinate coordinate) {
    View square = (View) getItem(getPosition(coordinate));
    return (ImageView) square.findViewById(R.id.square_background);
  }

  public void draw(Position chessPosition, Piece.Color orientation) {
    clear();
    this.orientation = orientation;
    for (PlacementEntry entry : chessPosition.getPiecesPlacement()) {
      ImageView pieceImageView = getPieceImageView(entry.getCoordinate());
      pieceImageView.setImageResource(PIECE_IMAGE.get(entry.getPiece()));
    }
  }

  public void clear() {
    for (int r = Coordinate.FIRST_RANK; r <= Coordinate.LAST_RANK; ++r) {
      for (int f = Coordinate.FIRST_FILE; f <= Coordinate.LAST_FILE; ++f) {
        getPieceImageView(Coordinate.get(f, r)).setImageDrawable(null);
      }
    }
    for (HightlightColor color : HightlightColor.values()) {
      resetHighlights(color);
    }
    orientation = Piece.Color.WHITE;
  }

  private void resetHighlights(HightlightColor color) {
    for (Coordinate coordinate : highlightedSquares.get(color)) {
      ImageView square = getBackgroundImageView(coordinate);
      square.setImageResource(
          SQUARE_BACKGROUND.get(getSquareColor(coordinate)));
    }
    highlightedSquares.removeAll(color);
  }

  public void resetHighlights() {
    resetHighlights(HightlightColor.YELLOW);
  }

  private void highlight(
      HightlightColor color, Iterable<Coordinate> coordinates) {
    resetHighlights(color);
    for (Coordinate coordinate : coordinates) {
      ImageView square = getBackgroundImageView(coordinate);
      square.setImageResource(HIGHLIGHTED_SQUARE_BACKGROUND
          .get(color).get(getSquareColor(coordinate)));
      highlightedSquares.put(color, coordinate);
    }
  }

  public void highlight(Iterable<Coordinate> coordinates) {
    highlight(HightlightColor.YELLOW, coordinates);
  }

  public void move(List<Move> moves) {
    List<Coordinate> coordinates = Lists.newLinkedList();
    for (Move move : moves) {
      Coordinate from = move.getFrom();
      Coordinate to = move.getTo();
      ImageView fromPiece = getPieceImageView(from);
      ImageView toPiece = getPieceImageView(to);
      toPiece.setImageDrawable(fromPiece.getDrawable());
      fromPiece.setImageDrawable(null);
      coordinates.add(from);
      coordinates.add(to);
    }
    resetHighlights(HightlightColor.YELLOW);
    highlight(HightlightColor.GREEN, coordinates);
  }
}
