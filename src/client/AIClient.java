/**
 * Extendable abstract class for creating AI clients
 *
 * created Jeff Long
 * extended Jan Schaefer, Michael Buro
 * updated by M. Buro
 *
 * licensed under GPLv3
 */

package client;

import common.*;
import java.util.*;
import java.io.*;
import java.awt.event.*;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;


// time is fixed to 1 sec right now, see !!! below

public abstract class AIClient implements ServiceClient.ServiceMsgHandler
{
  // thread that computes a move
  
  class MoveThread extends Thread
  {
    PlayerData pd;
    boolean started;
    boolean finished;
    Table table;
    boolean delay; // whether to use no less than 2 seconds at the beginning of the cardplay phase
    // to avoid leaking information about singletons

    public MoveThread(Table table, PlayerData playerData, boolean delay)
    {
      this.table = table;
      this.pd    = playerData;
      this.delay = delay;
    }

    public void interrupt()
    {
      if (started && !finished) {
        pd.player.interrupt(); // should stop move computation
      }
    }
    
    public void run()
    {
      started = true;

      int pi = table.playerInGameIndex(table.getViewerName());

      if (pi < 0 || pi > 2)
        Misc.err("pi " + pi + " " + table.getViewerName());

      long startTime = System.currentTimeMillis();
      String move = pd.player.chooseMove(1.0 /* !!! fixed for now table.getRemTime(pi)*/);

      if (pd.player.isInterrupted()) {
        finished = true;
        return; // search interrupted
      }

      // move legal?

      String[] moves = new String[1000];
      int toMoveCards = 0;
      boolean doDelay = false;
      boolean sendSC = false;
      
      synchronized (sc) {
        
        SimpleState newState = table.getGame().cloneLastState();
        doDelay = newState.numCards(newState.getToMove()) >= 6; // must have >= 6 cards
        doDelay &= (newState.getPhase() == SimpleState.CARDPLAY); // must be in cardplay phase
        doDelay &= (newState.getTrickCardNum() == 1 || newState.getTrickCardNum() == 2); // follow up

        // show cards?

        sendSC =
          newState.getView() == newState.getDeclarer() &&   // viewer is declarer
          newState.getPhase() == SimpleState.CARDPLAY &&    // we play cards
          !newState.getGameDeclaration().ouvert &&          // not ouvert game
          newState.getToMove() == newState.getDeclarer() && // declarer to move
          ( (newState.getTrickCardNum() % 3 == 0 &&  // declarer gets rest
            newState.trickLeaderGetsAll() &&
            newState.getTrickNum() <= 8 ) ||
           ( newState.safeNull() ));                 // declarer has safe null

        String res = newState.makeMove(newState.getToMove(), move, null);
        if (res != null) {
          
          // illegal move, send random move
          
          sendToTable(table.getId(), table.getViewerName(),
                      "tell Computed illegal move " + move + ": " + res + "! Send random move instead.");
          
          int mn = table.getGame().getCurrentState().genMoves(moves);
          int choice;
          
          synchronized (rng) {
            choice = rng.nextInt() % mn;
          }
          move = moves[choice];
        }
      }

      if (doDelay) {
        long endTime = System.currentTimeMillis();
        double spent = (endTime-startTime)*0.001;
        if (spent < 2.0) {
          Misc.sleep((int)(2.0-spent)*1000);
        }
      }

      // send SC before move so that the other players can take information
      // into accout right away
      
      if (!pd.sentSC && sendSC) {
        pd.sentSC = true;
        sendToTable(table.getId(), table.getViewerName(), "play SC");
      }

      // send it
      sendToTable(table.getId(), table.getViewerName(), "play " + move);

      finished = true;
    }
  }

  // associated with table
  class PlayerData
  {
    public Player player;
    public MoveThread mt;
    boolean sentSC; // only send SC once
    boolean sentRE; // only send RE once
  }
  
  
  ServerClient sc; // The guy to whom we'll be sending/receiving messages

  // one player for each context (tableid + nameontable)
  TreeMap<String, PlayerData> t2p = new TreeMap<String, PlayerData>();

  RNG rng = new RNG(); // for illegal moves
  javax.swing.Timer timer; // table watchdog timer
  
  /**
   * This should be the only method that needs to change between clients; it
   * initializes the Player object to be the kind of player we want
   */
  public abstract Player newPlayer();

  /**
   * Initializes the player
   * 
   * @param args Arguments
   */
  public void init(String[] args) {

    Options opt = new Options();

    opt.put("-h", "bodo1.cs.ualberta.ca", "client mode: server host");
    opt.put("-p", new Integer(80), "client mode: server port");
    opt.put("-id", "foo", "iss user name");
    opt.put("-pw", "foo", "iss password");
    opt.put("-cmds", "", "initial iss commands (|=sep _=space)");
    opt.put("-type", "", "player type (jni)");
    opt.put("-par", "",  "player parameters (jni)");
    opt.put("-lib", "",  "c++ library (for jni player - skattaplayer or xskatplayer)");    
    opt.put("-delay", "play no faster than 1-1.5 seconds per move");
    opt.put("-quit", "quit once an hour to free memory");
    opt.put("-aiId", "kermit", "select AI (kermit/zoot/theCount)"); 
    opt.put("-pos",   new String(""), "Game to analyze");
    opt.put("-rewind", new Integer(0), "Rewind by this many moves to analyze current state.");
    

    if (!opt.parse(args)) {
      Misc.err("option error");
    }

    id         = opt.getString("-id");
    playerType = opt.getString("-type");
    params     = opt.getString("-par");
    library    = opt.getString("-lib");
    delay      = opt.getSwitchOn("-delay");
    quit       = opt.getSwitchOn("-quit");
    aiId       = opt.getString("-aiId");    
    String gamefileName = opt.getString("-pos");
    int rw = opt.getInteger("-rewind");   

    if (!gamefileName.equals("")) {

      try {
        BufferedReader inStream = null;
        inStream = new BufferedReader(new FileReader(new File(gamefileName)));
        Misc.msg("read game from file " + gamefileName);
        askForMoves(rw, inStream);
      }
      catch (Exception e) {
        Misc.err("Could not find file: " + gamefileName);
      }

    } else { 
      sc = new ServerClient();

      String r = sc.run(this, "", opt.getString("-h"), opt.getInteger("-p")
                      .intValue(), opt.getString("-id"), opt.getString("-pw"), opt
                      .getString("-cmds"));
      if (r != null) {
        Misc.err(r);
        return;
      }

    // start table watchdog timer
    
      timer = new javax.swing.Timer(20000, new ActionListener() { // every 20 seconds ...
	public void actionPerformed(ActionEvent evt) {
          
          Misc.msg("watchdog timer");
          
          synchronized (sc) {

            if (ManagementFactory.getRuntimeMXBean().getUptime() >= 1*24*60*60*1000) {
              waitQuit = true;
            }
            
            Map<String, Table> joinedTables = sc.getServiceClient().getJoinedTables();

            // no need to wait for leaving tables anymore as games can be continued
            if (waitQuit /* && joinedTables.keySet().isEmpty()*/ ) { 
              Misc.msg("TIME IS UP ... QUIT!");
              System.exit(0); // leave after a while
            }
            
            for (String name : joinedTables.keySet()) {

              Table table = joinedTables.get(name);

              if (table.playersAtTableNum() <= 1 ||
                  (!table.getInProgress() && table.notTouchedTime() >= 100)) {
                // only player or no game for x seconds: leave
                sendToTable(table.getId(), table.getViewerName(), "leave");
              }
            }

            sendToServer("time"); // keep connection alive
          }
	}
      });

      timer.start();

    }

    
  }

  /** Prints out the move that would be selected by this client's
   *  player in the given situation.
  */
  public void askForMoves(int rewind, BufferedReader br) {

    SimpleGame g = new SimpleGame(4);
    String ret = g.fromSgf(br);

    if (ret == null)
      Misc.err("no game found");
    
    if (!g.rewindMoves(rewind))
      Misc.err("rewind problems");

    SimpleState s = g.getCurrentState();

    if (s.getPhase() == SimpleState.FINISHED) {
      Misc.err("game already finished");
    }

    int viewer = s.getToMove();

    if (viewer < 0 || viewer >= 3)
      Misc.err("non-player to move");
    
    SimpleGame newG = new SimpleGame(viewer);
    newG.replay(g, viewer);
    s = newG.getCurrentState();
    
    Player p = newPlayer();
    String move = p.chooseMove(newG, 2.0);
    Misc.msg("State is:\n" + s.toString());
    Misc.msg("Selected move: " + move);
  }

  /**
   * Handles all service messages
   * 
   * @param msg Message type
   * @param line message
   */
  public void handleServiceMsg(ServiceClient.ServiceMsg msg, String line)
  {
    Misc.msg("handling: " + line);
		
    if (msg instanceof ServiceClient.CreateMsg) {

      // send ready when entering a table
      Table table = ((ServiceClient.CreateMsg)msg).table;
      sendToTable(table.getId(), table.getViewerName(), "ready");
      
    } else if (msg instanceof ServiceClient.TableStartMsg) {
			
      // start message from a table
      startGameAtTable(((ServiceClient.TableStartMsg) msg).table);
			
    } else if (msg instanceof ServiceClient.TableGoMsg) {
			
      // go message from a table

      Misc.msg("IGNORING GO MSG");
      // makeMoveAtTable(((ServiceClient.TableGoMsg) msg).table);
			
    } else if (msg instanceof ServiceClient.TablePlayMsg) {
			
      // play message from a table
      makeMoveAtTable(((ServiceClient.TablePlayMsg) msg).table);
			
    } else if (msg instanceof ServiceClient.TableEndMsg) {

      Table table = ((ServiceClient.TableEndMsg) msg).table;
      processGameEnd(table, ((ServiceClient.TableEndMsg) msg).gameHist);
      if (!waitQuit) {
        // set ready for the next game        
        sendToTable(table.getId(), table.getViewerName(), "ready");
      }
			
    } else if (msg instanceof ServiceClient.InviteMsg) {

      if (!waitQuit) {
        // accept all invitations
        ServiceClient.InviteMsg imsg = (ServiceClient.InviteMsg) msg;
        sendToServer("join " + imsg.tableId + " " + imsg.tablePassword);
      }

    } else if (msg instanceof ServiceClient.CatchAllMsg) {
			
      // all other messages
      ServiceClient.CatchAllMsg nmsg = (ServiceClient.CatchAllMsg) msg;
			
      // still unhandled messages
      Misc.msg("unhandled command " + nmsg.parts[0]);
		
    } else {
			
      Misc.msg("????");
    }
  }
	
	
  /**
   * send string to server
   */
  protected void sendToServer(String msg)
  {
    sc.send(msg);
    Misc.msg("send: " + msg);
  }
	

  /**
   * send string to table
   * 
   * @param tableName and player in game
   */
  protected void sendToTable(String tableName, String player, String msg)
  {
    sendToServer("table " + tableName + " " + player + " " + msg);
  }


  /**
   * starts a game
   * 
   * @param table Table where a game started
   */
  protected void startGameAtTable(Table table)
  {
    SimpleGame sg = table.getGame();

    if (sg.getOwner() < 0 || sg.getOwner() > 2) {
      Misc.msg("Skip game, I am not a player!");
      return;
    }
    
    Misc.msg("Start Game At Table!");
		
    if (sg == null) Misc.err("game = null");

    // create player if there is none

    String tvid = table.getTableViewerId();
    PlayerData pd = t2p.get(tvid);

    // pd == null after game finished (see below)
    
    if (pd == null) {

      pd = new PlayerData();
      pd.player = newPlayer();
      pd.player.setContext(tvid);

      //!!! was: pd.player.reset(sg.getCurrentState().getView());
      // need to replay game when resuming
      String e = pd.player.replay(sg, sg.getCurrentState().getView());      
      if (e != null) {
        Misc.err("replay error: " + e);
      }
      t2p.put(tvid, pd);
    }

    pd.sentSC = pd.sentRE = false;
    
    if (pd.mt != null) {
      
      // finish previous task
      
      pd.mt.interrupt();
      
      // wait until finished
      while (!pd.mt.finished) {
        Misc.sleep(10);
      }
    }

    SimpleState st = sg.getCurrentState();

    Misc.msg("STARTING UP " + st.getView() + " " + st.getToMove());
    
    if (st.getPhase() != SimpleState.FINISHED &&
        st.isViewerToMove()) {

      Misc.msg("WE ARE TO MOVE");
      
      // spawn move computation thread
      pd.mt = new MoveThread(table, pd, delay);
      pd.mt.run();
    }
    
  }

  /**
   * Makes a move at a table
   * 
   * @param table Table where the move should be played
   */
  protected void makeMoveAtTable(Table table)
  {
    SimpleGame sg = table.getGame();

    if (sg.getOwner() < 0 || sg.getOwner() > 2) {
      Misc.msg("Skip move, I am not a player!");
      return;
    }
    
    SimpleState st = sg.getCurrentState();
    String tvid = table.getTableViewerId();
    PlayerData pd = t2p.get(tvid);
    if (pd == null) Misc.err("pd = null");

    if (st.getPhase() == SimpleState.FINISHED) {

      // Jeff - removed this, since we need the player to stick around
      // to get the 'game end' message.  Moved to 'processGameEnd'
      // instead
      
      // remove player
/*
      if (pd.mt != null) {
      
        pd.mt.interrupt();
        
        // wait until finished
        while (!pd.mt.finished) {
          Misc.sleep(100);
        }
      }

      pd.player.dispose();
      t2p.remove(tvid);
*/
      return;
    }

    Misc.msg("Phase: " + st.getPhase());
    
    // update player game with last move

    int    who  = sg.getMoveHist().get(sg.getMoveHist().size()-1).source;
    String what = sg.getMoveHist().get(sg.getMoveHist().size()-1).action;
    
    pd.player.gameChange(who, what);

    //    boolean getsAll =
    //  st.getPhase() == SimpleState.CARDPLAY &&
    //  st.trickLeader() == st.getDeclarer() && // resign if declarer gets all
    //  st.getTrickNum() <= 7 &&
    //  st.trickLeaderGetsAll();
    //
    //Misc.msg("");
    //Misc.msg(st.toString());
    //Misc.msg("TRICK LEADER GETS ALL : " + getsAll);

    Misc.msg("WHAT: " + who + " " + what + " " + st.getView() + " " + st.getDeclarer());
    
    if (!pd.sentRE &&
        st.getPhase() == SimpleState.CARDPLAY &&
        st.getView() != st.getDeclarer() &&      
        what.equals("RE")) {
          
      // resign if partner resigns
      pd.sentRE = true;
      sendToTable(table.getId(), table.getViewerName(), "play RE");
      return; // game finished, no reason to compute move
    }
    
    if (!pd.sentRE &&
        st.getPhase() == SimpleState.CARDPLAY &&      
        st.getView() != st.getDeclarer() &&
        (
         ( st.trickLeader() == st.getDeclarer() && // resign if declarer gets all
           st.getGameDeclaration().type != GameDeclaration.NULL_GAME &&
           st.trickLeaderGetsAll() &&
           st.getTrickNum() <= 8                   // no at the very end
           )
         ||
         st.safeNull()          // resign if soloist has a safe null hand
         )) {
      pd.sentRE = true;
      sendToTable(table.getId(), table.getViewerName(), "play RE");
      // resign but still compute move (in case the co-defender doesn't resign)
    }

    if (st.isViewerToMove()) {
      // spawn move computation thread if viewer is to move
      pd.mt = new MoveThread(table, pd, delay);
      pd.mt.run();
    }
  }

  public void processGameEnd(Table table, String gameHist) {
  
    String tvid = table.getTableViewerId();
    PlayerData pd = t2p.get(tvid);
    pd.player.gameOver(gameHist);
 
    // remove player

    if (pd.mt != null) {
      
      pd.mt.interrupt();
        
      // wait until finished
      while (!pd.mt.finished) {
        Misc.sleep(100);
      }
    }

    pd.player.dispose();
    t2p.remove(tvid);

  }

  public String getID() { return id; }
  
  public String id; // iss id
  public String playerType, params, library; // player info (used for jni players to distinguish player types)
  public String aiId; // selects AI (usually = id)
  boolean delay;
  boolean quit; // true: quit one a week to free memory
  boolean waitQuit; // if true, don't accept new games and quit when last game is finished

}
