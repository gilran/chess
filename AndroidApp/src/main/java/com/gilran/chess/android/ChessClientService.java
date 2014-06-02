package com.gilran.chess.android;

import com.gilran.chess.Proto.LoginResponse;
import com.gilran.chess.client.Client;

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
    // TODO(gilran): Add the server address to the settings.
    client = new Client(
        "http://192.168.1.162:8080/Server/chess/",
        new LoggerAdapter(Thread.currentThread().getStackTrace()[0].getClassName()));

    return binder;
  }

  public LoginResponse login(final String username) {
    return client.login(username);
  }

}
