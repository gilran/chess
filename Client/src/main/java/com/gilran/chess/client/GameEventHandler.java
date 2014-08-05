package com.gilran.chess.client;

import com.gilran.chess.Proto.GameEvent;

/**
 * An interface for a game event handler.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public interface GameEventHandler {
  void handle(GameEvent event);
}
