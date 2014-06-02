package com.gilran.chess.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.gilran.chess.board.Coordinate;

public class SquareAdapter extends BaseAdapter {
  private Context context;
  private static final Integer[] SQUARE_BACKGROUND = {
    R.drawable.dark_square, R.drawable.light_square
  };
  
  public SquareAdapter(Context context) {
    this.context = context;
  }

  @Override
  public int getCount() {
    return Coordinate.FILES * Coordinate.RANKS;
  }

  @Override
  public Object getItem(int position) {
    return null;
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    int file = position % Coordinate.FILES;
    int rank = Coordinate.LAST_RANK - (position / Coordinate.RANKS);
    
    View squareContainerView = convertView;
    if (convertView == null) {
      LayoutInflater layoutInflater =
          (LayoutInflater) context.getSystemService(
              Context.LAYOUT_INFLATER_SERVICE);
      squareContainerView =  layoutInflater.inflate(R.layout.square, null);
    }
    ImageView squareView = 
        (ImageView) squareContainerView.findViewById(R.id.square_background);
    squareView.setImageResource(SQUARE_BACKGROUND[(file + rank)%2]);
    return squareContainerView;
  }
}