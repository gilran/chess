package com.gilran.chess.android;

import com.google.common.base.Preconditions;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * An async task for GET requests to the chess server.
 *
 * @param <Response> The type of the taks response.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public abstract class AsyncGetTask<Response>
    extends AsyncTask<Void, Void, Response> {
  /** A callback for the async task. */
  public interface Callback<Response> {
    void run(Response response);
  }

  /** The android context. */
  protected Context context;
  /** The chess client service. */
  protected ChessClientService service;
  /**
   * The action message.
   * This message will be displayed while the get request is active, until a
   * response (or a timeout) is received. May be null, in which case, the user
   * will not get an indiaction while the request is pending.
   */
  private String actionMessage;
  /** The error message that will be displayed in case the request failed. */
  private String actionError;
  /** The callback that will be called with the response. */
  private Callback<Response> callback;
  /** A progress dialog that will be used for the action message. */
  private ProgressDialog dialog;

  /**
   * Constructor.
   *
   * @param context The android context.
   * @param service The chess client service.
   * @param actionMessage The message that will be displayed while the get
   *     request is active. If null, no message will be displayed.
   * @param actionError The error message that will be displayed in case the
   *     request failed.
   * @param callback The callback that will be called with the response.
   */
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

  /**
   * The actual work that needs to be done for this task.
   *
   * This method is called from doInBackground, and should use the service in
   * order to make do a GET from the server.
   */
  protected abstract Response run();

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
