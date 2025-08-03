// $Id$

// Card service client

// (c) Michael Buro, licensed under GPLv3

package client;

import java.util.*;
import common.*;

public class ServiceClient implements TableCallbacks
{
  public String viewerId;
  public String place; // where do we play?
  public TreeMap<String, ClientData> id2client;
  public TreeMap<String, String> id2TableName;
  public TreeMap<String, Table> id2JoinedTable;
  public TreeMap<String, Table> id2ObservedTable;
  public TreeMap<String, TournamentData> id2TourData;

  /** @parm name of client */
  public ServiceClient(String place, String viewerId)
  {
    this.viewerId = viewerId;
    this.place = place;
    id2client = new TreeMap<String, ClientData>();
    id2TableName = new TreeMap<String, String>();
    id2JoinedTable  = new TreeMap<String, Table>();
    id2ObservedTable  = new TreeMap<String, Table>();
    id2TourData = new TreeMap<String, TournamentData>();
  }

  // TableCallbacks functions
  public void fillInGameData(SimpleGame sg) { Misc.err("not implemented 1"); }
  public ClientData getClientData(String id, boolean connected) { return id2client.get(id); }
  public boolean communicationAllowed(String client) { Misc.err("not implemented 2"); return false; }
  public String clientToInvite(String invPlayer, Table table) { Misc.err("not implemented 2"); return null; }
  public void saveGame(String sgf) { Misc.err("not implemented 3"); }
  public void saveTable(String id, StringBuffer sb) { Misc.err("not implemented 99"); }      
  public String archive(Table table, StringBuffer sb) { Misc.err("not implemented 100"); return ""; }
  public void send(String to, String what) { Misc.err("not implemented 4"); }
  public void sendToAll(String what) { Misc.err("not implemented 111"); }  
  public long nextGameId() { return 0; } // dummy
  public long nextSeriesId() { return 0; } // dummy
  public boolean isShuttingDown() { return false; } // dummy
  
  public TreeMap<String, ClientData> getClients() { return id2client; }

  public TreeMap<String, Table> getJoinedTables() { return id2JoinedTable; }
  
  public TreeMap<String, Table> getObservedTables() { return id2ObservedTable; }  

  public TreeMap<String, String> getTableNames() { return id2TableName; }  

  public TreeMap<String, TournamentData> getTourData() { return id2TourData; }  
  
  /** change state according to message and
   *  @return message object, or null if no message was received
   */
  public ServiceMsg received(String msg)
  {
    String[] parts = msg.split(" ");
    SReader sr = new SReader(msg);

    if (parts[0].equals("")) {  // empty message
      ServiceMsg sm = new CatchAllMsg();
      sm.parse(parts, sr);
      return sm;
    }

    for (ServiceMsg m : msgTypes) {
      if (m.parse(parts, sr)) {
	return m;
      }
    }

    Misc.msg("RECEIVED BUT NOT HANDLED: " + msg);
    
    return null;
  }


  public interface ServiceMsgHandler
  {
    public void handleServiceMsg(ServiceMsg msg, String line);
  }

  abstract public class ServiceMsg {
    abstract boolean parse(String[] s, SReader sr);
  }

  public class TellMsg extends ServiceMsg
  {
    public String fromId;
    public String text;

    // tell <userid> <text>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 2 || !s[0].equals("tell"))
	return false;

      fromId = s[1];
      sr.nextWord(); sr.nextWord();
      text = sr.rest();
      return true;
    }
  }
  
  public class TextMsg extends ServiceMsg
  {
    public String textId;
    public String text;

    // text <textid> <text>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 2 || !s[0].equals("text") || s[1].length() == 0)
	return false;

      textId = s[1];
      sr.nextWord(); sr.nextWord();
      text = Misc.decodeCRLF(sr.rest());
      return true;
    }
  }
  
  public class FingerMsg extends ServiceMsg
  {
    public String name;
    public String info;

    // finger <name> <info>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 2 || !s[0].equals("finger") || s[1].length() == 0)
	return false;

      name = s[1];
      sr.nextWord(); sr.nextWord();
      info = Misc.decodeCRLF(sr.rest());
      return true;
    }
  }
  
  public class TimeMsg extends ServiceMsg
  {
    public String date;
    public String time;

    // finger <name> <info>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 3 || !s[0].equals("time"))
	return false;

      date = s[1];
      time = s[2];
      return true;
    }
  }
  
  public class YellMsg extends ServiceMsg
  {
    public String fromId;
    public String text;

    // yell <userid> <text>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 2 || !s[0].equals("yell"))
	return false;

      fromId = s[1];
      sr.nextWord(); sr.nextWord();
      text = sr.rest();
      return true;
    }
  }

  // user joins/observes table
  public class CreateMsg extends ServiceMsg
  {
    public Table table;
    public boolean isPlayer;
    
    // create <table-id> <playerOnTableName|.=observer> <table-type> [<blockNum> <scoresheet>]
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("create"))
        return false;

      String tableId = s[1];
      String myNameOnTable = s[2];
      isPlayer = !myNameOnTable.equals(Table.NO_NAME);
      String tableType = s[3];
      
      TreeMap<String, Table> tableMap = id2ObservedTable;
      if (isPlayer)
        tableMap = id2JoinedTable;

      String id = Table.getTableViewerId(tableId, myNameOnTable);
      table = tableMap.get(id);

      if (table != null)
        Misc.err("create: table already exists");

      // table does not exist yet: create
        
      table = new Table(getThis());
      String r = table.init(null, tableId, null, tableType, myNameOnTable, null);
      if (r != null)
        Misc.err("table init failed: " + sr.rest() + " : " + r);

      if (s.length >= 5) {

        // new server
        int bn = Integer.parseInt(s[4]);
        if (bn > 0) table.setTourny(bn);

        // skip prefix
        for (int i=0; i < 5; i++) sr.nextWord();

        if (s.length >= 6) {
          table.readScoreSheet(sr);
        }
      }
      
      tableMap.put(id, table);
      return true;
    }    
  }

  // user no longer receives table data
  public class DestroyMsg extends ServiceMsg
  {
    public Table table;
    public boolean isPlayer;
    
    // destroy <table-id> <player|.=observer>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 3 || !s[0].equals("destroy"))
	return false;

      String tableId = s[1];
      String player = s[2];

      String id = Table.getTableViewerId(tableId, player);
      
      if ((table=id2JoinedTable.get(id)) != null) {
        isPlayer = true;
        id2JoinedTable.remove(id);
        return true;
      }
        
      if ((table=id2ObservedTable.get(id)) != null) {
        isPlayer = true;
        id2ObservedTable.remove(id);
        return true;
      }
        
      Misc.err("destroy: table " + tableId + " not created");
      return true;
    }
  }

  
  public class ErrorMsg extends ServiceMsg
  {
    public String text;

    // error <text>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 1 || !s[0].equals("error"))
	return false;

      sr.nextWord();
      text = sr.rest();
      return true;
    }
  }

  public class ClientArrivedOrUpdateMsg extends ServiceMsg
  {
    public ClientData cd;

    // client + <clientid> <permission> <game-num> <rating> <disco> <timeout> <group> <clonenum>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length != 11 || !s[0].equals("clients") || !s[1].equals("+"))
	return false;

      cd = new ClientData();
      cd.id         = s[2];
      cd.permission = Integer.parseInt(s[3]);
      cd.languages  = s[4];
      cd.games3     = Integer.parseInt(s[5]); // total # games
      cd.games4     = 0;
      cd.rating     = Double.parseDouble(s[6]);
      cd.disconnect = Integer.parseInt(s[7]);
      cd.timeout    = Integer.parseInt(s[8]);
      cd.group      = Integer.parseInt(s[9]);
      cd.cloneNum   = Integer.parseInt(s[10]);
      id2client.put(cd.id, cd);
      return true;
    }
  }

  public class ClientDepartedMsg extends ServiceMsg
  {
    public String clientId;

    // client - <clientid>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length != 3 || !s[0].equals("clients") || !s[1].equals("-"))
	return false;

      clientId = s[2];

      id2client.remove(clientId);
      return true;
    }
  }

  public class TableAddedOrUpdateMsg extends ServiceMsg
  {
    public String tableId;
    public int playerNum;
    public int gameNum;
    public Vector<String> players;
    
    // tables + <tableid> <playernum> <players...> <blockNum>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 9 || !s[0].equals("tables") || !s[1].equals("+"))
	return false;

      tableId = s[2];
      id2TableName.put(s[2], "");
      playerNum = Integer.parseInt(s[3]);
      gameNum = Integer.parseInt(s[4]);
      players = new Vector<String>();
      for (int i=0; i < playerNum; i++) {
	players.add(s[5+i]);
      }
      return true;
    }
  }

  public class TableRemovedMsg extends ServiceMsg
  {
    public String tableId;

    // tables - <tableid>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length != 3 || !s[0].equals("tables") || !s[1].equals("-"))
	return false;

      tableId = s[2];
      id2TableName.remove(s[2]);
      return true;
    }
  }

  public class TableTellMsg extends ServiceMsg
  {
    public Table  table;
    public String fromId;
    public String text;

    // table <tableid> <player> tell <fromid> <text>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 5 || !s[0].equals("table") || !s[3].equals("tell"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));
      
      fromId = s[4];

      sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord();
      text = sr.rest();

      return true;
    }
  }

  public class TableErrorMsg extends ServiceMsg
  {
    public Table table;
    public String text;

    // table <tableid> <player> error <text>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("table") || !s[3].equals("error"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));
      
      sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord();
      text = sr.rest();
      return true;
    }
  }
  
  public class TablePlayMsg extends ServiceMsg
  {
    public Table table;
    public String player;
    public String move;
    
    // table <tableid> <player> play <playmsg>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("table") || !s[3].equals("play"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));

      sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord();

      player = s[4];
      move   = s[5];
      
      table.handlePlayMsg(sr);
      return true;
    }
  }

  public class TableStartMsg extends ServiceMsg
  {
    public Table table;

    // table <tableid> <player> start <startmsg>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("table") || !s[3].equals("start"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));

      sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord();
      table.handleStartMsg(sr);
      return true;
    }
  }

  public class TableEndMsg extends ServiceMsg
  {
    public Table table;
    public String gameHist;

    // table <tableid> <player> end <complete-game-sgf>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("table") || !s[3].equals("end"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));

      sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord();
      gameHist = sr.rest();
      // table.handleEndMsg(sr);
      table.handleEndMsg(gameHist);
      
      return true;
    }
  }

  public class TableStateMsg extends ServiceMsg
  {
    public Table table;

    // table <tableid> state <tableinfo...>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("table") || !s[3].equals("state"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));

      // update table state
      
      sr.nextWord(); sr.nextWord(); sr.nextWord(); sr.nextWord(); 
      table.handleStateMsg(sr);
      return true;
    }
  }

  public class TableGoMsg extends ServiceMsg
  {
    public Table table;

    // table <tableid> <player> go
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("table") || !s[3].equals("go"))
	return false;

      table = findTable(Table.getTableViewerId(s[1], s[2]));
      
      table.handleGoMsg();
      return true;
    }
  }

  private Table findTable(String tableId)
  {
    Table table = id2JoinedTable.get(tableId);
    
    if (table == null) {
      table = id2ObservedTable.get(tableId);
    }
    
    if (table == null)
      Misc.err("findTable: table " + tableId + " not created");

    return table;
  }


  public class TableStopMsg extends ServiceMsg
  {
    public Table table;

    // table <tableid> stop 
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 3 || !s[0].equals("table") || !s[2].equals("stop"))
	return false;

      table = findTable(s[1]);
      
      table.handleStopMsg();
      return true;
    }
  }

  public class TourAddedOrUpdateMsg extends ServiceMsg
  {
    public TournamentData td;
    
    // tour + <tourInfo> <clientJoined>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 14 || !s[0].equals("tour") || !s[1].equals("+"))
	return false;

      sr.nextWord(); sr.nextWord();

      td = new TournamentData();
      String e = td.fromInfoString(sr);

      if (e != null) return false;

      e = sr.nextWord();

      if (e == null) return false;
      
      td.userJoined = !e.equals("0");

      id2TourData.put(td.name, td);
      return true;
    }
  }

  public class TourRemovedMsg extends ServiceMsg
  {
    public String tourId;

    // tour - <tourId>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length != 3 || !s[0].equals("tour") || !s[1].equals("-"))
	return false;

      tourId = s[2];
      id2TourData.remove(tourId);
      return true;
    }
  }

  public class InviteMsg extends ServiceMsg
  {
    public String from;
    public String tableId;
    public String tablePassword;

    // invite <from> <table-id> <table-password>
    public boolean parse(String[] s, SReader sr)
    {
      if (s.length < 4 || !s[0].equals("invite"))
	return false;

      from = s[1];
      tableId = s[2];
      tablePassword = s[3];
      return true;
    }
  }

  /** catches all remaining message types */
  public class CatchAllMsg extends ServiceMsg
  {
    public String[] parts;
    public SReader  sreader;

    // <text>
    public boolean parse(String[] s, SReader sr)
    {
      parts = s;
      sreader = sr;
      return true;
    }
  }

  public class DisconnectMsg extends ServiceClient.ServiceMsg
  {
    public boolean parse(String[] s, SReader sr)
    {
      return false;
    }
  }

  public DisconnectMsg newDisconnectMsg() {
    return new DisconnectMsg();
  }
  
  ServiceMsg[] msgTypes = new ServiceMsg[] {
    new ClientArrivedOrUpdateMsg(),
    new ClientDepartedMsg(),
    new YellMsg(),
    new TellMsg(),
    new TextMsg(),
    new CreateMsg(),
    new DestroyMsg(),
    new TableAddedOrUpdateMsg(),
    new TableRemovedMsg(),
    new TablePlayMsg(),
    new TableStateMsg(),
    new TableTellMsg(),
    new TableStartMsg(),    
    new TableEndMsg(),
    new TableErrorMsg(),
    new TableGoMsg(),
    new TableStopMsg(), 
    new InviteMsg(),
    new FingerMsg(),
    new TimeMsg(),
    new TourAddedOrUpdateMsg(),
    new TourRemovedMsg(),    
    new ErrorMsg(),
    new CatchAllMsg()
  };

  ServiceClient getThis() { return this; }
}
