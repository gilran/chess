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

/**
 * A service for the chess client.
 *
 * @author Gil Ran <gilrun@gmail.com>
 * */
public class ChessClientService extends Service {
  /** Binder given to clients. */
  private final IBinder binder = new LocalBinder();

  /** The chess client. */
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

  /** The service connection. */
  public static class Connection implements ServiceConnection {
    /** Indicates whether the service is bound or not. */
    private boolean bound = false;
    /** The service. */
    ChessClientService service;

    /** Returns true iff the service is bound. */
    public boolean isBound() { return bound; }
    /** Returns the service. */
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

  /**
   * Logs-in to the server.
   *
   * @param serverAddress The full address of the server. This is the dirname of
   *     the URL of all requests.
   * @param username The username of the local user.
   */
  public LoginResponse login(
      final String serverAddress, final String username) {
    client = new Client(
        serverAddress,
        new LoggerAdapter(
            Thread.currentThread().getStackTrace()[0].getClassName()));
    return client.login(username);
  }

  /**
   * Seeks a new game.
   *
   * @param handler An event handler that will be invoked with each game event
   *     in the new game.
   * @return The response of the seek request.
   */
  public SeekResponse seek(GameEventHandler handler) {
    SeekResponse response = client.seek();
    if (response == null || response.getStatus() != Status.OK) {
      return response;
    }

    client.startListeningToEvents(handler);
    return response;
  }

  /** Sends to the server a move. */
  public ErrorResponse move(Coordinate from, Coordinate to) {
    return client.move(from.toString(), to.toString());
  }

  /** Sends to the server a resignation. */
  public ErrorResponse resign() {
    return client.resign();
  }

  /** Sends to the server a draw offer or acceptance. */
  public ErrorResponse offerOrAcceptDraw() {
    return client.offerOrAcceptDraw();
  }

  /** Sends to the server a draw decline. */
  public ErrorResponse declineDrawOffer() {
    return client.declineDrawOffer();
  }
}
