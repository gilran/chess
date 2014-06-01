package com.gilran.chess.android;

import com.gilran.chess.client.Client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ChessClientService extends Service {
	// Binder given to clients
  private final IBinder binder = new LocalBinder();
  
  private Client client;
  
  /**
   * Class used for the client Binder. Because we know this service always
   * runs in the same process as its clients, we don't need to deal with IPC.
   */
  public class LocalBinder extends Binder {
  	ChessClientService getService() {
        // Return this instance of LocalService so clients can call public methods
        return ChessClientService.this;
    }
  }

	@Override
	public IBinder onBind(Intent intent) {
		//client = new Client(baseUrl, username);
		return binder;
	}

	
}
