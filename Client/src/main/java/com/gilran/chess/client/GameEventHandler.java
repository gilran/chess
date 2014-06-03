package com.gilran.chess.client;

import com.gilran.chess.Proto.GameEvent;

public interface GameEventHandler {
  void handle(GameEvent event);
}