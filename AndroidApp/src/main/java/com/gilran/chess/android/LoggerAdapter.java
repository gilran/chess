package com.gilran.chess.android;

import com.google.common.base.Preconditions;

import android.util.Log;

import com.gilran.chess.client.Client;

public class LoggerAdapter implements Client.LoggerAdapter {
  private String tag;

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
