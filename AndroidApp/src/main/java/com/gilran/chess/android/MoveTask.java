package com.gilran.chess.android;

import com.gilran.chess.Proto.ErrorResponse;
import com.gilran.chess.board.Coordinate;

import com.google.common.base.Preconditions;

import android.content.Context;

/**
 * An async task for reporting to the server that a move was made.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class MoveTask extends AsyncGetTask<ErrorResponse> {
  /** The from coordinate of the move. */
  private Coordinate from;
  /** The to Coordinate of the move. */
  private Coordinate to;

  /** Constructor.
   *
   * @param context The android context.
   * @param service The chess client service.
   * @param from The from coordinate of the move.
   * @param to The to Coordinate of the move.
   */
  public MoveTask(
      Context context,
      ChessClientService service,
      Coordinate from,
      Coordinate to) {
    super(
        context,
        service,
        null /* actionMessage */,
        context.getResources().getString(R.string.move_failed),
        null /* callback */);
    this.from = Preconditions.checkNotNull(from);
    this.to = Preconditions.checkNotNull(to);
  }

  @Override
  protected ErrorResponse run() {
    return service.move(from, to);
  }

}
