package com.gilran.chess.server;

import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Maps;

public class Session {
  /** The session token. */
  private String token;
  /** The username of the user that the session belongs to. */
  private String username;
  /** The active games of this session. */
  private Map<String, Game> games;

  public Session(String username) {
    this.token = UUID.randomUUID().toString();
    this.username = username;
    this.games = Maps.newHashMap(); 
  }

  public String getToken() { return token; }
  public String getUsername() { return username; }
  
  public void addGame(Game game) { games.put(game.getId(), game); } 
  public Game getGame(String id) { return games.get(id); }
}

