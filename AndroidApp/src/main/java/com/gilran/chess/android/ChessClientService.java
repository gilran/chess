package com.gilran.chess.android;

import com.gilran.chess.client.Client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class ChessClientService extends Service {
	private static String EXTRA_USERNAME = "com.gilran.chess.android.username";
	
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
        // Return this instance of LocalService so clients can call public methods
        return ChessClientService.this;
    }
  }

	@Override
	public IBinder onBind(Intent intent) {
		String username = (String) intent.getExtras().get(EXTRA_USERNAME);
		client = new Client("http://localhost:8080/Server/chess/", username);
		return binder;
	}

	
}
