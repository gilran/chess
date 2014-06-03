package com.gilran.chess.android;

import android.content.Context;

import com.gilran.chess.Proto.MoveResponse;
import com.gilran.chess.board.Coordinate;
import com.google.common.base.Preconditions;

public class MoveTask extends AsyncGetTask<MoveResponse> {
  private static final String ERROR_MESSAGE = "Failed to send move.";

  private Coordinate from;
  private Coordinate to;
  
  public MoveTask(
      Context context,
      ChessClientService service,
      Coordinate from,
      Coordinate to) {
    super(
        context,
        service,
        null /* actionMessage */,
        ERROR_MESSAGE,
        null /* callback */);
    this.from = Preconditions.checkNotNull(from);
    this.to = Preconditions.checkNotNull(to);
  }

  @Override
  protected MoveResponse run() {
    return service.move(from, to);
  }

}
