package com.gilran.chess.android;

import com.gilran.chess.Proto.LoginResponse;
import com.gilran.chess.android.AsyncGetTask.Callback;
import com.gilran.chess.client.Client.LoggerAdapter.Level;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

/**
 * The login activity of the Chess android application.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class LoginActivity extends Activity {
  /** A logger adapter. */
  LoggerAdapter logger = new LoggerAdapter(getClass().getSimpleName());
  ChessClientService.Connection connection =
      new ChessClientService.Connection();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);

    if (savedInstanceState == null) {
      getFragmentManager().beginTransaction()
          .add(R.id.container, new TopLevelFragment()).commit();
    }

    if (!connection.isBound()) {
      bindService(
          new Intent(this, ChessClientService.class),
          connection,
          Context.BIND_AUTO_CREATE);
    }
  }

  @Override
  protected void onDestroy() {
    if (connection.isBound()) {
      unbindService(connection);
    }
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.login, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /** The top-level fragment of the activity. */
  public static class TopLevelFragment extends Fragment {
    public View onCreateView(
        LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState) {
      View rootView =
          inflater.inflate(R.layout.fragment_login, container, false);
      return rootView;
    }
  }

  /** Logs-in to the server. */
  public void login(View view) {
    EditText serverAddressEditText =
      (EditText) findViewById(R.id.serverAddress);
    EditText usernameEditText = (EditText) findViewById(R.id.loginUsername);
    final String serverAddress = serverAddressEditText.getText().toString();
    final String username = usernameEditText.getText().toString();
    if (username.isEmpty()) {
      logger.log(Level.DEBUG, "Empty user name.");
      Toast.makeText(
          this,
          getResources().getString(R.string.enter_user_name),
          Toast.LENGTH_LONG).show();
      return;
    }

    LoginTask loginTask = new LoginTask(
        this,
        connection.getService(),
        serverAddress,
        username, new Callback<LoginResponse>() {
      @Override
      public void run(LoginResponse response) {
        Intent boardIntent =
            new Intent(LoginActivity.this, BoardActivity.class);
        boardIntent.putExtra(BoardActivity.EXTRA_LOCAL_PLAYER_NAME, username);
        startActivity(boardIntent);
      }
    });
    loginTask.execute();
  }
}
