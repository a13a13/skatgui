// (c) Michael Buro, licensed under GPLv3

package common;

public interface TableCallbacks
{
  public void fillInGameData(SimpleGame sg);
  public ClientData getClientData(String id, boolean connected);
  public String clientToInvite(String invPlayer, Table table);
  public boolean communicationAllowed(String client);
  public void saveGame(String sgf);
  public void saveTable(String tableId, StringBuffer sb);
  public String archive(Table table, StringBuffer sb);
  public void send(String to, String what);
  public void sendToAll(String what);
  public long nextGameId();
  public long nextSeriesId();
  public boolean isShuttingDown();
}
