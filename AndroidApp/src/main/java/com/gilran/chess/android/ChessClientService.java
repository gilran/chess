package com.gilran.chess.android;

import com.gilran.chess.Proto.*;
import com.gilran.chess.board.Coordinate;
import com.gilran.chess.client.Client;
import com.gilran.chess.client.GameEventHandler;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ChessClientService extends Service {
  // Binder given to clients.
  private final IBinder binder = new LocalBinder();

  // The client itself.
  private Client client;

  /**
   * Class used for the client Binder. Because we know this service always
   * runs in the same process as its clients, we don't need to deal with IPC.
   */
  public class LocalBinder extends Binder {
    ChessClientService getService() {
      return ChessClientService.this;
    }
  }

  public static class Connection implements ServiceConnection {
    private boolean bound = false;
    ChessClientService service;

    public boolean isBound() { return bound; }
    public ChessClientService getService() { return service; }

    @Override
    public void onServiceConnected(ComponentName className, IBinder binder) {
        Log.i(getClass().getName(), "Service connected.");
        ChessClientService.LocalBinder localBinder =
            (ChessClientService.LocalBinder) binder;
        service = localBinder.getService();
        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      bound = false;
    }
  };

  @Override
  public IBinder onBind(Intent intent) {
    return binder;
  }

  public LoginResponse login(
      final String serverAddress, final String username) {
    client = new Client(
        serverAddress,
        new LoggerAdapter(
            Thread.currentThread().getStackTrace()[0].getClassName()));
    return client.login(username);
  }

  public SeekResponse seek(GameEventHandler handler) {
    SeekResponse response = client.seek();
    if (response == null || response.getStatus() != Status.OK) {
      return response;
    }

    client.startListeningToEvents(handler);
    return response;
  }

  public ErrorResponse move(Coordinate from, Coordinate to) {
    return client.move(from.toString(), to.toString());
  }

  public ErrorResponse resign() {
    return client.resign();
  }

  public ErrorResponse offerOrAcceptDraw() {
    return client.offerOrAcceptDraw();
  }

  public ErrorResponse declineDrawOffer() {
    return client.declineDrawOffer();
  }
}
