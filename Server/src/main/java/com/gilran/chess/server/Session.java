package com.gilran.chess.server;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.UUID;

/**
 * A client session.
 *
 * @author Gil Ran <gilrun@gmail.com>
 */
public class Session {
  /** The session token. */
  private String token;
  /** The username of the user that the session belongs to. */
  private String username;
  /** The active games of this session. */
  private Map<String, Game> games;

  /** Constructor. */
  public Session(String username) {
    this.token = UUID.randomUUID().toString();
    this.username = username;
    this.games = Maps.newHashMap();
  }

  /** Returns the session token. */
  public String getToken() { return token; }
  /** Returns the username of the session's user. */
  public String getUsername() { return username; }

  /** Adds a game to the session. */
  public void addGame(Game game) { games.put(game.getId(), game); }
  /** Gets a game that is associated with this session. */
  public Game getGame(String id) { return games.get(id); }
}

