package com.gilran.chess.android;

import com.gilran.chess.Proto.SeekResponse;
import com.gilran.chess.client.GameEventHandler;

import android.content.Context;

public class SeekTask extends AsyncGetTask<SeekResponse> {
  private static final String ACTION_MESSAGE = "Seeking an opponent...";
  private static final String ERROR_MESSAGE = "Seek found no match.";
  
  private GameEventHandler gameEventHandler;

  public SeekTask(
      Context context,
      ChessClientService service,
      GameEventHandler gameEventHandler,
      Callback<SeekResponse> callback) {
    super(context, service, ACTION_MESSAGE, ERROR_MESSAGE, callback);
    this.gameEventHandler = gameEventHandler;
  }

  @Override
  protected SeekResponse run() {
    return service.seek(gameEventHandler);
  }
}
