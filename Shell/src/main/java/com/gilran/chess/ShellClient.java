package com.gilran.chess;

import com.gilran.chess.Proto.ErrorResponse;
import com.gilran.chess.Proto.GameEvent;
import com.gilran.chess.Proto.PositionResponse;
import com.gilran.chess.Proto.Status;
import com.gilran.chess.board.Coordinate;
import com.gilran.chess.board.ForsythEdwardsNotation;
import com.gilran.chess.board.ForsythEdwardsNotation.InvalidFENStringException;
import com.gilran.chess.board.Piece;
import com.gilran.chess.client.Client;
import com.gilran.chess.client.GameEventHandler;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import jline.console.ConsoleReader;

public class ShellClient {
  public static void main(String[] args)
      throws IOException, InterruptedException {
    ShellClient shell = new ShellClient();
    shell.run();
  }

  private Client client;
  private PrintWriter out = new PrintWriter(System.out);

  public ShellClient() {
    out = new PrintWriter(System.out);
  }

  public void run() throws IOException {
    ConsoleReader reader = new ConsoleReader();
    reader.setBellEnabled(false);

    String line;
    while ((line = reader.readLine("prompt> ")) != null) {
      out.flush();

      if (line.equals("exit")) {
        break;
      }

      if (line.isEmpty()) {
        continue;
      }

      List<String> commandArgs = Lists.newArrayList(line.split(" "));
      String commandName = commandArgs.remove(0);
      Method method;
      try {
        method = this.getClass().getMethod(commandName, List.class);
        method.invoke(this, commandArgs);
      } catch (Exception e) {
        print("Unknown command: " + commandName + "\n");
      }
    }
  }

  private void print(String message) {
    out.print(message);
    out.flush();
  }

  public void login(List<String> args) {
    if (args.size() != 1) {
      print("Usage: login <username>\n");
      return;
    }
    client = new Client("http://localhost:8080/Server/chess/");
    print(client.login(args.get(0)).toString());
  }

  public void seek(List<String> args) {
    if (args.size() != 0) {
      print("Usage: seek\n");
      return;
    }
    if (client == null) {
      print("Not connected. Please login.");
      return;
    }
    print(client.seek().toString());
    client.startListeningToEvents(new GameEventHandler() {
      @Override
      public void handle(GameEvent event) {
        print("\nGot new event:\n" + event.toString() + "prompt> ");
      }
    });
  }

  public void move(List<String> args) {
    if (args.size() != 2) {
      print("Usage: move <from> <to>\n");
      return;
    }
    if (client == null) {
      print("Not connected. Please login.\n");
      return;
    }
    print(client.move(args.get(0), args.get(1)).toString());
  }
  
  private void callSimpleMethod(String methodName, List<String> args) {
    if (args.size() != 0) {
      print("Usage: " + methodName + "\n");
      return;
    }
    if (client == null) {
      print("Not connected. Please login.");
      return;
    }
    print(client.callSimpleMethod(methodName, ErrorResponse.class).toString());
  }
  
  public void resign(List<String> args) {
    callSimpleMethod(
        Thread.currentThread().getStackTrace()[1].getMethodName(), args);
  }
  
  public void offerDraw(List<String> args) {
    callSimpleMethod(
        Thread.currentThread().getStackTrace()[1].getMethodName(), args);
  }
  
  public void declineDrawOffer(List<String> args) {
    callSimpleMethod(
        Thread.currentThread().getStackTrace()[1].getMethodName(), args);
  }

  private String getFen() {
    PositionResponse response = client.getPosition();
    if (response.getStatus() != Status.OK) {
      print(response.toString() + "\n");
      return null;
    }
    return response.getFen();
  }
  
  public void fen(List<String> args) {
    if (args.size() != 0) {
      print("Usage: print\n");
      return;
    }
    if (client == null) {
      print("Not connected. Please login.");
      return;
    }
    String fen = getFen();
    if (fen != null) {
      print(fen + "\n");
    }
  }
  
  public void print(List<String> args) {
    if (args.size() != 0) {
      print("Usage: print\n");
      return;
    }
    if (client == null) {
      print("Not connected. Please login.");
      return;
    }
    
    String fen = getFen();
    if (fen == null) {
      return;
    }
    
    ForsythEdwardsNotation position;
    try {
      position = new ForsythEdwardsNotation(fen);
    } catch (InvalidFENStringException e) {
      print(e.toString() + "\n");
      return;
    }

    print("    A   B   C   D   E   F   G   H  \n");
    print("  +---+---+---+---+---+---+---+---+\n");
    for (int r = Coordinate.LAST_RANK; r >= Coordinate.FIRST_RANK; --r) {
      print(String.format("%d ", r + 1));
      for (int f = Coordinate.FIRST_FILE; f <= Coordinate.LAST_FILE; ++f) {
        Piece piece = position.getPiecesPlacement().at(Coordinate.get(f, r));
        Character pieceName =
            piece == null ? ' ' : ForsythEdwardsNotation.getPieceName(piece); 
        print("| " + pieceName + " ");
      }
      print(String.format("| %d\n", r + 1));
      print("  +---+---+---+---+---+---+---+---+\n");
    }
    print("    A   B   C   D   E   F   G   H  \n");
  }
}
