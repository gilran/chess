package com.gilran.chess.client;

import com.gilran.chess.client.Client.LoggerAdapter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.logging.Logger;

/**
 * A chess client logger adapter using standard logging.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class DefaultLogger implements LoggerAdapter {
  /** A map from logger adapter log levels to standard log levels. */
  static final Map<LoggerAdapter.Level, java.util.logging.Level> LEVELS =
      ImmutableMap.<LoggerAdapter.Level, java.util.logging.Level>builder()
      .put(Level.DEBUG, java.util.logging.Level.FINE)
      .put(Level.INFO, java.util.logging.Level.INFO)
      .put(Level.WARNING, java.util.logging.Level.WARNING)
      .put(Level.ERROR, java.util.logging.Level.SEVERE)
      .build();
  
  /** The logger itself. */
  private Logger logger = Logger.getLogger(
      Thread.currentThread().getStackTrace()[1].getClassName());

  @Override
  public void log(LoggerAdapter.Level level, String message) {
    logger.log(LEVELS.get(level), message);
  }
}
