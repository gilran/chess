package com.gilran.chess.android;

import java.util.List;
import java.util.Set;

import com.gilran.chess.Proto.GameEvent;
import com.gilran.chess.Proto.MoveProto;
import com.gilran.chess.Proto.SeekResponse;
import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Game;
import com.gilran.chess.board.Move;
import com.gilran.chess.board.Piece;
import com.gilran.chess.client.GameEventHandler;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

public class BoardActivity extends Activity {
  static final String EXTRA_LOCAL_PLAYER_NAME =
      "com.gilran.chess.android.EXTRA_LOCAL_PLAYER_NAME";

  private ChessClientService.Connection connection =
      new ChessClientService.Connection ();
  private String username;
  private Game game;
  private Piece.Color pieceColor;
  private Coordinate moveSource;
  private GridView board;
  private SquareAdapter squareAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!connection.isBound()) {
      bindService(
          new Intent(this, ChessClientService.class),
          connection,
          Context.BIND_AUTO_CREATE);
    }

    username = getIntent().getExtras().getString(EXTRA_LOCAL_PLAYER_NAME);
    
    setContentView(R.layout.activity_game);
    
    if (savedInstanceState == null) {
      getFragmentManager().beginTransaction()
          .add(R.id.container, new BoardFragment()).commit();
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
    if (id == R.id.action_seek) {
      seek();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  public static class BoardFragment extends Fragment {
    @Override
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState) {
      View rootView =
          inflater.inflate(R.layout.fragment_board, container, false);

      final BoardActivity activity = (BoardActivity) getActivity();
      TextView playerNameView =
          (TextView) rootView.findViewById(R.id.localPlayerName);
      playerNameView.setText(activity.username);

      activity.board = (GridView) rootView.findViewById(R.id.chessboard);
      activity.squareAdapter = new SquareAdapter(getActivity());
      activity.board.setAdapter(activity.squareAdapter);
      activity.board.setOnItemClickListener(
          new AdapterView.OnItemClickListener() {
            public void onItemClick(
                AdapterView<?> parent, View view, int position, long id) {
              activity.handleCoordinateClicked(
              activity.squareAdapter.getCoordinate(position));
            }
          });
      return rootView;
    }
  }
  
  private class SeekTaskCallback implements SeekTask.Callback<SeekResponse> {
    @Override
    public void run(SeekResponse response) {
      game = new Game(response.getWhite(), response.getBlack());
      pieceColor = username.equals(game.getWhitePlayer())
          ? Piece.Color.WHITE : Piece.Color.BLACK;
      
      TextView playerNameView =
          (TextView) findViewById(R.id.otherPlayerName);
      playerNameView.setText(game.getPlayer(Piece.otherColor(pieceColor)));
      
      squareAdapter.setOrientation(pieceColor);
      squareAdapter.draw(game.getPosition());
    }
  }
  
  private class EventHandler implements GameEventHandler {
    @Override
    public void handle(GameEvent event) {
      switch (event.getType()) {
        case MOVE_MADE:
          applyMoves(event.getMoveList());
          break;
        default:
          Log.w(getClass().getSimpleName(),
                "Got unhandled event: " + event.toString());
      }
    }
  }
  
  /**
   * Applies the moves received as part of a game event.
   * 
   * <p>Applies locally the first move, which is the move the player indicated.
   * This is needed in order to update the position and calculate legal moves
   * for the next move.
   * The locally calculated moves list should be identical to the received
   * list.  
   */
  private void applyMoves(List<MoveProto> moves) {
    Preconditions.checkArgument(!moves.isEmpty());
    
    Coordinate from = Coordinate.get(moves.get(0).getFrom());
    Coordinate to = Coordinate.get(moves.get(0).getTo());
    Preconditions.checkState(from != null && to != null);
    
    final List<Move> calculatedMoves = game.getPosition().move(from, to);
    Preconditions.checkState(calculatedMoves.size() == moves.size());
    
    board.post(new Runnable() {
      public void run() {
        squareAdapter.move(calculatedMoves);
      }
    });
  }
  
  public void seek() {
    SeekTask seekTask =  new SeekTask(
        this,
        connection.getService(),
        new EventHandler(),
        new SeekTaskCallback());
    seekTask.execute();
  }
  
  private void handleCoordinateClicked(Coordinate coordinate) {
    if (moveSource != null && 
        game.getPosition().getLegalMoves(moveSource).contains(coordinate)) {
      MoveTask moveTask =
          new MoveTask(this, connection.getService(), moveSource, coordinate);
      moveTask.execute();
    } else {
      setMoveSource(coordinate);
    }
  }
  
  private void setMoveSource(Coordinate coordinate) {
    if (game == null || game.getPosition().getActivePlayer() != pieceColor) {
      return;
    }
    
    Piece piece = game.getPosition().getPiecesPlacement().at(coordinate);
    if (piece == null || piece.getColor() != pieceColor) {
      return;
    }
    
    Set<Coordinate> destinations = game.getPosition().getLegalMoves(coordinate);
    if (destinations.isEmpty()) {
      return;
    }
    
    moveSource = coordinate;
    GridView board = (GridView) findViewById(R.id.chessboard);
    SquareAdapter squareAdapter = (SquareAdapter) board.getAdapter();
    squareAdapter.highlight(
        ImmutableList.<Coordinate>builder()
        .add(coordinate)
        .addAll(destinations)
        .build());
  }
}
