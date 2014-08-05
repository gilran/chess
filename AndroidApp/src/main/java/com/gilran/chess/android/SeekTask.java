package com.gilran.chess.android;

import com.gilran.chess.Proto.SeekResponse;
import com.gilran.chess.client.GameEventHandler;

import android.content.Context;

/** An async task for seeking a game. */
public class SeekTask extends AsyncGetTask<SeekResponse> {
  /** An event handler that is called for every game event in the new game. */
  private GameEventHandler gameEventHandler;

  /**
   * Constructor.
   *
   * @param context The android context.
   * @param service The chess client service.
   * @param gameEventHandler An event handler for game events in the new
   *     game.
   * @param callback A callback that will be called with the seek response.
   */
  public SeekTask(
      Context context,
      ChessClientService service,
      GameEventHandler gameEventHandler,
      Callback<SeekResponse> callback) {
    super(
        context,
        service,
        context.getResources().getString(R.string.seeking_an_opponent),
        context.getResources().getString(R.string.seek_error_message),
        callback);
    this.gameEventHandler = gameEventHandler;
  }

  @Override
  protected SeekResponse run() {
    return service.seek(gameEventHandler);
  }
}
