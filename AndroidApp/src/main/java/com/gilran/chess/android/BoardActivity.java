package com.gilran.chess.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

public class BoardActivity extends Activity {
  static final String EXTRA_LOCAL_PLAYER_NAME =
      "com.gilran.chess.android.EXTRA_LOCAL_PLAYER_NAME";

  private ChessClientService.Connection connection =
      new ChessClientService.Connection();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!connection.isBound()) {
      bindService(
          new Intent(this, ChessClientService.class),
          connection,
          Context.BIND_AUTO_CREATE);
    }

    setContentView(R.layout.activity_game);

    if (savedInstanceState == null) {
      getFragmentManager().beginTransaction()
          .add(R.id.container, new TopLevelFragment()).commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.game, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  public static class TopLevelFragment extends Fragment {
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState) {
      View rootView =
          inflater.inflate(R.layout.fragment_board, container, false);

      TextView playerNameView =
          (TextView) rootView.findViewById(R.id.localPlayerName);
      playerNameView.setText(
          getActivity().getIntent().getExtras().getString(
              EXTRA_LOCAL_PLAYER_NAME));

      GridView board = (GridView) rootView.findViewById(R.id.chessboard);
      board.setAdapter(new SquareAdapter(getActivity()));
      return rootView;
    }
  }

}
