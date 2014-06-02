package com.gilran.chess.android;

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
      case INFO:
        Log.i(tag, message);
      case WARNING:
        Log.w(tag, message);
      case ERROR:
        Log.e(tag, message);
      default:
        assert false;
    }
  }
}
