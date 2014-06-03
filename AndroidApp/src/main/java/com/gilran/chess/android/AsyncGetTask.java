package com.gilran.chess.android;

import com.google.common.base.Preconditions;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public abstract class AsyncGetTask<Response>
    extends AsyncTask<Void, Void, Response> {
  public interface Callback<Response> {
    void run(Response response);
  }

  protected Context context;
  protected ChessClientService service;
  private String actionMessage;
  private String actionError;
  private Callback<Response> callback;
  private ProgressDialog dialog;

  public AsyncGetTask(
      Context context,
      ChessClientService service,
      String actionMessage,
      String actionError,
      Callback<Response> callback) {
    super();
    this.context = context;
    this.service = service;
    this.actionMessage = actionMessage;
    this.actionError = Preconditions.checkNotNull(actionError);
    this.callback = callback;
  }

  @Override
  protected void onPreExecute() {
    if (actionMessage != null) {
      dialog = new ProgressDialog(context);
      dialog.setMessage(actionMessage);
      dialog.show();
    }
  }
  
  @Override
  protected Response doInBackground(Void... params) {
    return run();
  }
  
  protected abstract Response run(); 

  @Override
  protected void onPostExecute(Response response) {
    if (dialog != null) {
      dialog.dismiss();
    }
    
    if (response == null) {
      Toast.makeText(context, actionError, Toast.LENGTH_LONG).show();
      return;
    }

    Log.d(this.getClass().getSimpleName(), response.toString());
    if (callback != null) {
      callback.run(response);
    }
  }
}
