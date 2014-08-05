package com.gilran.chess.android;

import com.gilran.chess.Proto.ErrorResponse;
import com.gilran.chess.Proto.GameEvent;
import com.gilran.chess.Proto.GameStatus;
import com.gilran.chess.Proto.MoveProto;
import com.gilran.chess.Proto.SeekResponse;
import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.Game;
import com.gilran.chess.board.Move;
import com.gilran.chess.board.Piece;
import com.gilran.chess.board.Piece.Color;
import com.gilran.chess.client.GameEventHandler;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The board activity of the Chess android application.
 *
 * This activity is most of the application, managing seeks and all in-game
 * actions.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class BoardActivity extends Activity {
  /** The identifier of the extra message containing the local player name. */
  static final String EXTRA_LOCAL_PLAYER_NAME =
      "com.gilran.chess.android.EXTRA_LOCAL_PLAYER_NAME";

  /** A map from game statuses to the resource id of its description string. */
  static final Map<GameStatus, Integer> GAME_STATUS_MESSAGES;

  static {
    GAME_STATUS_MESSAGES = ImmutableMap.<GameStatus, Integer>builder()
        .put(GameStatus.WHITE_TO_MOVE, R.string.white_to_move)
        .put(GameStatus.BLACK_TO_MOVE, R.string.black_to_move)
        .put(GameStatus.WHITE_CHECKED, R.string.white_checked)
        .put(GameStatus.BLACK_CHECKED, R.string.black_checked)
        .put(GameStatus.BLACK_CHECKMATED, R.string.black_checkmated)
        .put(GameStatus.BLACK_RESIGNED, R.string.black_resigned)
        .put(GameStatus.BLACK_CLOCK_EXPIRED, R.string.black_clock_expired)
        .put(GameStatus.WHITE_CHECKMATED, R.string.white_checkmated)
        .put(GameStatus.WHITE_RESIGNED, R.string.white_resigned)
        .put(GameStatus.WHITE_CLOCK_EXPIRED, R.string.white_clock_expired)
        .put(GameStatus.BLACK_STALEMATED, R.string.black_stalemated)
        .put(GameStatus.WHITE_STALEMATED, R.string.white_stalemated)
        .put(GameStatus.INSUFFICIENT_MATERIAL, R.string.insufficient_material)
        .put(GameStatus.HALFMOVE_CLOCK_EXPIRED, R.string.halfmove_clock_expired)
        .put(GameStatus.THREEFOLD_REPETITION, R.string.threefold_repetition)
        .put(GameStatus.DRAW_BY_AGREEMENT, R.string.draw_by_agreement)
        .build();
  }

  /** A connection to the chess client service. */
  private ChessClientService.Connection connection =
      new ChessClientService.Connection();
  /** The local player username. */
  private String username;
  /** The active game. Used only during a game. */
  private Game game;
  /** The piece color of the local player. Used only during a game. */
  private Piece.Color pieceColor;
  /**
   * The from coordinate of a move.
   * Used only during a game, only when it's the local player's turn, and only
   * when the user chose a source square for a move.
   */
  private Coordinate moveSource;
  /** A grid view used for drawing the board. */
  private GridView board;
  /** A square adapter for the board grid view. */
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
  public boolean onPrepareOptionsMenu (Menu menu) {
    boolean duringGame = game != null;
    boolean allowDrawOffer =
        duringGame && game.getOutstandingDrawOffer() == null;
    boolean hasPendingOtherPlayerDraw =
        duringGame &&
        game.getOutstandingDrawOffer() == Piece.otherColor(pieceColor);

    // Seek.
    menu.getItem(0).setEnabled(!duringGame);
    // Resign.
    menu.getItem(1).setEnabled(duringGame);
    // Offer draw.
    menu.getItem(2).setEnabled(allowDrawOffer);
    // Accept draw offer.
    menu.getItem(3).setEnabled(hasPendingOtherPlayerDraw);
    // Decline draw offer.
    menu.getItem(4).setEnabled(hasPendingOtherPlayerDraw);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    switch (item.getItemId()) {
      case R.id.action_seek:
        seek();
        break;
      case R.id.action_resign:
        resign();
        break;
      case R.id.action_draw:
      case R.id.action_accept_draw:
        offerOrAcceptDraw();
        break;
      case R.id.action_decline_draw:
        declineDraw();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  /** A fragment for the game board. */
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

  /** A callback for seek. */
  private class SeekTaskCallback implements SeekTask.Callback<SeekResponse> {
    @Override
    public void run(SeekResponse response) {
      game = new Game(response.getWhite(), response.getBlack());
      pieceColor = username.equals(game.getWhitePlayer())
          ? Piece.Color.WHITE : Piece.Color.BLACK;

      TextView playerNameView =
          (TextView) findViewById(R.id.otherPlayerName);
      playerNameView.setText(game.getPlayer(Piece.otherColor(pieceColor)));

      squareAdapter.draw(game.getPosition(), pieceColor);
      markActivePlayer(game.getPosition().getActivePlayer());
    }
  }

  /** Adds the active player marker to the active player. */
  private void markActivePlayer(Color activePlayer) {
    TextView localPlayerNameView =
        (TextView) findViewById(R.id.localPlayerName);
    TextView otherPlayerNameView =
        (TextView) findViewById(R.id.otherPlayerName);
    String localPlayerName = game.getPlayer(pieceColor);
    String otherPlayerName = game.getPlayer(Piece.otherColor(pieceColor));
    String activePlayerMarker =
        getResources().getString(R.string.active_player_marker);
    if (pieceColor == activePlayer) {
      localPlayerName = activePlayerMarker + " " + localPlayerName;
    } else {
      otherPlayerName = activePlayerMarker + " " + otherPlayerName;
    }
    localPlayerNameView.setText(localPlayerName);
    otherPlayerNameView.setText(otherPlayerName);
  }

  /** Resets the board to an empty board with no game. */
  private void resetBoard() {
    TextView localPlayerNameView =
        (TextView) findViewById(R.id.localPlayerName);
    TextView otherPlayerNameView =
        (TextView) findViewById(R.id.otherPlayerName);
    localPlayerNameView.setText(username);
    otherPlayerNameView.setText(
        getResources().getString(R.string.player_name_placeholder));
    squareAdapter.clear();
  }

  /** An event handler for the game events. */
  private class EventHandler implements GameEventHandler {
    @Override
    public void handle(GameEvent event) {
      switch (event.getType()) {
        case MOVE_MADE:
          handleDrawOfferDecline();
          applyMoves(event.getMoveList());
          break;
        case GAME_ENDED:
          gameEnded(event.getStatus());
          break;
        case WHITE_OFFERED_DRAW:
          handleDrawOffer(Piece.Color.WHITE);
          break;
        case BLACK_OFFERED_DRAW:
          handleDrawOffer(Piece.Color.BLACK);
          break;
        case DRAW_OFFER_DECLINED:
        case DRAW_OFFER_WITHDRAWN:
          handleDrawOfferDecline();
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
        markActivePlayer(game.getPosition().getActivePlayer());
      }
    });
  }

  /** Handles a draw offer. */
  public void handleDrawOffer(final Piece.Color color) {
    game.setOutstandingDrawOffer(color);
    final TextView gameMessageView = (TextView) findViewById(R.id.gameMessage);
    gameMessageView.post(new Runnable() {
      @Override
      public void run() {
        if (color == pieceColor) {
          gameMessageView.setText(
              getResources().getString(R.string.outstanding_draw_offer));
        } else {
          gameMessageView.setText(
              game.getPlayer(color) + " " +
              getResources().getString(R.string.offered_draw));
        }
      }
    });
  }

  /** Handles a draw offer decline or withdraw. */
  public void handleDrawOfferDecline() {
    game.setOutstandingDrawOffer(null);
    final TextView gameMessageView = (TextView) findViewById(R.id.gameMessage);
    gameMessageView.post(new Runnable() {
      @Override
      public void run() {
        gameMessageView.setText("");
      }
    });
  }

  /** Called when the game has ended. */
  private void gameEnded(final GameStatus status) {
    game = null;
    board.post(new Runnable() {
      @Override
      public void run() {
        final String message =
            getResources().getString(GAME_STATUS_MESSAGES.get(status));
        new AlertDialog.Builder(BoardActivity.this)
            .setTitle(getResources().getString(R.string.game_ended))
            .setMessage(message)
            .setPositiveButton(R.string.ok, new OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                resetBoard();
              }
            })
            .show();
      }
    });
  }

  /** Seeks an opponent. */
  private void seek() {
    SeekTask seekTask = new SeekTask(
        this,
        connection.getService(),
        new EventHandler(),
        new SeekTaskCallback());
    seekTask.execute();
  }

  /**
   * Offers draw or accepts pending draw offer.
   * Behind the scenes, both of these actions are the same.
   */
  private void offerOrAcceptDraw() {
    AsyncGetTask<ErrorResponse> drawTask = new AsyncGetTask<ErrorResponse>(
        this,
        connection.getService(),
        null /* actionMessage */,
        getResources().getString(R.string.draw_offer_failed),
        null /* callback */) {
      @Override
      protected ErrorResponse run() {
        return service.offerOrAcceptDraw();
      }
    };
    drawTask.execute();
  }

  /** Declines a pending draw offer. */
  private void declineDraw() {
    AsyncGetTask<ErrorResponse> declineDrawTask =
      new AsyncGetTask<ErrorResponse>(
          this,
          connection.getService(),
          null /* actionMessage */,
          getResources().getString(R.string.draw_decline_failed),
          null /* callback */) {
        @Override
        protected ErrorResponse run() {
          return service.declineDrawOffer();
        }
      };
    declineDrawTask.execute();
  }

  /** Resigns the game. */
  private void resign() {
    AsyncGetTask<ErrorResponse> resignTask = new AsyncGetTask<ErrorResponse>(
        this,
        connection.getService(),
        null /* actionMessage */,
        getResources().getString(R.string.resign_failed),
        null /* callback */) {
      @Override
      protected ErrorResponse run() {
        return service.resign();
      }
    };
    resignTask.execute();
  }

  /**
   * Handles a click on a coordinate on the board.
   * If this is the first click, sets the coordinate as the move source square.
   * If there is a move source square, tries to make the move.
   */
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

  /** Sets the current move's source. */
  private void setMoveSource(Coordinate coordinate) {
    if (game == null || game.getPosition().getActivePlayer() != pieceColor) {
      // Not this players turn.
      return;
    }

    Piece piece = game.getPosition().getPiecesPlacement().at(coordinate);
    if (piece == null || piece.getColor() != pieceColor) {
      // There isn't a piece of this player's color in the square.
      return;
    }

    Set<Coordinate> destinations = game.getPosition().getLegalMoves(coordinate);
    if (destinations.isEmpty()) {
      // The piece in this square can't move.
      return;
    }

    // Legal source square.
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
