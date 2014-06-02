package com.gilran.chess;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import com.gilran.chess.Proto.GameEvent;
import com.gilran.chess.client.Client;
import com.gilran.chess.client.EventsListenerThread;
import com.google.common.collect.Lists;

import jline.console.ConsoleReader;

public class ShellClient {
	public static void main(String[] args) throws IOException, InterruptedException {
		ShellClient shell = new ShellClient();
		shell.Run();
	}
	
	private Client client;
	private PrintWriter out = new PrintWriter(System.out);
	
	public ShellClient() {
		out = new PrintWriter(System.out);
	}
	
	public void Run() throws IOException {
		ConsoleReader reader = new ConsoleReader();
		reader.setBellEnabled(false);
		
		String line;
		while ((line = reader.readLine("prompt> ")) != null) {
			out.flush();
			
			if (line.equals("exit"))
				break;
			
			if (line.isEmpty())
				continue;
			
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
		client.startListeningToEvents(new EventsListenerThread.EventHandler() {
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
}
