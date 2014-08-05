package com.gilran.chess.android;

import com.gilran.chess.client.Client;

import com.google.common.base.Preconditions;

import android.util.Log;

/**
 * An implementation of the chess client logger adapter for android log library.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class LoggerAdapter implements Client.LoggerAdapter {
  private String tag;

  /**
   * Constructor.
   *
   * @param tag The tag that will be used for all logging by this adapter.
   */
  public LoggerAdapter(String tag) {
    this.tag = tag;
  }

  @Override
  public void log(Level level, String message) {
    switch (level) {
      case DEBUG:
        Log.d(tag, message);
        break;
      case INFO:
        Log.i(tag, message);
        break;
      case WARNING:
        Log.w(tag, message);
        break;
      case ERROR:
        Log.e(tag, message);
        break;
      default:
        Preconditions.checkState(false);
    }
  }
}
