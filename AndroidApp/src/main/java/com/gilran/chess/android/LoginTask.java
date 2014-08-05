package com.gilran.chess.android;

import android.content.Context;

import com.gilran.chess.Proto.LoginResponse;

/** An async task for logging-in to the server. */
class LoginTask extends AsyncGetTask<LoginResponse> {
  /** The local username. */
  private String username;
  /** The server address. */
  private String serverAddress;

  /**
   * Constructor.
   * 
   * @param context The android context.
   * @param service The chess client service.
   * @param serverAddress The address of the server.
   * @param username The username of the local user.
   * @param callback A callback that will be called with the login response.
   */
  public LoginTask(
      Context context,
      ChessClientService service,
      String serverAddress,
      String username,
      Callback<LoginResponse> callback) {
    super(
        context,
        service,
        context.getResources().getString(R.string.logging_in),
        context.getResources().getString(R.string.login_failed),
        callback);
    this.serverAddress = serverAddress;
    this.username = username;
  }

  @Override
  protected LoginResponse run() {
    return service.login(serverAddress, username);
  }
}
