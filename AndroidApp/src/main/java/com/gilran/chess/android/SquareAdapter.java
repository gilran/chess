package com.gilran.chess.android;

import java.util.List;
import java.util.Map;

import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Piece;
import com.gilran.chess.board.PiecesPlacement.PlacementEntry;
import com.gilran.chess.board.Position;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class SquareAdapter extends BaseAdapter {
  private static enum SquareColor { LIGHT, DARK }
  private static final Map<SquareColor, Integer> SQUARE_BACKGROUND;
  private static final Map<SquareColor, Integer> HIGHLIGHTED_SQUARE_BACKGROUND;
  private static final Map<Piece, Integer> PIECE_IMAGE;
  
  static {
    SQUARE_BACKGROUND =
        ImmutableMap.<SquareAdapter.SquareColor, Integer>builder()
        .put(SquareColor.LIGHT, R.drawable.light_square)
        .put(SquareColor.DARK, R.drawable.dark_square)
        .build();
    HIGHLIGHTED_SQUARE_BACKGROUND =
        ImmutableMap.<SquareAdapter.SquareColor, Integer>builder()
        .put(SquareColor.LIGHT, R.drawable.highlighted_light_square)
        .put(SquareColor.DARK, R.drawable.highlighted_dark_square)
        .build();
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
  private List<Coordinate> highlightedSquares;

  public SquareAdapter(Context context) {
    this.context = context;
    this.orientation = Piece.Color.WHITE;
    this.highlightedSquares = Lists.newLinkedList();
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
  
  public void setOrientation(Piece.Color orientation) {
    this.orientation = orientation;
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
  
  public void draw(Position chessPosition) {
    for (PlacementEntry entry : chessPosition.getPiecesPlacement()) {
      ImageView pieceImageView = getPieceImageView(entry.getCoordinate());
      pieceImageView.setImageResource(PIECE_IMAGE.get(entry.getPiece()));
    }
  }
  
  public void resetHighlights() {
    for (Coordinate coordinate : highlightedSquares) {
      ImageView square = getBackgroundImageView(coordinate);
      square.setImageResource(
          SQUARE_BACKGROUND.get(getSquareColor(coordinate)));
    }
    highlightedSquares.clear();
  }
  
  public void highlight(Iterable<Coordinate> coordinates) {
    resetHighlights();
    for (Coordinate coordinate : coordinates) {
      ImageView square = getBackgroundImageView(coordinate);
      square.setImageResource(
          HIGHLIGHTED_SQUARE_BACKGROUND.get(getSquareColor(coordinate)));
      highlightedSquares.add(coordinate);
    }
  }
  
  public void move(Coordinate from, Coordinate to) {
    ImageView fromImageView = getPieceImageView(from);
    ImageView toImageView = getPieceImageView(to);
    toImageView.setImageDrawable(fromImageView.getDrawable());
    fromImageView.setImageResource(0);
  }
}
