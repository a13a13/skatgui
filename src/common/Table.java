/**

   Skat Table
   
   (c) Michael Buro, licensed under GPLv3    
*/


// fixme: remove "go" message

package common;

import java.util.*;
import java.io.*;

public class Table
{
  class Observer {
    String id;
    // boolean watching;

    public Observer(String id) {
      this.id = id;
      // watching = false;
    }
  }
  
  private String tableId;
  private long seriesId;
  private boolean newSeries; // if true next game will start new series
  private boolean atLeastOneSeries; // true if series was started
  private String viewerName; // null: server
  private String type;
  private int playerNum; // 3 or 4; to set call setPlayerNum to keep scoresheet in sync
  private boolean tourny; // true: tournament table
  private int blockNum;   // in tournament mode
  private String creator; 
  private static final int GAME_PLAYER_NUM = 3;
  private static final int MAX_PLAYER_NUM = 4;
  private String passwd; // for joining game
  private TableCallbacks cb;
  private Random rng;
  private ArrayList<SimpleGame> playedGames; // for serialization
  private ScoreSheet scoreSheet;             // add games to both containers!
    
  public final static String NO_NAME = ".";
  private final String[] playerSuffixes = { "", ":2", ":3", ":4" }; // multiple players in group
  
  // name formats:
  // table format:  mic mic:2 mic:3 ... used at tables (i-th occurrence of mic)
  //                                               
  // user  format:  mic (user) mic:1 mic:2 ... (group) actual client names, used
  //                                          in table list (i-th clone connected)
   
  private TreeMap<String, String> nameAtTable2UserName = new TreeMap<String,String>();
  
  private String[] playersAtTable;       // players at table (table format) (null: no longer there (disconnect/left)
  private String[] playersInResultTable; // players in result table (table format)
  private boolean[] disconnectedAtTable; // true: disconnected (index same as above)
  // only disconnected players will be auto-joined when they return
  private char[] ipSetIdInResultTable;   // id of IP address set for player
                                         // (=='.'<=>player has unique IP address)
  private String[] playersInGame;        // in table format
  private ArrayList<Observer> observers; // user format
  
  private boolean ready[];     // player ready for next game
  private boolean gameTalk[];  // player fine with in game talking
  private boolean threeFour[]; // player requests seat number change
  private boolean practice[];  // player requests practice table (obsolete)
  private double[] remTimes;   // remaining game time (msecs)
  private long moveTimeStamp;  // when toMove move was last received
  private SimpleGame sg;
  private boolean gameInProgress; // game running
  private int played[], wins[], lastPts[], totalPts[];
  private int gameNum;

  private boolean gameStarted; // indicated whether this game has
                               // started yet and we want moves from
                               // clients

  private boolean remove; // true: finish game and kill on checktime 
  
  long lastTouched; // time (millis) when game messages were sent
  String gameFileName;
  public final static String NO_GAME_FILE = "/";
  public final static double GAME_TIME = 4*60.0;  // seconds per player for entire game

  public final static int PENALTY_PTS = 100; // disconnect or timeout before bidding is over
  public final static int DECL_WIN_PTS = 50;
  public final static int DEF3_WIN_PTS = 40;
  public final static int DEF4_WIN_PTS = 30;   
  
  // last move data
  private int lastPlayer;
  private String lastMove;
  StringBuffer dateTime;
  BufferedReader buff;

  private Locale locEn = new Locale("en"); // for format

  private String i18n(String s) {
    return "_" + s;
  }
  
  public Table(TableCallbacks cb)
  {
    this.cb = cb;
    playersInGame  = new String[MAX_PLAYER_NUM];
    playersAtTable = new String[MAX_PLAYER_NUM];
    disconnectedAtTable = new boolean[MAX_PLAYER_NUM];
    playersInResultTable = new String[MAX_PLAYER_NUM];
    ipSetIdInResultTable = new char[MAX_PLAYER_NUM];
    ready    = new boolean[MAX_PLAYER_NUM];
    gameTalk = new boolean[MAX_PLAYER_NUM];    
    threeFour= new boolean[MAX_PLAYER_NUM];
    practice = new boolean[MAX_PLAYER_NUM];
    remTimes = new double[MAX_PLAYER_NUM];    
    played   = new int[MAX_PLAYER_NUM];
    wins     = new int[MAX_PLAYER_NUM];
    lastPts  = new int[MAX_PLAYER_NUM];    
    totalPts = new int[MAX_PLAYER_NUM];
    observers = new ArrayList<Observer>();
    playedGames = new ArrayList<SimpleGame>();
    scoreSheet = null;
    dateTime = new StringBuffer();

    // !!! MIC: can't load settings file in server! has to be separated somehow
    // i18n is done in the client who knows the language
    // the server just passes on _.... strings that get translated (still to be done)
    
    //String language = "en"; // default
    
    //    try {
    //  language = getLanguage();
    //}
    //catch (Exception e) {
    //  Misc.err("could not get user's language from settings file " + settingsFile);
    //}

    //    if (!language.equals("de") && !language.equals("pl") && !language.equals("fr"))
    //  language = "en";
    //bundle = java.util.ResourceBundle.getBundle(resourceFile, new Locale(language));

    touch();    
  }

  /* I tried using the Settings class to do this, but because it is defined as an inner
     class to ClientWindow, it cannot be accessed by outside packages. - Ryan */
  //  /** @return user's preferred language from settings file */
  //String getLanguage() throws Exception {
  //  try {
  //    buff = new BufferedReader(new InputStreamReader(new FileInputStream(settingsFile)));
  //  }
  //  catch (FileNotFoundException e) {
  //    Misc.err("settings file " + settingsFile + " does not exist");
  //  }
  //
  //  for (int index = 0; index < 14; index++) {
  //    String token = nextToken(buff);
  //
  //    if (token.startsWith("language")) {
  //      return token.substring(9, token.length());
  //    }
  //  }
  //
  //  return "";
  //}

  /** init regular table
   *  @param viewerName: name of viewer on table (null if server, "." if observer)
   *  @param gameReader: != null: read sgf game(prefix) from file
   *  @return null if OK, error message otherwise
   */
  public String init(Random rng, 
		     String tableId, String pw, String type, String viewerName,
		     String gameFileName)
  {
    this.viewerName = viewerName;
    this.rng     = rng;
    this.tableId = tableId;
    this.type    = type;
    this.passwd  = pw;
    this.gameFileName = gameFileName;
    this.newSeries = true;
    gameNum = 0;
    gameStopped();
    touch();

    tourny = false;
    
    if (type == null)
      return i18n("illegal_table_type") + " " + type;      
    else if (type.equals("3")) {
      setPlayerNum(3);
    } else if (type.equals("4")) {
      setPlayerNum(4);
    } else
      return i18n("illegal_table_type") + " " + type;

    //scoreSheet = new ScoreSheet(playerNum);
    return null; // OK
  }

  // to keep score sheet in sync with playerNum
  void setPlayerNum(int n)
  {
    playerNum = n;
    scoreSheet = new ScoreSheet(playerNum);
  }
  
  /** init tournament table
   *  @param viewerName: name of viewer on table (null if server, "." if observer)
   *  @param gameReader: != null: read sgf game(prefix) from file
   *  @param seats:      list of seated players (-> seat number, tourny mode)
   *  @return null if OK, error message otherwise
   */
  public String init(Random rng, 
		     String tableId, String pw, int playerNum, int blockNum, String[] seats,
                     String seed, String creator)
  {
    this.viewerName = null; // server
    this.rng     = rng;
    this.tableId = tableId;
    this.passwd = pw;
    this.creator = creator;
    this.type    = "" + playerNum;
    setPlayerNum(playerNum);
    this.blockNum = blockNum;
    this.passwd  = pw;
    this.gameFileName = "/";
    this.newSeries = true;
    gameNum = 0;
    for (int i=0; i < playerNum; i++) {
      String s;
      // find next name at table for user seats[i]
      int k = 0;
      for (int j=0; j < i; j++) {
        if (seats[i].equals(seats[j])) k++;
      }
      
      this.playersInResultTable[i] = seats[i] + playerSuffixes[k];
      disconnectedAtTable[i] = true; // auto-join
    }
    gameStopped();
    touch();

    tourny = true;
    seriesId = cb.nextSeriesId(); // needs to be set before the first game starts (save...)

    // scoreSheet = new ScoreSheet(playerNum);    
    return null; // OK
  }

  /** @return number of players at table */
  public int playersAtTableNum() {
    int n = 0;
    if (playersAtTable[0] != null) n++;
    if (playersAtTable[1] != null) n++;
    if (playersAtTable[2] != null) n++;
    if (playersAtTable[3] != null) n++;    
    return n;
  }

  /** @return date and time */
  public String getDateTime() { return dateTime.toString(); }

  /** @return SimpleGame object for a game played in the current series */
  public SimpleGame getPlayedGame(int index) {
    return playedGames.get(index);
  }

  /** @return name of player in result table */
  public String getPlayerInResultTable(int index) {
    if (index < 0 || index >= getPlayerNum())
      return "?";
    else
      return playersInResultTable[index];
  }

  /** @return table id */
  public String getId() { return tableId; }

  /** @return series id */
  public long getSeriesId() { return seriesId; }

  /** @return viewer player name (".": observer) */
  public String getViewerName() { return viewerName; }

  /** @return id composed of table id and viewer name */
  public String getTableViewerId() {
    return getTableViewerId(tableId, viewerName);
  }
    
  /** @return id composed of table id and viewer name */
  public static String getTableViewerId(String tab, String viewer) {
    return tab+"/"+viewer;
  }

  /** @return -1: unbounded, >0: number of games at tournament table */  
  public int getMaxGameNum() {
    if (!tourny) return -1;
    
    return blockNum * playerNum;
  }

  public boolean getTourny()
  {
    return tourny;
  }

  // client
  public void setTourny(int blockNum)
  {
    this.blockNum = blockNum;
    tourny = true;
  }

  /** @return number of players */
  public int getPlayerNum() { return playerNum; }

  public String getCreator() { return creator; }
  
  /** @return number of games */
  public int getGameNum() { return gameNum; }

  /** @return max number of players */
  static public int getMaxPlayerNum() { return MAX_PLAYER_NUM; }

  /** @return true iff player is ready */
  public boolean getReady(int player) { return ready[player]; }

  /** @return remaining time for player */
  public double getRemTime(int player) { return remTimes[player]; }

  /** @return true iff player allows game talk */
  public boolean getGameTalk(int player) { return gameTalk[player]; }

  /** @return true iff player wants to change seat number */
  public boolean get34(int player) { return threeFour[player]; }

  /** @return true iff player requests practice table */
  public boolean getPractice(int player) { return practice[player]; }

  /** @return number of wins */
  public int getWins(int player) { return wins[player]; }

  /** @return points in last game */
  public int getLastPts(int player) { return lastPts[player]; }

  /** @return total pts so far */
  public int getTotalPts(int player) { return totalPts[player]; }

  /** @return number of played games */
  public int getPlayed(int player) { return played[player]; }

  /** @return true if game is in progress */
  public boolean getInProgress() { return gameInProgress; }

  /** set game in progress variable */
  public void setInProgress(boolean f) { gameInProgress = f; }

  /** inform table to be removed */
  public void toBeRemoved() { remove = true; }

  /** can the table be removed right now? */
  public boolean canBeRemoved() { return remove && !gameInProgress; }

  public boolean inPracticeMode() {
    // no clocks (open hands later)

    for (int i=0; i < playerNum; i++) {
      if (!practice[i])
        return false;
    }
    return true;
  }
  
  /** is talking while playing allowed? */
  public boolean gameTalkAllowed()
  {
    for (int i=0; i < playerNum; i++) {
      if (!gameTalk[i])
        return false;
    }
    return true;
  }
  
  /** @return game */
  public SimpleGame getGame() { return sg; }

  /** set game */
  //  public void setGame(SimpleGame sg) { this.sg = sg; }

  /** set id */
  public void setId(String id) { tableId = id; }

  //  /** @return sender */
  //public MsgHandler getCbSender() { return sender; }

  /** @return RNG */
  public Random getRng() { return rng; }

  /** @return password */
  public String getPassword() { return passwd; }

  /** @return table type */
  public String getType() { return type; }

  /** @return true if table is private */
  public boolean isUserCreated() { return Misc.validId(tableId) == null; }

  /** @return true if all games scheduled for a tournament table have been played */
  public boolean isFinished() {
    return tourny && gameNum >= getMaxGameNum();
  }
  
  /** @return true if the game has started and we are expecting moves from clients */
  public boolean isGameStarted() { return gameStarted; }

  /** send error message to client at a table */
  public void sendError(String client, String player, String msg)
  {
    cb.send(client, "table " + tableId + " " + player + " " + "error " + msg);
  }

  /** send error message to client at a table */
  public void sendError(String client, String player, String cmd, String usage, String msg)
  {
    sendError(client, player, cmd + " : " + msg + ", " + i18n("Usage_colon") + " " + cmd + " " + usage);
  }

  /** send error message to client at a table */
  public void sendError(String client, String player, String cmd, String msg)
  {
    sendError(client, player, cmd + " : " + msg);
  }

  /** send message to client at a table */
  public void send(String client, String player, String msg)
  {
    cb.send(client, "table " + tableId + " " + player + " " + msg);
  }

  /** user joins the table */
  public boolean join(String user)
  {
    int playerIndex = userArrived(user);
    if (playerIndex < 0)
      return false;

    disconnectedAtTable[playerIndex] = false;
    
    Misc.msg(user + " joins table " + tableId);

    cb.sendToAll(updateMsg());

    cb.send(user, "create " + tableId + " " + playersAtTable[playerIndex] + " " +
            type + " " + (tourny ? blockNum : -1) + " " + scoreSheet.toString());

    sendToAll("state " + stateToString());

    if (gameInProgress) {

      send(user, playersAtTable[playerIndex], 
           String.format(locEn, "start %d %s %.1f %s %.1f %s %.1f ! %s",
                         gameNum,
                         playersInGame[0],
                         remainingTime(0),
                         playersInGame[1],
                         remainingTime(1),
                         playersInGame[2],
                         remainingTime(2),
                         sg.toSgf(true, playerInGameIndex(playersAtTable[playerIndex]))
                         ));
    }
    
    return true;
  }

  /** user observes table */
  public boolean observe(String user)
  {
    if (observerIndex(user) >= 0)
      return false;

    String group = groupName(user);
    for (int i=0; i < playerNum; i++) {
      String p = playersAtTable[i];
      if (p != null) {
        if (group.equals(groupName(p)))
          return false; // player can't observe
      }
    }
    
    Misc.msg(user + " observes table " + tableId);
    
    observers.add(new Observer(user));

    cb.send(user, "create " + tableId + " . " + type + " " + (tourny ? blockNum : -1) + " " +
            scoreSheet.toString());
  
    sendToAll("state " + stateToString());

    if (gameInProgress) {

      send(user, NO_NAME,
           String.format(locEn, "start %d %s %.1f %s %.1f %s %.1f p %s",
                         gameNum,
                         playersInGame[0],
                         remainingTime(0),
                         playersInGame[1],
                         remainingTime(1),
                         playersInGame[2],
                         remainingTime(2),
                         sg.toSgf(true, SimpleState.PUBLIC_VIEW)
                         ));
    }
    return true;
  }

  public String updateMsg()
  {
    String s = "tables + " + getId() + " " +
      getPlayerNum() + " " + getGameNum();
    
    for (int i=0; i < getPlayerNum(); i++) {
      String u = getUserAtTable(i);
      if (u == null) {
        if (tourny) {
          s += " [" + groupName(playersInResultTable[i]) + "]";
        } else
          s += " " + NO_NAME;
      } else {
        s += " " + u;
      }
    }

    s +=  " " + getMaxGameNum(); // for tournament tables
    return s;
  }

  /** @return string representation of table */
  public String stateToString()
  {
    StringBuffer sb = new StringBuffer();

    // player number

    sb.append(playerNum + " ");
    
    // players at table

    for (int i=0; i < MAX_PLAYER_NUM; ++i) {
      String s = playersAtTable[i];
      if (s == null) {
        sb.append(NO_NAME+" ");
      } else
        sb.append(s+" ");
    }

    // compute ipAddress set ids and mark player with '#'+setId
    char[] ipSetIds = new char[MAX_PLAYER_NUM];
    String[] ips = new String[MAX_PLAYER_NUM];
    
    for (int i=0; i < MAX_PLAYER_NUM; ++i) {
      ips[i] = "";
      ipSetIds[i] = '.';
      String t = playersInResultTable[i];
      ClientData cd;
      if (t != null && (cd=cb.getClientData(t, false)) != null) {
        ips[i] = cd.lastIPaddress;
      }
    }

    for (int i=0; i < MAX_PLAYER_NUM; ++i) {
      int n = 0, first = -1;
      for (int j=0; j < MAX_PLAYER_NUM; ++j) {
        if (ips[i].equals(ips[j])) {
          if (first < 0) first = j;
          n++;
        }
      }
      if (n > 1) ipSetIds[i] = (char)('#'+first);
    }
    
    for (int i=0; i < MAX_PLAYER_NUM; ++i) {
      String t = playersInResultTable[i];      
      if (t == null) {
        sb.append(NO_NAME + " " + NO_NAME + " ");
      } else {
        sb.append(t+" "+ipSetIds[i]+" ");
      }
      
      sb.append(played[i] + " ");
      sb.append(wins[i] + " ");
      sb.append(lastPts[i] + " ");      
      sb.append(totalPts[i] + " ");

      sb.append(threeFour[i] ? "1 " : "0 ");      
      sb.append(practice[i] ? "1 " : "0 ");
      sb.append(gameTalk[i] ? "1 " : "0 ");      
      sb.append(ready[i] ? "1 " : "0 ");
    }

    // match status
    sb.append(gameInProgress + " ");
    sb.append(gameNum);
    return sb.toString();
  }

  /** prepare to be deleted */
  public void cleanUp()
  {
    // make everybody leave

    for (Observer o : observers) {
      if (o.id == null) continue;
      cb.send(o.id, "destroy " + tableId + " " + NO_NAME);
    }

    for (int i=0; i < MAX_PLAYER_NUM; ++i) {
      String p = playersAtTable[i];
      if (p == null) continue;
      String u = nameAtTable2UserName.get(p);
      cb.send(u, "destroy " + tableId + " " + p);
    }
  }

  /** check for timeouts */
  public void checkTimeService()
  {
    if (!gameInProgress || sg.isFinished() || inPracticeMode()) return;

    int pi = sg.getToMove();
    long now = System.currentTimeMillis();

    // Misc.msg("TIME CHECK: " + remainingTime(0) + " " + remainingTime(1) + " " + remainingTime(2));
    
    if (remainingTime(pi) < 0) {

      // player to move timeout

      Misc.msg("TIMEOUT in checktime. finished = " + sg.isFinished());
      
      if (!sg.isFinished()) { // could be finished already (RE)

        Misc.msg("TIMEOUT in checktime: makemove!");
        
        String[] views = new String[SimpleState.VIEW_NUM];
        String res = sg.makeMove(SimpleState.WORLD_VIEW, "TI."+pi, views);
        if (res != null) {
          Misc.msg("!!!!!!! Timeout in checktime error: " + res);
          return; // exit gracefully to prevent server from crashing, but this is SERIOUS
          // collect more data to figure out why this is reached despite game is finished
        }
        
        sendMoveViews("w", views); // public = player views
      }
      
      handleGameEnd();
    }
  }
  

  /**
   * execute table cmd in service
   * @return true iff handled
   */
  public boolean execInService(String sender, String player, SReader reader)
  {
    String cmd = reader.nextWord();

    // sender and player must match
    String group = groupName(sender);
    if (!player.equals(NO_NAME) &&
        !player.equals(group+playerSuffixes[0]) &&
	!player.equals(group+playerSuffixes[1]) &&
	!player.equals(group+playerSuffixes[2]) &&
	!player.equals(group+playerSuffixes[3])) {
      sendError(sender, player, i18n("player_not_sender_colon") + " " + player + " " + sender);
      return true;
    }

    if (!player.equals(NO_NAME) && playerAtTableIndex(player) < 0) {
      sendError(sender, player, i18n("player_not_playing_colon") + player);
      return true;
    }

    touch();
    
    for (ExecData e : svcCmds) {
      if (!e.cmd1.equals(cmd) && !e.cmd2.equals(cmd)) continue;
      e.exec.exec(sender, player, reader);
      return true;
    }

    Misc.msg("TABLE CMD RECEIVED BUT NOT HANDLED: " + cmd);
    return false;
  }

  /** @return minimum number of players */
  public int missingPlayerNum()
  {
    int m = 0;
    for (int i=0; i < playerNum; i++) {
      if (playersAtTable[i] == null)
        m++;
    }

    return m;
  }


  /** @return true iff client plays at table */
  public boolean playsAtTable(String client)
  {
    String g = groupName(client);
    for (int i = 0; i < playerNum; ++i) {
      String p = playersAtTable[i];
      if (p == null) continue;
      if (groupName(p).equals(g))
        return true;
    }
    
    return false;
  }

  public void joinIfDisconnected(String client)
  {
    String g = groupName(client);
    int join = 0;
    for (int i = 0; i < playerNum; ++i) {

      if (playersAtTable[i] == null && (tourny || disconnectedAtTable[i])) {
        
        if (tourny) {

          if (g.equals(groupName(playersInResultTable[i])))
            join++;
          
        } else {
          
          if (playersInResultTable[i] != null && // empty seat
              groupName(playersInResultTable[i]).equals(g))
            join++;
        }
      }
    }

    for (int i=0; i < join; i++) {

      String clientToInvite = cb.clientToInvite(client, getThis());

      if (clientToInvite == null) {
        Misc.err("invite didn't work: " + client);
        return;
      }

      Misc.msg("INVITE: " + clientToInvite);
      join(clientToInvite);
    }
  }

  /** @return compute player index at table */
  public int playerAtTableIndex(String player)
  {
    for (int i = 0; i < playerNum; ++i) {
      if (player.equals(playersAtTable[i]))
        return i;
    }
    
    return -1;
  }

  /** @return compute player index in result table */
  public int playerInResultTableIndex(String player)
  {
    for (int i = 0; i < playerNum; ++i) {
      if (player.equals(playersInResultTable[i]))
        return i;
    }
    
    return -1;
  }

  /** @return number of players at table */
  public int numOfPlayersAtTable()
  {
    int n = 0;
    
    for (int i=0; i < GAME_PLAYER_NUM; i++) {
      if (playersAtTable[i] != null)
        n++;
    }
    return n;
  }

  /** @return index of player in game, -1 if not present */
  public int playerInGameIndex(String player)
  {
    for (int i=0; i < GAME_PLAYER_NUM; i++) {
      if (player.equals(playersInGame[i]))
        return i;
    }
    return -1;
  }

  /** @return player name */
  public String playerInGame(int index)
  {
    assert(index >= 0 && index < GAME_PLAYER_NUM);
    return playersInGame[index];
  }
  
  /** @return compute player index at table */
  public int observerIndex(String user)
  {
    for (int i = 0; i < observers.size(); ++i) {
      if (user.equals(observers.get(i).id))
        return i;
    }
    
    return -1;
  }

  /**
   * send individual move view to players and observers
   */
  private void sendMoveViews(String player, String[] views)
  {
    StringBuilder sb = new StringBuilder();
    Formatter formatter = new Formatter(sb, Locale.US);
    formatter.format(locEn, "%.1f %.1f %.1f",
                     remTimes[0]*0.001, remTimes[1]*0.001, remTimes[2]*0.001);
    String times = sb.toString();

    // players

    for (String nameOnTable : playersAtTable) {

      if (nameOnTable == null) continue;
      
      String tableUser = nameAtTable2UserName.get(nameOnTable);

      int mi = playerInGameIndex(nameOnTable);
      if (mi < 0)
	send(tableUser, nameOnTable, "play " + player + " " + views[SimpleState.WORLD_VIEW] + " " + times); //dealer
      else
	send(tableUser, nameOnTable, "play " + player + " " + views[mi] + " " + times);
    }

    // observers
    
    for (Observer o : observers) {
      if (o.id == null) continue;
      send(o.id, ".", "play " + player + " " + views[SimpleState.PUBLIC_VIEW] + " " + times);
    }
  }

  /** send individual message to players: player msgs 0,1,2 + observers */
  private void sendToAll(String[] playerMsgs)
  {
    sendToAllExcept("", playerMsgs);
  }

  /** send message to all */
  private void sendToAll(String msg)
  {
    sendToAll(new String[] { msg, msg, msg, msg, msg });
  }

  private void sendToAllExcept(String user, String msg)
  {
    sendToAllExcept(user, new String[] { msg, msg, msg, msg, msg });
  }
  
  /** send individual message to players: player msgs 0,1,2 + observers */
  private void sendToAllExcept(String user, String[] playerMsgs)
  {
    //  Misc.msg("messages: " + playerMsgs[0] + " |\n" + playerMsgs[1] + "|\n" + playerMsgs[2] + "|\np:" + playerMsgs[3]);

    // players

    for (String nameOnTable : playersAtTable) {

      if (nameOnTable == null) continue;
      
      String tableUser = nameAtTable2UserName.get(nameOnTable);
      
      if (tableUser.equals(user)) continue;
      int mi = playerInGameIndex(nameOnTable);
      if (mi < 0)
	send(tableUser, nameOnTable, playerMsgs[SimpleState.WORLD_VIEW]); // dealer
      else
	send(tableUser, nameOnTable, playerMsgs[mi]);
    }

    // observers
    
    for (Observer o : observers) {
      if (o.id == null || o.id.equals(user)) continue;
      send(o.id, NO_NAME, playerMsgs[SimpleState.PUBLIC_VIEW]);
    }
  }

  //   /** send message to user */
  //   private void sendToUser(String user, String msg)
  //   {
  //     // Misc.msg("message to user " + user + ":" + msg);
  //     send(user, msg);
  //   }

  /** game stopped, waiting for ready */
  private void gameStopped()
  {
    gameInProgress = false;
    ready[0] = ready[1] = ready[2] = ready[3] = false;
  }    

  /** @return remaining time for player in seconds */
  public double remainingTime(int player)
  {
    if (player < 0 || player > 2) Misc.err("only players are timed");
    if (!inPracticeMode() && gameInProgress && player == sg.getToMove()) 
      return (remTimes[player] - (System.currentTimeMillis() - moveTimeStamp))*0.001;
    else
      return remTimes[player]*0.001;
  }

  
  /**
   * Removes disconnected client from table
   * 
   */
  public void disconnected(String client)
  {
    int oi = observerIndex(client);
    if (oi >= 0) {
      // remove observer
      observers.remove(oi);
      return;
    }

    for (int i=0; i < playerNum; i++) {
      
      String player = playersAtTable[i];
      if (player == null) continue;
      
      if (nameAtTable2UserName.get(player).equals(client)) {

        int pig = playerInGameIndex(player);

        if (false) {

          // disconnects used to end games - no more
          
          if (gameInProgress && pig >= 0) {
            
            // player left, kill game
            String[] views = new String[SimpleState.VIEW_NUM];
            String res = sg.makeMove(SimpleState.WORLD_VIEW, "LE."+pig, views);
            if (res != null)
              Misc.err("Left move error: " + res);
            
            sendMoveViews("w", views);
            handleGameEnd();
            gameStopped();
          }
        }

        playersAtTable[i] = null;
        disconnectedAtTable[i] = true;
        nameAtTable2UserName.remove(player);
      }
    }

    cb.sendToAll(updateMsg());
    sendToAllExcept(client, "state " + stateToString());
  }

  /** command interface */

  abstract class ExecCmd
  {
    /** return true iff exec OK */
    abstract void exec(String sender, String player, SReader reader);

    /** send error message to client */
    public void error(String sender, String player, String cmd, String usage, String msg)
    {
      sendError(sender, player, cmd + " : " + msg + ", " + i18n("Usage_colon") + " " + cmd + " " + usage);
    }
  }

  /** element of command list (see below) */

  class ExecData
  {
    public ExecData(String cmd1, String cmd2, ExecCmd exec)
    {
      this.cmd1 = cmd1;
      this.cmd2 = cmd2; // abbrevation
      this.exec = exec;
    }

    public String cmd1, cmd2;

    public ExecCmd exec;
  }

  // -------------- command execution in service --------------


  /*
   * cmd: leave
   * 
   * leave table
   */

  class SvcLeave extends ExecCmd
  {
    public void exec(String client, String player, SReader reader)
    {
      int oi = observerIndex(client);
      if (oi >= 0) {
        // remove observer
        observers.remove(oi);
        cb.send(client, "destroy " + tableId + " .");
        return;
      }

      int pi = playerAtTableIndex(player);

      if (pi < 0) {
        sendError(client, player, "leave : " + i18n("not_sitting_at_table"));
	return;
      }

      if (gameInProgress && playerInGameIndex(player) >= 0) {
        sendError(client, player, "leave : " + i18n("not_allowed_when_playing"));
	return;
      }

      if (tourny && gameNum < getMaxGameNum()) {
        sendError(client, player, "leave : " + i18n("list_not_finished"));
	return;
      }
      
      nameAtTable2UserName.remove(player);      
      playersAtTable[pi] = null;
      ready[pi] = false;

      cb.sendToAll(updateMsg());
      
      sendToAll("state " + getThis().stateToString());

      cb.send(client, "destroy " + tableId + " " + player);
    }
  }

  /*
   * cmd: tell <msg>
   * 
   * send message to table occupants
   */
  class SvcTell extends ExecCmd
  {
    public void exec(String sender, String player, SReader reader)
    {
      if (playerAtTableIndex(player) < 0) {
        sendError(sender, player, "tell : " + i18n("not_joined_so_cannot_talk"));
        return;
      }

      if (!cb.communicationAllowed(sender)) {
        sendError(sender, player, "tell : " + i18n("not_allowed_when_playing"));
	return;
      }
      
      sendToAll("tell " + player + " " + reader.rest());
    }
  }

  /*
   * cmd: gametalk
   * 
   * toggle your gametalk status
   */
  class SvcGameTalk extends ExecCmd
  {
    public void exec(String sender, String player, SReader reader)
    {
      int i = playerAtTableIndex(player);

      if (i < 0) {
        sendError(sender, player, "gametalk : " + i18n("not_a_player"));
        return;
      }

      gameTalk[i] = !gameTalk[i];

      sendToAll("state " + getThis().stateToString()); // table state update
    }
  }

  /*
   * cmd: 34
   * 
   * toggle player number 
   */
  class Svc34 extends ExecCmd
  {
    public void exec(String sender, String player, SReader reader)
    {
      int ind = playerAtTableIndex(player);

      if (ind < 0) {
        sendError(sender, player, "34 : " + i18n("not_a_player"));
        return;
      }

      if (tourny) {
        sendError(sender, player, "34 : " + i18n("not_allowed_at_ttable"));
        return;
      }
      
      if (gameInProgress) {
        sendError(sender, player, "34 : " + i18n("game_in_progress"));
        return;
      }

      if (playerNum == 4) {
        int i;
        for (i=0; i < playerNum; i++) {
          if (playersAtTable[i] == null)
            break;
        }
        if (i == playerNum) {
          sendError(sender, player, "34 : " + i18n("34_table_full"));
          return;
        }
      }
      
      threeFour[ind] = !threeFour[ind];

      boolean changed = false;
      
      for (;;) {

        int i;
        for (i=0; i < playerNum; i++) {
          if (playersAtTable[i] != null && !threeFour[i])
            break; // not everybody agrees
        }

        if (i < playerNum)
          break;
        
        if (playerNum == 3) {

          changed = true;
          setPlayerNum(4); // changed to 4
          for (int j=0; j < 4; j++) {
            threeFour[j] = false;
          }

          type = "4";

          // clear 4th slot
          playersAtTable[3] = null;
          ready[3] = false;
          break;
        }
        
        // need at least one empty seat to reduce player number
        for (i=0; i < playerNum; i++) {
          if (playersAtTable[i] == null) {

            changed = true;
            setPlayerNum(3); // changed to 3
            for (int j=0; j < 4; j++) {
              threeFour[j] = false;
            }

            type = "3";

            // move 4th player if necessary
            if (playersAtTable[3] != null) {
              playersAtTable[i] = playersAtTable[3];
              playersAtTable[3] = null;
              ready[3] = false;
            }
            break;
          }
        }
        break;
      }

      sendToAll("state " + getThis().stateToString()); // table state update

      if (changed) {
        newSeries = true;
        cb.sendToAll(updateMsg());
      }
    }
  }

  /*
   * cmd: practice
   * 
   * toggle practice mode
   */
  class SvcPractice extends ExecCmd
  {
    public void exec(String sender, String player, SReader reader)
    {
      int ind = playerAtTableIndex(player);

      if (ind < 0) {
        sendError(sender, player, "practice : " + i18n("not_a_player"));
        return;
      }

      if (gameInProgress) {
        sendError(sender, player, "practice : " + i18n("game_in_progress"));
        return;
      }

      practice[ind] = !practice[ind];

      sendToAll("state " + getThis().stateToString()); // table state update      
    }
  }

  // swap players i and j
  void swap(int i, int j)
  {
    { String t = playersAtTable[i]; playersAtTable[i] = playersAtTable[j]; playersAtTable[j] = t; }    
    { boolean t = ready[i]; ready[i] = ready[j]; ready[j] = t; }
    { boolean t = gameTalk[i]; gameTalk[i] = gameTalk[j]; gameTalk[j] = t; }
    { boolean t = threeFour[i]; threeFour[i] = threeFour[j]; threeFour[j] = t; }
    { boolean t = practice[i]; practice[i] = practice[j]; practice[j] = t; }

    { double t = remTimes[i]; remTimes[i] = remTimes[j]; remTimes[j] = t; }
    { int t = played[i]; played[i] = played[j]; played[j] = t; }
    { int t = wins[i]; wins[i] = wins[j]; wins[j] = t; }
    { int t = lastPts[i]; lastPts[i] = lastPts[j]; lastPts[j] = t; }
    { int t = totalPts[i]; totalPts[i] = totalPts[j]; totalPts[j] = t; }            
  }
  
  /*
   * cmd: ready
   * 
   * toggle your ready status
   */
  class SvcReady extends ExecCmd
  {
    void error(String sender, String player, String msg)
    {
      sendError(sender, player, "ready : " + msg);
    }

    public void exec(String sender, String player, SReader reader)
    {
      int i = playerAtTableIndex(player);

      if (i < 0) {
        sendError(sender, player, i18n("not_a_player"));
        return;
      }

      if (gameInProgress) {
        sendError(sender, player, i18n("game_in_progress"));
        return;
      }

      if (cb.isShuttingDown()) {
        sendError(sender, player, i18n("server_shutting_down"));
        return;
      }

      if (remove) {
        sendError(sender, player, i18n("table_to_be_removed"));
        return;
      }

      if (tourny && gameNum >= getMaxGameNum()) {
        sendError(sender, player, i18n("list_is_finished"));
        return;
      }
      
      ready[i] = !ready[i];

      for (i=0; i < playerNum; i++) {
        if (playersAtTable[i] == null || !ready[i]) break;
      }

      if (i < playerNum) {
        sendToAll("state " + getThis().stateToString()); // table state update
        return;
      }

      // everybody ready: new game

      if (!tourny && newSeries) {
        
        newSeries = false;
        atLeastOneSeries = true;

        // manual shuffle

        for (int j=3; j >= 0; j--) {
          // swap i with random player
          swap(j, rng.nextInt(j+1));
        }
        
        //Collections.shuffle(Arrays.asList(playersAtTable), rng);
        
        if (playerNum == 3) {
          // move null to end
          for (int j=0; j < 3; j++) {
            if (playersAtTable[j] == null) {
              swap(3, j);
              ready[3] = false;
              break;
            }
          }
        }
        
        nextSeries();
      }
      
      // reset ready[] so that arriving player can't start new game
      for (int j=0; j < 4; j++) {
        ready[j] = false;
      }

      gameNum++;

      // compute playersInGame array from game number

      int dealer = (gameNum-1) % playerNum;
      playersInGame[0] = playersAtTable[(dealer+1) % playerNum]; // FH
      playersInGame[1] = playersAtTable[(dealer+2) % playerNum]; // MH
      playersInGame[2] = playersAtTable[(dealer+3) % playerNum]; // RH      

      if (playerNum > 3) {
        playersInGame[3] = playersAtTable[dealer]; // dealer
      }
      
      sendToAll("state " + getThis().stateToString()); // table state update
      
      // create game start msg

      sg = new SimpleGame(SimpleState.WORLD_VIEW);
      sg.setPlayerName(0, playersInGame[0]);
      sg.setPlayerName(1, playersInGame[1]);
      sg.setPlayerName(2, playersInGame[2]);
      sg.setId(cb.nextGameId()+"");
      sg.setSeriesId(seriesId+"");        
        
      String[] msgs = new String[SimpleState.VIEW_NUM];

      String msg = "start " + gameNum + " " +
        playersInGame[0] + " " + GAME_TIME + " " +
        playersInGame[1] + " " + GAME_TIME + " " +
        playersInGame[2] + " " + GAME_TIME;

      msgs[0] = msgs[1] = msgs[2] = msg;
      msgs[SimpleState.PUBLIC_VIEW] = msg + " p " + sg.toSgf(true, SimpleState.PUBLIC_VIEW); 
      msgs[SimpleState.WORLD_VIEW] =  msg + " w " + sg.toSgf(true, SimpleState.WORLD_VIEW);
        
      remTimes[0] = remTimes[1] = remTimes[2] = GAME_TIME * 1000; // init. clocks

      sendToAll(msgs);

      moveTimeStamp = System.currentTimeMillis();
	
      String r = null;

      for (;;) { // for using break
          
        if (gameFileName != null && !gameFileName.equals(NO_GAME_FILE)) {

          // read game (prefix) from file
            
          BufferedReader in = null;
            
          try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(gameFileName)));
          }
          catch (FileNotFoundException e) {
            r = "game file " + gameFileName + " does not exist";
            break;
          }
            
          SimpleGame readGame = new SimpleGame(SimpleState.WORLD_VIEW);
          r = readGame.fromSgf(in);
          if (r != "") {
            if (r == null) r = "no game found";
            break;
          }
            
          // replay game and send all moves to players
            
          ArrayList<Move> moves = readGame.getMoveHist();
            
          for (Move m : moves) {
              
            //Misc.msg("REPLAY MOVE : " + m.source + " " + m.action);
              
            String res = sg.makeMove(m.source, m.action, msgs);
            if (res != null) {
              Misc.err("REPLAY MAKEMOVE ERROR: " + m.source + " " + m.action + " : " + res);
            }
              
            //for (int j=0; j < GAME_PLAYER_NUM; j++)
            //  Misc.msg("VIEW " + j + " " + msgs[j]);
              
            String playerName = null;
            if (m.source == SimpleState.WORLD_VIEW)
              playerName = "w";
            else
              playerName = m.source + "";
              
            sendMoveViews(playerName, msgs);
          }
        }
        break;
      }

      if (r != null)
        Misc.msg("read game from file " + gameFileName + " failed: " + r);
        
      if (!handleGameEnd()) { // game could be over by now already

        sendToAll("go");

        gameInProgress = true;
        
        // handle initial world move sequence
          
        while (handleWorldMove(msgs)) {
          sendMoveViews("w", msgs);
          handleGameEnd();
        }
      }
    }
  }

  /** @return true if world is to move, in that case also compute player views
   * of that move to be sent to them
   */
  private boolean handleWorldMove(String[] views)
  {
    if (!gameInProgress || sg.isFinished() || sg.getToMove() != SimpleState.WORLD_VIEW)
      return false;

    String move = sg.generateWorldMove(rng);

    if (move == null) 
      Misc.err("no world move available?");

    // Misc.msg("WORLD MOVE: " + move);
    
    String r = makeMove(null, move, views);
    
    if (r != null)
      Misc.err(r); // illegal world move???

    return true;
  }


  /** make move; player == null: world moves
   *  @return null if move is OK, errmsg otherwise, also return player views 
   */
  private String makeMove(String player, String move, String[] views)
  {
    if (!gameInProgress)
      return i18n("game_not_started");

    if (sg.isFinished())
      return i18n("game_finished");

    int pi;
    
    if (player == null) {

      pi = SimpleState.WORLD_VIEW;

    } else {
    
      pi = playerInGameIndex(player);
      if (pi < 0)
        return i18n("player_not_in_game_colon") + " " + player;
    }
    
    return sg.makeMove(pi, move, views);
  }

  
  private boolean handleGameEnd()
  {
    if (!sg.isFinished())
      return false;

    // create game end msg

    // game data: names, place, time, ratings    

    sg.setPlayerName(0, playersInGame[0]);
    sg.setPlayerName(1, playersInGame[1]);
    sg.setPlayerName(2, playersInGame[2]);
    cb.fillInGameData(sg);

    cb.sendToAll(updateMsg());
    
    sendToAll("end " + sg.toSgf(true, SimpleState.WORLD_VIEW));
    
    // update table variables

    GameResult gr = new GameResult();
    sg.getCurrentState().gameResult(gr);

    if (gr.unknown)
      Misc.err("game result unknown, although game finished " +
               sg.toSgf(false, SimpleState.WORLD_VIEW));

    if (gr.declarer >= 0) {

      // adjust series stats.
      
      int defWonPts = playerNum == 3 ? DEF3_WIN_PTS : DEF4_WIN_PTS;
      int declIndex = playerInResultTableIndex(playersInGame[gr.declarer]);
      played[declIndex]++;
      int declValue = gr.declValue;
      if (declValue > 0) {

        // declarer won
        wins[declIndex]++;
        for (int i=0; i < MAX_PLAYER_NUM; i++) {
          lastPts[i] = 0;
        }
        lastPts[declIndex] = declValue + DECL_WIN_PTS;        
        totalPts[declIndex] += lastPts[declIndex];        

      } else {

        // declarer lost
        lastPts[declIndex] = declValue - DECL_WIN_PTS;
        totalPts[declIndex] += lastPts[declIndex];
        for (int i=0; i < MAX_PLAYER_NUM; i++) {
          if (i != declIndex && playersInResultTable[i] != null) {
            totalPts[i] += defWonPts;
            lastPts[i] = defWonPts;
          }
        }
      }
    }

    // penalties?

    for (int i=0; i < 3; i++) {
      if (gr.getPenalty(i) > 0) {
        int pi = playerInResultTableIndex(playersInGame[i]);
        if (pi < 0) Misc.err("penalties: pi < 0");
        lastPts[pi] = -PENALTY_PTS;
        totalPts[pi] -= PENALTY_PTS;
      }
    }

    // update player stats

    ClientData[] cds = new ClientData[3];
    ClientData cdDealer = null;
    boolean[] decl = new boolean[3];
      
    // penalty timeout left games3/4
    
    for (int i=0; i < 3; i++) {

      decl[i] = (i == gr.declarer);
      //fails when player is diconnected:
      //String group = groupName(nameAtTable2UserName.get(sg.getPlayerName(i)));
      String group = groupName(sg.getPlayerName(i));

      // Misc.msg("RESULT: " + group); // !!!
      
      cds[i] = cb.getClientData(group, false);
      if (cds[i] == null)
        Misc.err("no clientdata found for player " + group);

      // adjust penalties

      if (gr.getPenalty(i) > 0) cds[i].penalty++;
      if (gr.timeout == i)      cds[i].timeout++;
      if (gr.left == i)         cds[i].disconnect++;

      if (playerNum == 3) {
        cds[i].games3++;
      } else {
        cds[i].games4++;
      }
    }

    // sitOut for dealer

    if (playerNum == 4) {
      cdDealer = cb.getClientData(playersInGame[3], false);
      if (cdDealer == null)
        Misc.err("no client data found for dealer " + playersInGame[3]);

      cdDealer.gamesSitOut++;
    }

    if (gr.declarer >= 0) {

      // game was played

      boolean declWon = gr.declValue > 0;

      // declarer adjustments
      
      if (playerNum == 3) {
        cds[gr.declarer].decl3Pts += gr.declValue;
        cds[gr.declarer].gamesDecl3++;
        if (declWon) cds[gr.declarer].gamesDecl3Won++;
      } else {
        cds[gr.declarer].decl4Pts += gr.declValue;
        cds[gr.declarer].gamesDecl4++;
        if (declWon) cds[gr.declarer].gamesDecl4Won++;
      }

      // defender adjustments
      
      for (int i=0; i < 3; i++) {

        if (i != gr.declarer) {
          if (playerNum == 3) {
            cds[i].gamesDef3++;
            if (!declWon) cds[i].gamesDef3Won++;
          } else {
            cds[i].gamesDef4++;
            if (!declWon) cds[i].gamesDef4Won++;
          }
        }
      }

      // dealer adjustment

      if (playerNum == 4 && !declWon)
        cdDealer.gamesSitOutWon++;
    }

    gameStopped();
    
    playedGames.add(sg.copy());
    scoreSheet.addRowForGame(sg);

    cb.saveGame(sg.toSgf(true, SimpleState.WORLD_VIEW));
    
    save(); // save tables

    if (this.isFinished())
      saveTableHistory();

    sendToAll("state " + stateToString());
    return true;
  }

  /*
   * cmd: play <move>
   * 
   * play move
   */
  class SvcPlay extends ExecCmd
  {
    public void exec(String sender, String player, SReader reader)
    {
      if (playerInGameIndex(player) < 0) {
        error(sender, player, i18n("not_a_player"));
        return;
      }
      
      String move = reader.nextWord();

      if (move == null) {
        error(sender, player, i18n("move_missing"));
	return;
      }
      
      if (!gameInProgress) {
        error(sender, player, i18n("game_not_started"));
        return;
      }

      if (sg.isFinished()) {
        error(sender, player, i18n("game_finished"));
        return;
      }

      String[] views = new String[SimpleState.VIEW_NUM];
      int prevToMove = getGame().getToMove();
      String res = makeMove(player, move, views);

      if (res == null) {

	// move ok

        // Misc.msg("MOVE OK : " + client + " " + move);

        int pi = playerInGameIndex(player);

        if (pi == prevToMove) { // ignore out of order moves (RE, LE)

	  long now = System.currentTimeMillis();

	  if (pi >= 0) {

	    // deduct time
	    if (now < moveTimeStamp)
	      Misc.err("negative move time? " + now + " " + moveTimeStamp);
	    
	    remTimes[pi] -= now - moveTimeStamp;
	  }

	  moveTimeStamp = now; // new move starts now
        }

        sendMoveViews(pi+"", views);

        if (pi >= SimpleState.FORE_HAND && pi <= SimpleState.REAR_HAND && remTimes[pi] < 0) {

          Misc.msg("TIMEOUT after move. finished = " + sg.isFinished());
          
          // timeout

          res = sg.makeMove(SimpleState.WORLD_VIEW, "TI."+pi, views);
          if (res != null) {
            Misc.msg("!!!! Timeout after move error: " + res);
            // !!! gracefully skip over this SERIOUS bug
            // need to collect more data to see why this part is reached
          } else {
            sendMoveViews("w", views);
          }
        }
        
        if (handleGameEnd()) return;
        
        while (handleWorldMove(views)) {
          sendMoveViews("w", views); // handle world moves
          if (handleGameEnd()) break;
        }

	return;
      }

      // invalid move
      error(sender, player, res);
    }

    void error(String sender, String player, String msg)
    {
      sendError(sender, player, "play : " + msg);
    }
  }

  /*
   * cmd: invite <player/group>+
   * 
   * invite list of players or groups to join table
   */
  class SvcInvite extends ExecCmd
  {
    public void exec(String sender, String player, SReader reader)
    {
      int i = playerAtTableIndex(player);

      if (i < 0) {
        sendError(sender, player, i18n("not_a_player"));
        return;
      }

      for (;;) {

        String invPlayer = reader.nextWord();
        if (invPlayer == null) break;
        
        if (missingPlayerNum() == 0) {
          error(sender, player, i18n("no_vacancy"));
	  return;
	}

	if (cb.getClientData(invPlayer, true) == null) {
          error(sender, player, i18n("player_not_connected_colon") + " " + invPlayer);
	  continue;
	}

        String clientToInvite = cb.clientToInvite(invPlayer, getThis());

        if (clientToInvite == null) {
          error(sender, player, i18n("player_not_available_colon") + " " + invPlayer);
	  continue;
        }
        
	cb.send(clientToInvite, "invite " + sender + " " + tableId + " " + passwd);
      }
    }

    void error(String sender, String player, String msg)
    {
      sendError(sender, player, "invite : " + msg);
    }
  }

  
  // -------------- data + helpers ----------------

  Table getThis() { return this; }

  // extract group from user name
  static public String groupName(String user)
  {
    if (user == null) return null;
    int i = user.indexOf(':');
    if (i < 0) return user;
    return user.substring(0, i);
  }
  
  /**
   * new users become players
   * 
   * @return index at table >= 0 if ok, -1 otherwise
   */
  private int userArrived(String user)
  {
    if (observerIndex(user) >= 0) // already observer
      return -1;

    // reclaim earlier spot
    
    String group = groupName(user);
    boolean[] present  = { false, false, false, false }; // suffix present
      
    for (int i=0; i < playerNum; i++) {
      String p = playersAtTable[i];
      if (p == null) {
        // empty slot
        if (group.equals(groupName(playersInResultTable[i]))) {
          playersAtTable[i] = playersInResultTable[i];
          nameAtTable2UserName.put(playersAtTable[i], user);
          ready[i] = false;
          gameTalk[i] = true;
          practice[i] = false;
          return i;
        }
      } else {
        // occupied slot
        for (int j=0; j < 4; j++)
          if (p.equals(group + playerSuffixes[j]))
            present[j] = true;
      }
    }

    // otherwise find empty one
    for (int i=0; i < playerNum; i++) {
      if (playersAtTable[i] == null) {

        if (tourny && !group.equals(playersInResultTable[i]))
          continue; // at tournment tables only allow seated players            
        
        // empty: assign smallest available suffix
        int j;
        for (j=0; j < 4; j++) {
          if (!present[j]) break;
        }
        playersAtTable[i] = playersInResultTable[i] = group + playerSuffixes[j];
        nameAtTable2UserName.put(playersAtTable[i], user);
        played[i] = wins[i] = lastPts[i] = totalPts[i] = 0;
        ready[i] = false;
        gameTalk[i] = true;
	practice[i] = false;
        newSeries = true;
        return i;
      }
    }

    // table full
    return -1;
  }
        
  void nextSeries()
  {
    // reset result table and copy playersAtTable to other player arrays

    for (int k=0; k < MAX_PLAYER_NUM; k++) {
      playersInResultTable[k] = playersAtTable[k];
      played[k] = wins[k] = lastPts[k] = totalPts[k] = 0;
    }
    
    gameNum = 0;
    seriesId = cb.nextSeriesId();
  }
  
  /** lock table if necessary before using this function
   * @return i-th player at table (table format)
   */
  public String getPlayerAtTable(int i)
  {
    return playersAtTable[i];
  }

  /** lock table if necessary before using this function
   * @return i-th player at table (user format)
   */
  public String getUserAtTable(int i)
  {
    if (tourny) {
      return nameAtTable2UserName.get(playersInResultTable[i]);
    }
    
    if (playersAtTable[i] == null) return null;
    return nameAtTable2UserName.get(playersAtTable[i]);
  }
  
  /** lock table if necessary before using this function
   * @return set of players at in result table
   */
  public String[] getPlayersInResultTable()
  {
    return playersInResultTable;
  }

  /** lock table if necessary before using this function
   * @return set of players at in result table
   */
  public char getIPsetIdInResultTable(int index)
  {
    return ipSetIdInResultTable[index];
  }

  /** commands to service table */

  private ExecData svcCmds[] = {
    new ExecData("play",  "p", new SvcPlay()), // play
    new ExecData("tell",  "t", new SvcTell()), // table tell
    new ExecData("ready", "r", new SvcReady()), // player is ready
    new ExecData("34", "34",   new Svc34()), // player likes to switch seat number
    new ExecData("practice", "prac", new SvcPractice()), // player requests practice table
    new ExecData("gametalk", "g", new SvcGameTalk()), // player fine with game talk
    new ExecData("leave", "l", new SvcLeave()), // leave table
    new ExecData("invite", "i", new SvcInvite()), // invite player to table
  };

  //--------------------------------------------------------------------------

  // client functions

  /** return client data */
  public ClientData getClientData(String userId)
  {
    if (userId == null) return null;
    return cb.getClientData(userId, false);
  }

  // <gamenum> <playerid0> <time0> <playerid1> <time1> <playerid2> <time2> [ !/p/w <sgf-game> ]
  // p/w => observer
  // ! => player
  public void handleStartMsg(SReader sr)
  {
    touch();
    
    // game number
    String r1 = sr.nextWord();
    if (r1 == null) Misc.err("game number missing");
    gameNum = Integer.parseInt(r1);
    
    playersInGame = new String[GAME_PLAYER_NUM];

    for (int i=0; i < 3; i++) {
      playersInGame[i] = sr.nextWord();
      remTimes[i] = Double.parseDouble(sr.nextWord())*1000;

      Misc.msg("PLAYER " + i + " " + playersInGame[i] + " " + remTimes[i]);
    }

    if (gameNum == 1) {
      
      // series just started: clear history data and set names
      playedGames.clear();
      scoreSheet = new ScoreSheet(playerNum, playersInResultTable);
    }
    
    moveTimeStamp = System.currentTimeMillis();
    gameInProgress = true;
    
    int vid;
    String obsMode = sr.nextWord();
    
    if (obsMode == null) {

      // playing from start
      
      vid = playerInGameIndex(viewerName);

      Misc.msg("GAME OWNER : " + vid);
      sg = new SimpleGame(vid);

    } else {

      // observing or resuming

      if (obsMode.equals("!"))
        vid = playerInGameIndex(viewerName);  // resume
      else if (obsMode.equals("w"))
        vid = SimpleState.WORLD_VIEW;
      else
        vid = SimpleState.PUBLIC_VIEW;

      sg = new SimpleGame(vid);
      String sgf = sr.rest();

      BufferedReader br = new BufferedReader(new StringReader(sgf));

      String r = sg.fromSgf(br);
      if (r == null || !r.equals("")) {
        Misc.err("SGF corrupt! " + sgf + " : " + r);
        sg = null;
      }
    }

    for (int i=0; i < 3; i++) {
      sg.setPlayerName(i, playersInGame[i]);
    }
  }


  // <sgf-game>
  // public void handleEndMsg(SReader sr)
  public void handleEndMsg(String sgf)
  {
    touch();

    sg = new SimpleGame(SimpleState.WORLD_VIEW); // world owner
    //String sgf = sr.rest();
    BufferedReader br= new BufferedReader(new StringReader(sgf));

    String r = sg.fromSgf(br);
    if (r == null || !r.equals("")) {
      Misc.err("SGF corrupt! " + sgf + " : " + r);
      sg = null;
    }

    playedGames.add(sg);
    scoreSheet.addRowForGame(sg);
    
    gameInProgress = false;
  }
    
  // <player> <move>
  public void handlePlayMsg(SReader sr)
  {
    touch();

    String player = sr.nextWord();
    String move   = sr.nextWord();

    Misc.msg("MOVE-RCVD: " + player + " " + move);
    
    if (move == null)
      Misc.err("handlePlayMsg: missing move");

    // remaining times
    
    for (int i=0; i < GAME_PLAYER_NUM; i++) {
      String t = sr.nextWord();
      if (t == null)
	Misc.err("handlePlayMsg: time is missing");
      remTimes[i] = Double.parseDouble(t) * 1000.0;
    }
    
    int pi = SimpleState.WORLD_VIEW;
    if      (player.equals("0")) pi = 0;
    else if (player.equals("1")) pi = 1;
    else if (player.equals("2")) pi = 2;

    if (pi == sg.getToMove()) { // ignore out of order moves (RE, LE)
      moveTimeStamp = System.currentTimeMillis(); 
    }
    
    String r = sg.makeMove(pi, move, null);
    if (r != null)
      Misc.err("handlePlayMsg: move " + move + " error " + r);

    lastPlayer = pi;
    lastMove = move;
  }

  // <tableinfo>
  public void handleStateMsg(SReader sr)
  {
    touch();

    // player num
    
    String res = sr.nextWord();
    if (res == null) Misc.err("playernum value missing");
    setPlayerNum(Integer.parseInt(res));
    
    // players at table
    for (int i=0; i < MAX_PLAYER_NUM; i++) {

      // player name
      String r = sr.nextWord();    
      if (r == null) Misc.err("ready value missing");
      if (r.equals(NO_NAME))
        r = null;
      playersAtTable[i] = r;
    }

    // result table

    for (int i=0; i < MAX_PLAYER_NUM; i++) {

      disconnectedAtTable[i] = false;
      
      // player name
      String r = sr.nextWord();    
      if (r == null) Misc.err("player name value missing");
      playersInResultTable[i] = r;

      // ipset id
      r = sr.nextWord();    
      if (r == null) Misc.err("ipset id missing");
      ipSetIdInResultTable[i] = r.charAt(0);

      // played
      r = sr.nextWord();
      if (r == null) Misc.err("played value missing");
      played[i] = Integer.parseInt(r);
      
      // wins
      r = sr.nextWord();
      if (r == null) Misc.err("wins value missing");
      wins[i] = Integer.parseInt(r);
      
      // lastpoints
      r = sr.nextWord();
      if (r == null) Misc.err("lastpts value missing");
      lastPts[i] = Integer.parseInt(r);

      // totalpoints
      r = sr.nextWord();
      if (r == null) Misc.err("totalpts value missing");
      totalPts[i] = Integer.parseInt(r);

      // 34
      r = sr.nextWord();
      if (r == null) Misc.err("34 value missing");
      threeFour[i] = r.equals("1");

      // practice
      r = sr.nextWord();
      if (r == null) Misc.err("practice value missing");
      practice[i] = r.equals("1");

      // gameTalk
      r = sr.nextWord();
      if (r == null) Misc.err("gameTalk value missing");
      gameTalk[i] = r.equals("1");

      // ready
      r = sr.nextWord();
      if (r == null) Misc.err("ready value missing");
      ready[i] = r.equals("1");
    }

    // progress
    String r = sr.nextWord();
    if (r == null)
      Misc.err("inprogress value missing");

    if (!r.equals("false") && !r.equals("true")) {
      Misc.err("corrupt inprogress value " + r);
    }
    
    gameInProgress = r.equals("true");

    // game num

    r = sr.nextWord();
    if (r != null) {
      // optional for now (handle old server messages as well
      try { gameNum = Integer.parseInt(r); }
      catch (Throwable e) { Misc.err("corrupt game number"); }
    }
  }

  public void handleGoMsg()
  {
    touch();
    gameStarted = true;
  }

  public void handleStopMsg()
  {
    touch();
    gameStarted = false;
  }

  void touch()
  {
    lastTouched = System.currentTimeMillis();
  }

  /** how long not touched in seconds */
  public double notTouchedTime() {
    return (System.currentTimeMillis() - lastTouched) * 0.001;
  }
  
  public int getLastPlayer() { return lastPlayer; }
  public String getLastMove() { return lastMove; }


  // save tournament table after game ends
  public void save() {
    if (!tourny) return; // don't bother with non-tournament tables

    Calendar now = Calendar.getInstance();
    StringBuffer sb = new StringBuffer();
    sb.append(String.format(Locale.US, "#info %04d-%02d-%02d %02d:%02d:%02d ",
                            now.get(Calendar.YEAR),
                            (now.get(Calendar.MONTH)+1),
                            now.get(Calendar.DAY_OF_MONTH),
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            now.get(Calendar.SECOND)));
    
    sb.append(tableId + " ");
    sb.append(creator + " ");
    sb.append(seriesId + " ");
    sb.append(type + " ");
    sb.append(playerNum + " ");
    sb.append(passwd + " ");
    sb.append(blockNum + " ");

    for (int i=0; i < playerNum; i++) {
      sb.append(playersInResultTable[i] + " " +
                played[i] + " " +
                wins[i] + " " +
                lastPts[i] + " " +
                totalPts[i] + " ");
    }

    sb.append("\ngames= " + playedGames.size() + "\n");
    
    if (playedGames.size() != gameNum)
      Misc.err("game history length corrupt " + playedGames.size() + " " + gameNum);

    for (SimpleGame g : playedGames) {
      sb.append(g.toSgf(true, SimpleState.WORLD_VIEW) + "\n");
    }

    if (false) {
      // test: archive after each game
      String path = cb.archive(getThis(), sb);
      sendToAll("tell <table> path= " + path);
    }
    
    if (gameNum < getMaxGameNum()) {

      cb.saveTable(tableId, sb);

    } else {

      // archive table when list is complete

      String path = cb.archive(getThis(), sb);

      // send path to players
      sendToAll("tell <table> path= " + path);

      // tourny finished - delete save entry
      cb.saveTable(tableId, null);
    }
  }

  // Main difference between save() and save2() is simply that save2() returns a string.
  public String save2() {
    if (!tourny) return null; // don't bother with non-tournament tables

    Calendar now = Calendar.getInstance();
    StringBuffer sb = new StringBuffer();
    sb.append(String.format(Locale.US, "#info %04d-%02d-%02d %02d:%02d:%02d ",
                            now.get(Calendar.YEAR),
                            (now.get(Calendar.MONTH)+1),
                            now.get(Calendar.DAY_OF_MONTH),
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            now.get(Calendar.SECOND)));
    
    sb.append(tableId + " ");
    sb.append(creator + " ");
    sb.append(seriesId + " ");
    sb.append(type + " ");
    sb.append(playerNum + " ");
    sb.append(passwd + " ");
    sb.append(blockNum + " ");

    for (int i=0; i < playerNum; i++) {
      sb.append(playersInResultTable[i] + " " +
                played[i] + " " +
                wins[i] + " " +
                lastPts[i] + " " +
                totalPts[i] + " ");
    }

    sb.append("\ngames= " + playedGames.size() + "\n");

    if (playedGames.size() != gameNum)
      Misc.err("game history length corrupt " + playedGames.size() + " " + gameNum);

    for (SimpleGame g : playedGames) {
      sb.append(g.toSgf(true, SimpleState.WORLD_VIEW) + "\n");
    }

    if (false) {
      // test: archive after each game
      String path = cb.archive(getThis(), sb);
      sendToAll("tell <table> path= " + path);
    }

    return sb.toString();
  }

  /* Saves table archive file to local directory so that it can later be converted to an HTML
     result table and HTML game histories while running a local server. - Ryan */
  public void saveTableHistory() {
    if (!tourny) return;
    PrintWriter tableWriter;
    StringBuffer outputFileName = new StringBuffer();
    String gameFileSuffix = ".html.game";

    /* Directory name is not internationalized.  Otherwise, for each language there will
       have to be a separate if-statement in httpRequest() in Server that decides what to
       do if the string starts with any translation of "archive/table". */
    String mainDirectoryName = "archive/table";
    File mainDirectory = new File(mainDirectoryName);

    try {
      mainDirectory.mkdir();
    }
    catch (SecurityException e) {
      if (!mainDirectory.exists())
        Misc.err("Directory " + mainDirectoryName + " could not be created.");
    }

    String dateDirectoryNamePartial = Misc.currentUTCdate().substring(0, 10).replace('-', '_');
    String dateDirectoryName = mainDirectoryName + "/" + dateDirectoryNamePartial;
    File dateDirectory = new File(dateDirectoryName);

    try {
      dateDirectory.mkdir();
    }
    catch (SecurityException e) {
      if (!dateDirectory.exists())
        Misc.err("Directory " + dateDirectory + " could not be created.");
    }

    String tableDirectoryNamePartial;

    if (getId().startsWith("t:"))
      tableDirectoryNamePartial = getId().substring(2, getId().length()); // remove the t: for the directory name
    else
      tableDirectoryNamePartial = getId().substring(1, getId().length()); // remove the leading decimal

    StringBuffer tableDirectoryName = new StringBuffer();
    /* Have to initialize tableDirectory; otherwise, I get a compiler error. */
    File tableDirectory = new File("");

    for (int index = 0; index < Integer.MAX_VALUE; index++) {
      tableDirectoryName.delete(0, tableDirectoryName.length());
      tableDirectoryName.append(dateDirectoryName);
      tableDirectoryName.append("/");
      tableDirectoryName.append(tableDirectoryNamePartial);

      if (index != 0) tableDirectoryName.append("_" + index);

      tableDirectory = new File(tableDirectoryName.toString());
      if (!tableDirectory.exists())
        break;
    }

    try {
      tableDirectory.mkdir();
    }
    catch (SecurityException e) {
      if (!tableDirectory.exists())
        Misc.err("Directory " + tableDirectoryName + " could not be created.");
    }

    outputFileName.append(tableDirectoryName);
    outputFileName.append("/");
    outputFileName.append("results");
    outputFileName.append(".html.table");

    try {
      tableWriter = new PrintWriter(new FileWriter(outputFileName.toString()));
      tableWriter.print(save2());
      tableWriter.close();

      /* (not saving individual game histories anymore)
         for (int index = 0; index < playedGames.size(); index++) {
         tableWriter = new PrintWriter(new FileWriter(tableDirectoryName.toString() + "/" + index + gameFileSuffix));
         tableWriter.print(playedGames.get(index).toSgf(false, SimpleState.WORLD_VIEW));
         tableWriter.close();
         }
      */
    }
    catch (IOException e) {
      Misc.err("failed to print sgf file");
    }	
  }

  // load tournament table when server restarts
  public void load(Random rng, BufferedReader r) {

    this.rng = rng;
    playedGames = new ArrayList<SimpleGame>();
    
    try {

      if (!"#info".equals(nextToken(r)))
        throw new Exception("#info not found");
      
      dateTime.append(nextToken(r));
      dateTime.append("/");
      dateTime.append(nextToken(r));
      dateTime.append("/");
      dateTime.append("local"); // table archive files use local time, not UTC

      tableId = nextToken(r); // will have to delete the "t:" or "." from the beginning when using in HTML result tables
      creator = nextToken(r);
      seriesId = Integer.parseInt(nextToken(r));
      type = nextToken(r);
      playerNum = Integer.parseInt(nextToken(r));
      passwd = nextToken(r);
      blockNum = Integer.parseInt(nextToken(r));

      for (int i=0; i < playerNum; i++) {
        playersInResultTable[i] = nextToken(r);
        disconnectedAtTable[i] = true; // so that every player will auto-join
        played[i] = Integer.parseInt(nextToken(r));
        wins[i] = Integer.parseInt(nextToken(r));
        lastPts[i] = Integer.parseInt(nextToken(r));
        totalPts[i] = Integer.parseInt(nextToken(r));
      }

      if (!"games=".equals(nextToken(r)))
        throw new Exception("games= not found");
        
      gameNum = Integer.parseInt(nextToken(r));

      for (int i=0; i < gameNum; i++) {
        SimpleGame sg = new SimpleGame(SimpleState.WORLD_VIEW);
        sg.fromSgf(r);
        playedGames.add(sg);
      }

      // restore scoresheet from games
      scoreSheet = new ScoreSheet(playerNum, playersInResultTable);
      
      for (int i=0; i < gameNum; i++) {
        scoreSheet.addRowForGame(playedGames.get(i));
      }

      if (playedGames.size() != scoreSheet.size())
        Misc.err("sizes disagree!");
      
    }
    catch (Throwable e) { Misc.msg("!!! load error " + Misc.stack2string(e)); return; }
    
    viewerName = null; // server
    tourny = true;
  }

  static private String nextToken(BufferedReader r) throws Exception
  {
    // skip ws
    int c;
    while (true) {
      try { c = r.read(); }
      catch (IOException e) { return null; }
      if (c < 0) return null;
      if (c != ' ' && c != '\n') break;
    }
   
    StringBuffer sb = new StringBuffer();
   
    while (true) {
     
      sb.append((char)c);
      try { c = r.read(); }
      catch (IOException e) { return null; }
     
      if (c < 0 || (c == ' ' || c == '\n')) break;
    }
   
    Misc.msg("READ TOKEN : " + sb.toString());
    return sb.toString();
  }


  public void readScoreSheet(SReader sr)
  {
    scoreSheet.load(sr);
  }

  public ScoreSheet getScoreSheet()
  {
    return scoreSheet;
  }
  
}
