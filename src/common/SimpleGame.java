/* $Id: SimpleGame.java 11006 2012-01-13 04:03:36Z mburo $
 * SimpleGame.java
 *
 * (c) Michael Buro, Jeff Long, Nathan Taylor
 * licensed under GPLv3
 */

package common;

import java.util.*;
import java.io.*;


public class SimpleGame
{
  // Game indices: important points in the vector of states
  public static final int GI_GAMESTART    = 0;
  public static final int GI_BIDDINGSTART = 1;
  public static final int GI_SKAT_OR_HAND = 2;
  public static final int GI_CARDPLAY     = 3;
  public static final int GI_INDICESCOUNT = 4;
  
  private int[] bookmarks = new int[GI_INDICESCOUNT];
  private int [] trickBookmarks = new int[10]; // first state in trick i (0: right after declaring)
  private String place, date, comment, seriesId, gameId;
  private String[] names   = new String[3];
  private String[] ratings = new String[3]; // fixme: integers?
  private String  sgfResult;

  // Important: if you add members here, you need to adjust copy()!
  
  ArrayList<Move> moveHist = new ArrayList<Move>();

  int owner; // The player in whose view this SimpleGame exists.  
  
  public ArrayList<SimpleState> stateHist = new ArrayList<SimpleState>();

  /** Creates a new instance of SimpleGame */
  public SimpleGame()
  {
    reset(SimpleState.WORLD_VIEW);
  }
  
  public SimpleGame(int owner_)
  {
    if (owner < 0)
      Misc.err("simplegame owner < 0");
    reset(owner_);
  }

  /** reset game (0 moves) , new owner */  
  public void reset(int o)
  {
    owner = o;
    reset();
  }
  
  /** reset game (0 moves) , keep owner */
  public void reset()
  {
    for (int i = 0; i < GI_INDICESCOUNT; i++)
      bookmarks[i] = -1;

    bookmarks[GI_GAMESTART] = 0;

    for (int i=0; i < 10; i++) {
      trickBookmarks[i] = -1;
    }

    place = date = comment = seriesId = gameId = "";

    names[0] = names[1] = names[2] = "";
    ratings[0] = ratings[1] = ratings[2] = "";
    sgfResult = "";
    
    stateHist.clear();
    stateHist.add(new SimpleState(owner));

    moveHist.clear();
  }

  /** keeps initialMoves */
  public void prune(int initialMoves)
  {
    while (stateHist.size() > initialMoves+1) {
      undoMove();
    }
  }

  /** @return true if this SimpleGame is in Server mode
  */
  public boolean isServer() {
    return (owner < 0);
  }

  /** @return move list (trusting the user doesn't change it!)
  */
  public ArrayList<Move> getMoveHist() {
    return moveHist;
  }

  public Move getMove(int i) {
    if (i < moveHist.size()) {
      return moveHist.get(i);
    } else {
      Misc.err("Requested move beyond end of move list");
    }
    return null;
  }

  /** Returns the number of moves in the game.
  */
  public int getNumMoves() {
    return moveHist.size();    
  }

  public Move getLastMove() {
    return moveHist.get(moveHist.size()-1);
  }
  
  /** @return The player in whose view this SimpleGame is represented
  */
  public int getOwner() {
    return owner;
  }

  /** change the owner of this SimpleGame */
  public void setOwner(int owner_) {
    owner = owner_;
  }

  /** set comment string */
  public void setComment(String comment) {
    this.comment = comment;
  }

  /** set name string */
  public void setName(int index, String name) {
    names[index] = name;
  }

  public int getPlayerIndex(String s)
  {
    for (int i=0; i < 3; i++) {
      if (names[i].equals(s))
        return i;
    }
    return -1;
  }

  public SimpleState getCurrentState()
  {
    int s = stateHist.size();
    if (s == 0)
      throw new RuntimeException("stateHist vector is empty!");
    
    return stateHist.get(s-1);
  }

  /** Deletes the last state from the state history.  Use with care! */
  public void removeLastState() {
    stateHist.remove(stateHist.size()-1);
  }

  /** Adds the SimpleState s to the end of the state history.  Use with care! */
  public void appendState(SimpleState s) {
    stateHist.add(s);
  }
  
  public SimpleState getState(int index)
  { 
    if (index < 0 || index >= stateHist.size())
      throw new RuntimeException("stateHist index out of range");
    
    return stateHist.get(index);
  }
  
  public int getStateNum()
  { 
    return stateHist.size();
  }

  int getBookmark(int index)
  {
    return bookmarks[index];
  }
  
  public int getCardPlayBookmark()
  {
    return bookmarks[GI_CARDPLAY];
  }

  public int getSkatOrHandBookmark() {
    return bookmarks[GI_SKAT_OR_HAND];
  }
  
  public SimpleState cloneLastState()
  {
    if (stateHist.size() == 0)
      throw new RuntimeException("stateHist vector is empty!");
    
    return SimpleState.createCopy(getCurrentState());
  }

  /** Rewind the last m moves.
  */
  public boolean rewindMoves(int m) {
    for (int i = 0; i < m; i++) {
      if (stateHist.size() > 0) {
        undoMove();
      } else {
        return false;
      }
    }
    return true;
  }

  public boolean rewindToTrick(int trick) {
    int trickmark = getTrickBookmark(trick);
    if (trickmark < 0) return false;

    while (stateHist.size() > trickmark+1) {
      undoMove();
    }

    return true;
  }

  /** Repeatedly undoes moves until we reach the given bookmark, effectively erasing old states
  */

  // fixme: check, should return boolean to indicate whether rewind was possible
  public boolean rewindToBookmark(int bookmark) {
    assert ( (bookmark >= GI_GAMESTART) && (bookmark <= GI_CARDPLAY) );
    assert (bookmarks[bookmark] != -1);
  
    while ( (stateHist.size() > bookmarks[bookmark]+1) && (bookmarks[bookmark] > -1) ) {
      undoMove();
    }

    return true;
    
  }

  /** @return true if rewind successful */
  public boolean rewindToCardPlay() {
    int bm = bookmarks[GI_CARDPLAY];
    if (bm < 0) return false;
    
    while (stateHist.size() > bm+1) {
      undoMove();
    }

    return true;
  }

  /** @return true if rewind successful */  
  public boolean rewindToSkatDeclare() {

    SimpleState s = getCurrentState();

    if (s.getPhase() < SimpleState.SKAT_OR_HAND_DECL) {
      return false;
    }
    
    while (s.getPhase() > SimpleState.SKAT_OR_HAND_DECL) {
      undoMove();
      s = getCurrentState();
    }

    return true;
  }

  /** @return true if rewind successful */  
  public boolean rewindToBiddingStart()
  {
    int bm = bookmarks[GI_BIDDINGSTART];
    if (bm < 0) return false;
    
    while (stateHist.size() > bm+1) {
      undoMove();
    }

    return true;
  }
  
  /** Undoes that most recent move    
  */
  
  public void undoMove()
  {
    assert (moveHist.size() > 0);
  
    // Are we about to rewind past an important part of the game?  If so, unbookmark it
    int n = stateHist.size();
    if (bookmarks[GI_GAMESTART] == n - 1)
      bookmarks[GI_GAMESTART] = -1;
    else if (bookmarks[GI_BIDDINGSTART] == n - 1)
      bookmarks[GI_BIDDINGSTART] = -1;
    else if (bookmarks[GI_SKAT_OR_HAND] == n - 1)
      bookmarks[GI_SKAT_OR_HAND] = -1;
    else if (bookmarks[GI_CARDPLAY] == n - 1)
      bookmarks[GI_CARDPLAY] = -1;
  
    stateHist.remove(stateHist.size()-1);
    moveHist.remove(moveHist.size() -1);
  }
  
  /** Plays a certain move. The most recent game state is cloned and
   * the player/move pair is processed from there.  If views != null
   * and move succeeds, store player views of that move in array
   * views.
   * @return null on success; error message if something failed.
   */

  public String makeMove(int player, String move, String[] views)
  {
    return makeMove(player, move, views, false);
  }
  
  public String makeMove(int player, String move, String[] views, boolean fixResign)
  {
    // Misc.msg("Game makeMove " + player + " " + move);

    stateHist.add(cloneLastState());

    if (fixResign && move.equals("RE")) {

      if (views != null)
        Misc.err("views must be null when fixing RE moves");
      
      // in doskv games, only the player to move can resign, but
      // convert.C doesn't know anything about skat, so fix it
      player = getCurrentState().getToMove();

      String ret = getCurrentState().makeMove(player, move, views);
      int n = stateHist.size();

      if (ret != null) {
        stateHist.remove(n-1);

        if (isFinished() && ret.equals("game already finished")) 
          return null; // when fixing, ignore RE moves after game end
      }

      moveHist.add(new Move(player, move)); // append fixed RE move
    
      if (player != getCurrentState().getDeclarer()) {

        // one defender resigned
        // insert resignation of other defender (need both to end game)

        stateHist.add(cloneLastState());

        int other = 3 - getCurrentState().getDeclarer() - player;
        ret = getCurrentState().makeMove(other, move, views);
        if (ret != null) {
          return "error when adding RE move " + ret;
        }

        moveHist.add(new Move(other, "RE"));
      }
      
      return null; // OK
    }

    // non-RE move

    SimpleState st = getCurrentState();

    // Misc.msg("tomove === " + st.getToMove());
    
    int trickNum = st.getTrickNum();
    // Misc.msg("move= " + move);
    String ret = st.makeMove(player, move, views);
    int n = stateHist.size();
    
    if (ret != null) {
      stateHist.remove(n-1);
      return ret;
    }

    int tn = st.getTrickNum();
    
    if (tn != trickNum && tn >= 0 && tn < 10) {
      // new trick : bookmark location
      trickBookmarks[tn] = n-1;
    }
    
    // move OK, append to move list

    moveHist.add(new Move(player, move));

    // Have we hit an important part of the game? If so, bookmark it!

    int phase = getCurrentState().getPhase();
    
    if (phase == SimpleState.BID) {
      if (bookmarks[GI_BIDDINGSTART] == -1)
	bookmarks[GI_BIDDINGSTART] = n-1;
      
    } else if (phase == SimpleState.CARDPLAY) {
      if (bookmarks[GI_CARDPLAY] == -1)
	bookmarks[GI_CARDPLAY] = n-1;
    
    } else if (phase == SimpleState.SKAT_OR_HAND_DECL) {
      if (bookmarks[GI_SKAT_OR_HAND] == -1)
	bookmarks[GI_SKAT_OR_HAND] = n-1;
    }
    
    return null;
  }

  /** Strips all game info then returns the game in sgf format
   */
  public String toSgfClean(boolean singleLine, int view)
  {
    names[0] = "";
    names[1] = "";
    names[2] = "";
    ratings[0] = "";
    ratings[1] = "";
    ratings[2] = "";
    place = "";
    comment = "";
    seriesId = "";
    gameId = "";
    date = "";

    return toSgf(singleLine, view);
  }

  public String toSgf(boolean singleLine)
  {
    return toSgf(singleLine, getOwner());
  }
  
  /** Returns a world-view game in sgf format
   */
  public String toSgf(boolean singleLine, int view)
  {
    if (view < 0)
      Misc.err("toSgf view < 0");

    if (owner != SimpleState.WORLD_VIEW && view != owner)
      Misc.err("toSgf: owner != world && view != owner");
    
    StringBuffer sb = new StringBuffer();
    String delimiter = "";
    if (!singleLine) delimiter = "\n";
    
    // 2. Write general game-specific labels (GM[..], PC[...], Pn[...], etc)
    sb.append("(;" + delimiter);
    sb.append("GM[Skat]" + delimiter);
    sb.append("PC[" + place + "]" + delimiter);
    if (comment != null)
      sb.append("CO[" + comment + "]" + delimiter);
    if (seriesId != null)
        sb.append("SE[" + seriesId + "]" + delimiter);
    if (gameId != null)
        sb.append("ID[" + gameId + "]" + delimiter);
    sb.append("DT[" + date + "]" + delimiter);

    if (view != SimpleState.WORLD_VIEW) // !!! was owner != ...
      sb.append("OW[" + view + "]" + delimiter); // !!! was + owner
    
    sb.append("P0[" + names[0] + "]" + delimiter);
    sb.append("P1[" + names[1] + "]" + delimiter);   
    sb.append("P2[" + names[2] + "]" + delimiter);
    sb.append("R0[" + ratings[0] + "]" + delimiter);
    sb.append("R1[" + ratings[1] + "]" + delimiter);
    sb.append("R2[" + ratings[2] + "]" + delimiter);
    
    // 3. Write the moves with formatting based on the given state's phase.

    int trickCount = 0; // How far along we are in a particular trick

    SimpleGame rg = null; // replay game
    String[] views = new String[SimpleState.VIEW_NUM];

    if (getOwner() == SimpleState.WORLD_VIEW && view != SimpleState.WORLD_VIEW) {
      // replay world view game in view of specific player
      rg = new SimpleGame(SimpleState.WORLD_VIEW);
    }
      
    sb.append("MV[" + delimiter);
    int hl = moveHist.size();

    // create move sequence according to view
    
    for (int i = 0; i < hl; i++) {
      Move m = moveHist.get(i);
      String source;
      String action = m.action;

      if (rg != null) {
        // create move view
        String r = rg.makeMove(m.source, m.action, views);
        if (r != null)
          Misc.err("error filtering move " + m.source + " " + m.action + " (view=" + view + ") : " + r);
        action = views[view];
      }
      
      // Write the move
      if (m.source == SimpleState.WORLD_MOVE)
        source = "w";
      else
        source = ""+m.source;
      
      sb.append(source + " " + action);
         
      // Add some phase-specific pretty printing.

      if (singleLine)
        sb.append(" ");
      
      else if (stateHist.get(i).getPhase() == SimpleState.CARDPLAY) {

        // Cardplay: group the cards played in a particular trick together
        
        if (trickCount == 2) {
          trickCount = 0;
          sb.append("\n");

        } else {
          ++trickCount;
          sb.append(" ");
        }

      } else if (stateHist.get(i).getPhase() == SimpleState.DEAL) {

        sb.append("\n");
        
      } else if (stateHist.get(i+1).getPhase() == SimpleState.ANSWER || 
                 stateHist.get(i+1).getPhase() == SimpleState.BID ||
                 stateHist.get(i+1).getPhase() == SimpleState.GET_SKAT) {
        // Bidding: keep the bid/answer moves on a single line.
        sb.append(" ");

      } else
        sb.append("\n");
    }
    
    // Write endgame result.

    if (!singleLine && trickCount != 0) sb.append("\n");
    sb.append("]");
    sb.append(delimiter);
    sb.append("R[");
    GameResult gr = new GameResult();
    getCurrentState().gameResult(gr);
    sb.append(gr.toString());
    sb.append("] " + delimiter + ";)" + delimiter);
    return sb.toString();
  }
  
  
  /** Reads in game in sgf format from a reader
   * @return "" if OK, null if EOF reached, or errmsg otherwise
   */
  public String fromSgf(BufferedReader br)
  {
    return fromSgf(br, false);
  }


  /** read game string in between (; and ;)
      @return null if EOF, "" if OK, error message otherwise
  */
  static public String readGameString(BufferedReader br, StringBuffer sb)
  {
    // skip until (;

    int c = 0, last = 0;

    while (true) {
      last = c;
      try { c = br.read(); }
      catch (IOException e) { return "IO Exception"; }
      if (c < 0 || (c == ';' && last == '(')) break;
    } 

    if (c < 0) {
      return null; // eof
    }

    // collect all characters between (;  and ;)  , strip comments #...

    c = -1;
    boolean skip = false;
    while (true) {
      last = c;
      try { c = br.read(); }
      catch (IOException e) { return "IO Exception"; }
      if (c < 0) return "unexpected end of file";

      if (skip) {
	if (c == '\n') {
	  skip = false;
	  sb.append('\n');
	}
      } else {
	if (c == '#') skip = true;
	else {
	  if (c == ')' && last == ';') break;
	  sb.append((char)(c));
	}
      }
    }

    return "";
  }
  
  /**
   * @param fixResign if true fixes resign moves
   */
  public String fromSgf(BufferedReader br, boolean fixResign)
  {
    StringBuffer sb = new StringBuffer();
    String r = readGameString(br, sb);

    if (r == null) return null;
    if (!r.equals("")) return r; // error

    return fromSgf(sb, fixResign);
  }


  /**
   * @param fixResign if true fixes resign moves
   */
  public String fromSgf(StringBuffer sb, boolean fixResign)
  {
    reset();

    owner = SimpleState.WORLD_VIEW;

    String s = new String(sb);
    StringTokenizer st = new StringTokenizer(s, "[]", true);

    while (st.hasMoreTokens()) {

      String label=null, pleft=null, data=null, pright=null;

      if (st.hasMoreTokens()) label  = st.nextToken().trim();

      if (label.equals(";")) break;
      
      if (st.hasMoreTokens()) pleft  = st.nextToken().trim();
      if (st.hasMoreTokens()) data   = st.nextToken().trim();
      if (data.equals("]")) {
        data = ""; pright = "]";
      } else {
        if (st.hasMoreTokens()) pright = st.nextToken().trim();
      }
      
      if (pleft == null || !pleft.equals("[")) return label+ ": [ missing";
      if (data  == null) return label + ": data missing";
      if (pright == null || !pright.equals("]")) return label+ ": ] missing";

      if (label.equals("GM")) {

      } else if (label.equals("PC")) {
	place = data;
      } else if (label.equals("CO")) {
	comment = data;
      } else if (label.equals("SE")) {
	seriesId = data;
      } else if (label.equals("ID")) {
	gameId = data;
      } else if (label.equals("DT")) {
	date = data;
      } else if (label.equals("OW")) {

        try { owner = Integer.parseInt(data); }
        catch (Throwable t) { return label+": illegal viewer " + data; }

      } else if (label.equals("P0")) {
	names[0] = data;
      } else if (label.equals("P1")) {
	names[1] = data;	
      } else if (label.equals("P2")) {
	names[2] = data;	
      } else if (label.equals("R0")) {
	ratings[0] = data;		
      } else if (label.equals("R1")) {
	ratings[1] = data;			
      } else if (label.equals("R2")) {
	ratings[2] = data;
      } else if (label.equals("MV")) {

	// moves
	
	StringTokenizer t = new StringTokenizer(data, "\n ", false);

	boolean whoNext = true;
	int who = -1;
	
	while (t.hasMoreTokens()) {
	  String tok = t.nextToken();
          
	  // System.out.println("move: " + tok);

	  if (whoNext) {

	    if      (tok.equals("w")) who = SimpleState.WORLD_MOVE;
            else if (tok.equals("0")) who = SimpleState.FORE_HAND;
            else if (tok.equals("1")) who = SimpleState.MIDDLE_HAND;
            else if (tok.equals("2")) who = SimpleState.REAR_HAND;
	    else return "illegal player id: " + tok;

	  } else {
            String str = makeMove(who, tok, null, fixResign);
            if (str != null) return "move error: " + str;
	  }

	  whoNext = !whoNext;
	}

	if (!whoNext)
	  return "incomplete move";
	
      } else if (label.equals("R")) {

        // store result
        sgfResult = data;
        
      } else {
	return "unrecognized label " + label;
      }
    }      

    if (fixResign && isFinished()) {
      GameResult gr = new GameResult();
      getCurrentState().gameResult(gr);
      String r = gr.toString();
      String[] parts = r.split("\\ ");

      if (parts.length > 2) {

        if (!parts[2].equals("v:"+sgfResult.split("\\ ")[4])) {
          
          // different game result
          // happens mostly when first trick isn't completed
          // append "@" otherwise for filter to catch those games
          // mark games with "~" with a disagreement about the winner
          
          return "different results! new: [" + r + "] file: [ " + sgfResult + "] " +
            ((getCurrentState().getTricksWon(0) +
              getCurrentState().getTricksWon(1) +
              getCurrentState().getTricksWon(2) >= 1) ? "@" : "") +
            (!parts[1].equals(sgfResult.split("\\ ")[1]) ? "~" : "") +
            (getCurrentState().getResigned(0)+
             getCurrentState().getResigned(1)+
             getCurrentState().getResigned(2) == 0 ? "%" : "");
        }

      } else if (parts.length == 1) {

        if (!parts[0].equals("passed") || !sgfResult.equals("? ? ? ? ?")) {
          return "different results 2! new: [" + r + "] file: [ " + sgfResult + "] ";
        }
      }
    }
    
    return ""; // OK
  }

  /** @return true if game has ended */
  public boolean isFinished() { return getCurrentState().isFinished(); }

  /** @return player to move */
  public int getToMove() { return getCurrentState().getToMove(); }

  /** @return player to move */
  public String generateWorldMove(Random rng) {
    return getCurrentState().generateWorldMove(rng);
  }

  /** @return declarer or -1 if there is none */
  public int getDeclarer() {
    if (stateHist.isEmpty()) return -1;
    return getCurrentState().getDeclarer();
  }

  /** @return a player's initial hand, prior to bidding */
  public int getHandInitial(int player) {
    return stateHist.get(bookmarks[GI_BIDDINGSTART]).getHand(player);
  }

  /** @return hand of player prior to trick */
  public int getHandPriorToTrick(int player, int trick) {
    if (trickBookmarks[trick] < 0) return 0;    
    return stateHist.get(trickBookmarks[trick]).getHand(player);
  }

  /** @return state prior to trick */
  public SimpleState getStatePriorToTrick(int trick) {
    if (trickBookmarks[trick] < 0) return null;    
    return stateHist.get(trickBookmarks[trick]);
  }
  
  /** @return hand of player prior to trick */
  public int getPlayedCardsPriorToTrick(int player, int trick) {
    if (trickBookmarks[trick] < 0) return 0;
    return stateHist.get(trickBookmarks[trick]).getPlayedCards(player);
  }

  /** @return The state in which the listed trick-card was just played
  */
  public SimpleState getStateByTrickCard(int trick, int card) {
    if (trickBookmarks[trick] < 0) return null;
    int index = trickBookmarks[trick];

    index = index + 1 + card;
    if (index >= stateHist.size()) return null;
    SimpleState s = stateHist.get(index);
    while (s.getTrickCardNum() != (card +1)) {
      index++;
      if (index >= stateHist.size()) return null;
      s = stateHist.get(index);
    }

    return stateHist.get(index);
  }

  /** @return The state in which the listed trick-card is played;
      trick:0..9 card:0..2
  */
  public int getTrickCardIndex(int trick, int card)
  {
    assert card >= 0 && card < 3;
    int index = trickBookmarks[trick];
    // Misc.msg("T: " + trick + " " + card + " " + index);
    if (index < 0) return -1;

    while (true) {

      if (index >= stateHist.size()) return -1;
      
      SimpleState s = stateHist.get(index);
      assert s != null;

      int tcn = s.getTrickCardNum();
      
      if (tcn == card || (card == 0 && tcn == 3)) // tcn=0 and 3 match card=0
        return index;
      
      index++;
    }
  }

  /** @return The player who played card in listed trick. -1 if none.
  */
  public int getToMoveByTrickCard(int trick, int card) {
    if (trickBookmarks[trick] < 0) return -1;
    int index = trickBookmarks[trick];
    index = index + card;
    if (index >= stateHist.size()) return -1;
    return stateHist.get(index).getToMove();
  }

  /** @return bookmark for trick i */
  public int getTrickBookmark(int i)
  {
    return trickBookmarks[i];
  }
  
  /** @param player index
      @param trick#
      @return the score of the player before a certain trick. */
  public int getScorePriorToTrick(int player, int trick) 
  {
    // Verify that cardplay has even been recorded in this SimpleGame
    if (bookmarks[GI_CARDPLAY] == -1) 
      Misc.err("Can't get trick - no cardplay recorded!");
  
    if (player < 0 || player >= 3)
      Misc.err("player out of bounds");
  
    if (trick < 0 || trick > 10) 
      Misc.err("trick out of bounds");
  
    // Find out what move the trick took place at
    int move = bookmarks[GI_CARDPLAY];
    move += trick * 3;
  
    return stateHist.get(move).getTrickPoints(player);
  }

  /**
     computes hand values from bids

     types:
     0 hand value =  value
     1 hand value >= value
    -1 hand value <= value
    
    @return true if bidding completed
  */
  public boolean getHandValues(int[] values, int[] types)
  {
    assert values.length == 3 && types.length == 3;
    int decl = getDeclarer();

    if (decl < 0) return false;

    SimpleState st = getCurrentState();
    for (int i=0; i < 3; i++) {
      values[i] = st.getMaxBid(i);
    }
    types[decl] = 1;

    // 0,1 always exact
    // if 2's bid is < 1's, then val2 <= val1
    if (values[2] < values[1]) {
      values[2] = values[1]; types[2] = -1;
    }

    return true;
  }
    
  /*  Returns the cards in trick # 'trick'
   *  null if trick was not played
   */
  public ArrayList<Card> getTrick(int trick) 
  {
    // Run of the mill checks
    if (bookmarks[GI_CARDPLAY] == -1)
      return null;
    // Misc.err("Can't get trick - no cardplay recorded!");
  
    if (trick < 0 || trick > 10) 
      Misc.err("trick out of bounds");
  
    // Find out what move the trick took place at
    int move = bookmarks[GI_CARDPLAY];
    move += trick * 3;
    if (move >= stateHist.size()) // was assert
      return null;

    if (move + 3 < stateHist.size()) {
      move = move + 3;
    } else {
      move = stateHist.size() - 1;
    }

    ArrayList<Card> theTrick = new ArrayList<Card>();
    
    stateHist.get(move).getCurrentTrick(theTrick);
    return theTrick;
  }

  /** @return The number of tricks that have been at least started so far in the game
  */
  public int numTricks() {
    
    if (bookmarks[GI_CARDPLAY] == -1) {
      return 0;
    }

    int movesSinceCardplay = stateHist.size() - bookmarks[GI_CARDPLAY] + 1;

    return (int) Math.ceil(movesSinceCardplay / 3);

  }

  /** @return a skat card (0/1) prior to the bidding
  */
  public Card getOriginalSkat(int index)
  {
    //0. Verify that cards have been dealt out.
    int loc = bookmarks[GI_BIDDINGSTART];
    if (loc == -1)
        Misc.err("Can't get original skat: cards haven't been dealt yet!");
  
    //1. Get the pre-bidding state from the vector.
    SimpleState s = stateHist.get(loc);
    if (index == 0) return s.getSkat0(); else return s.getSkat1();
  }

  /** @return venue where game was played */
  public String getPlace() {
    return place;
  }
 
  public void setPlace(String s) { place = s; }

  /** @return game declaration or null if it doesn't exist */
  public GameDeclaration getGameDeclaration() {
    if (stateHist.isEmpty()) return null;
    return getCurrentState().getGameDeclaration();
  }

  public String getSeriesId()  { return seriesId; }
  public void setSeriesId(String s) { seriesId = s; }
  
  public String getId()  { return gameId; }
  public void setId(String s) { gameId = s; }
  
  public String getDateTime()  { return date; }
  public void setDateTime(String s) { date = s; }
  
  public String getPlayerName(int player)
  {
    return names[player];
  }

  public void setPlayerName(int player, String s)
  {
    names[player] = s;
  }
  
  public String getRating(int player)
  {
    assert(player >= 0 && player < 3);
    return ratings[player];
  }
  public void setRating(int player, String r)
  {
    assert(player >= 0 && player < 3);
    ratings[player] = r;
  }


  public static void serialize(SimpleGame sg, StringBuffer sb)
  {
    sb.append("\nSimpleGame");    
    if (sg == null) { sb.append("Null "); return; }

    sb.append("\nMoveVector " + sg.moveHist.size() + "\n");

    for (Move m : sg.moveHist) {
      Move.serialize(m, sb);
    }

    sb.append("\nSimpleStateVector " + sg.stateHist.size());

    for (SimpleState s : sg.stateHist) {
      SimpleState.serialize(s, sb);
    }
  }


  /** return original skat cards if known or null otherwise */
  public Card[] getOriginalSkat()
  {
    if (stateHist.size() <= 1) return null;
    
    SimpleState st = getState(1);

    if (st.getSkat0().isKnown()) {
      return new Card[] { st.getSkat0(), st.getSkat1() };
    }

    for (int i=0; i < stateHist.size(); i++) {
      SimpleState s = stateHist.get(i);
      int phase = s.getPhase();
      if (phase == SimpleState.DISCARD_AND_DECL) {
        return new Card[] { s.getSkat0(), s.getSkat1() };
      } else if (phase == SimpleState.CARDPLAY)
        break;
    }

    return null;
  }

  /** replay game in pos' view, given a complete initial deal.
      @return null if OK, error message otherwise
  */
  public String replay(SimpleGame game, int pos, String deal)
  {
    reset(pos);

    //Misc.msg("owner= " + owner);
    
    SimpleGame rg = new SimpleGame(SimpleState.WORLD_VIEW);
    String[] views = new String[SimpleState.VIEW_NUM];

    for (int i=0 ; i < 3; i++) {
      setPlayerName(i, game.getPlayerName(i));
    }

    for (Move m : game.getMoveHist()) {

      String action;
      // replay move in world view creating move view for pos
      if (rg.getCurrentState().getPhase() == SimpleState.DEAL) {

        action = deal;

      } else if (rg.getCurrentState().getPhase() == SimpleState.GET_SKAT) {        

        action =
          rg.getCurrentState().getSkat0().toString() + "." +
          rg.getCurrentState().getSkat1().toString();

      } else if ((rg.getCurrentState().getPhase() == SimpleState.DISCARD_AND_DECL) &&
                 ((game.owner != SimpleState.WORLD_VIEW) ||
                  (game.owner != game.getCurrentState().getDeclarer())) ) {
        // only the world and the declarer know what the discard
        // actually was; otherwise, we just assume he discarded what
        // he picked up
        action = game.getCurrentState().getGameDeclaration().toString() + ".";
        String skatcards =
          rg.getCurrentState().getSkat0().toString() + "."
          + rg.getCurrentState().getSkat1().toString();

        action += skatcards;
      } else {
        action = m.action;
      }

      //Misc.msg("Action is: " + action);

      String res = rg.makeMove(m.source, action, views);
      if (res != null) {
        return "move: " + m.toString() + " " + res;
      }

      // replay move for pos

      // Misc.msg("ms= " + m.source + " " + views[pos] + " o=" + owner + " " + game.getOwner() + " " + getOwner() + " : " + pos);
      
      res = makeMove(m.source, views[pos], null);
      if (res != null) {
        return "player view move: " + m.toString() + " " + res;
      }

    } // end for

    return null;
  }

  /** replay game in pos's view
      @return null if OK, error message otherwise
   */
  public String replay(SimpleGame game, int pos)
  {
    owner = pos;
    reset();

    if (game.getOwner() != SimpleState.WORLD_VIEW && pos != game.getOwner())
      return "can't replay " + game.getOwner() + "-game in view of " + pos;
    
    SimpleGame rg = new SimpleGame(game.getOwner()); // was: SimpleState.WORLD_VIEW);
    String[] views = new String[SimpleState.VIEW_NUM];
    
    for (int i =0 ; i < 3; i++) {
      
      setPlayerName(i, game.getPlayerName(i));
    }

    for (Move m : game.getMoveHist()) {

      // Misc.msg("replay move : " + m.toString());

      // replay move in world view creating move view for pos
      String res = rg.makeMove(m.source, m.action, views);
      if (res != null) {
        return "move: " + m.toString() + " " + res;
      }

      // replay move in pos view
      res = makeMove(m.source, views[pos], null);
      if (res != null) {
        return "player view move: " + m.toString() + " " + res;
      }
    }

    return null;
  }

  // return copy of game
  public SimpleGame copy()
  {
    // replay game
    SimpleGame g = new SimpleGame(getOwner());

    // replay resets all data ...
    if (g.replay(this, getOwner()) != null)
      return null; // error

    g.place = place;
    g.comment = comment;
    g.seriesId = seriesId;
    g.gameId = gameId;
    g.date = date;
    g.owner = owner;
    for (int i=0; i < 3; i++) {
      g.names[i] = names[i];
      g.ratings[i] = ratings[i];
    }

    return g;
  }
  

  /** return XSkat deal representation */
  public String XSkatDeal()
  {
    if (moveHist.size() == 0) return ""; // no deal

    StringBuffer sb = new StringBuffer();

    sb.append("# The following block specifies the distribution\n");
    sb.append("# of cards for player 1, 2, 3 and Skat(0).\n");
    sb.append("# A 10  K  Q  J  9  8  7\n");

    int[] xRanks = { Card.RANK_ACE, Card.RANK_TEN,
                     Card.RANK_KING, Card.RANK_QUEEN, Card.RANK_JACK,
                     Card.RANK_NINE, Card.RANK_EIGHT, Card.RANK_SEVEN };

    SimpleState state = getState(1);
    
    for (int s=0; s < 4; s++) {
      for (int r=0; r < 8; r++) {
        // where is the card?
        int bit = 1 << (s*8 + xRanks[r]);
        int i;
        for (i=0; i < 3; i++) {
          if ((state.getHand(i) & bit) != 0) {
            sb.append("  " + (i+1));
            break;
          }
        }

        if (i >= 3) sb.append("  0"); // skat
      }
      sb.append("  # " + Card.suitNames[s] + "\n");
    }

    return sb.toString();
  }

  // Card ordering 0..7:  789TJQKA
  // xskat Card ordering: ATKQJ987
  static int[] xranks = { 7,6,5,1,4,3,2,0 };
  
  private int card2xskatCard(Card card)
  {
    int suit = card.getSuit();
    int rank = card.getRank();
    return (suit << 3) + xranks[rank];
  }
  
  private void xskatSort(Card[] cards)
  {
    assert cards.length == 2;

    if (card2xskatCard(cards[0]) > card2xskatCard(cards[1])) {
      Card c = cards[0];
      cards[0] = cards[1];
      cards[1] = c;
    }
  }

  
  /** return XSkat game representation */
  public String XSkatGame()
  {
    SimpleState cs = getCurrentState();
    if (moveHist.size() == 0) return ""; // no deal
    if (!isFinished() || cs.getDeclarer() < 0) return ""; // not finished or no declarer
    
    StringBuffer sb = new StringBuffer();
    int cc = 0; // card counter

    for (int i=0; i < moveHist.size(); i++) {
      // print 3 card moves in a row
      SimpleState s = stateHist.get(i);
      if (s.getPhase() == SimpleState.CARDPLAY) {
        sb.append(moveHist.get(i) + " ");
        cc++;
        if (cc >= 3) {
          cc = 0;
          sb.append("\n");
        }
      }
    }

    // write skat cards in order so that they match with xskat outputs
    Card[] ca = new Card[2];
    ca[0] = stateHist.get(1).getSkat0();
    ca[1] = stateHist.get(1).getSkat1();
    xskatSort(ca);
    sb.append("orig skat: " + ca[0] + " " + ca[1] + "\n");

    ca[0] = cs.getSkat0();
    ca[1] = cs.getSkat1();
    xskatSort(ca);
    sb.append("disc skat: " + ca[0] + " " + ca[1] + "\n");

    sb.append("max-bid: " + cs.getMaxBid() + "\n");
    sb.append("soloist: " + cs.getDeclarer() + "\n");
    sb.append("type:    " + cs.getGameDeclaration() + "\n");    
    
    return sb.toString();    
  }

  /** write game in binary for C++ interoperability */

  static GameResult gr = new GameResult();
  
  public void writeAsNumbers()
  {
    /*

      format:

      sint4 hands[3] (bitsets)
      sint2 maxbids[3] (maximum bids, 0: no bid)
      sint1 soloist (0..2)
      sint1 gametype (0..4, see GameDeclaration.java)
      sint1 handgame (0..1)
      sint1 schneider_announced (0..1)
      sint1 schwarz_announced (0..1)
      sint1 ouvert (0..1)
      sint1 discard[2] (0..31, card indexes, -1: undef)
      sint1 cards[30] (0..31, card indexes, -1: undef)
      sint1 resigned[3] (0..1)
      sint2 result (regular game score)
      sint1 cardpoints (for soloist, >= 0)
      sint1 tricks (for soloist) 

    */

    SimpleState s = getCurrentState();
    GameDeclaration gd = getGameDeclaration();
    int maxBid0 = s.getMaxBid(0); if (maxBid0 == 1) maxBid0 = 0;
    int maxBid1 = s.getMaxBid(1); if (maxBid1 == 1) maxBid1 = 0;
    int maxBid2 = s.getMaxBid(2); if (maxBid2 == 1) maxBid2 = 0;
    int decl = getDeclarer();

    int declCardPoints, declTricks, declScore;
    
    synchronized (gr) {
      s.gameResult(gr);
      declCardPoints = gr.declCardPoints;
      declTricks = gr.declTricks;
      declScore = gr.declValue;
    }

    if (decl < 0 || gr.left >= 0) return; // no declarer or player left
    
    System.out.print(String.
                     format("%11d %11d %11d %3d %3d %3d %d %d %d %d %d %d %2d %2d ",
                            getHandInitial(0), getHandInitial(1), getHandInitial(2),
                            maxBid0, maxBid1, maxBid2,
                            decl,
                            gd.type,
                            gd.hand ? 1 : 0,
                            gd.schneiderAnnounced ? 1 : 0,
                            gd.schwarzAnnounced ? 1 : 0,
                            gd.ouvert ? 1 : 0,
                            gd.hand ? -1 : s.getSkat0().getIndex(),
                            gd.hand ? -1 : s.getSkat1().getIndex()
                            )
                     );
    // played cards

    int n = 0;
    for (int i=0; i < moveHist.size(); i++) {
      SimpleState t = stateHist.get(i);
      if (t.getPhase() == SimpleState.CARDPLAY) {

        Card card = Card.fromString(moveHist.get(i).action);
        if (card == null) continue;
        
        System.out.print(String.format("%2d ", card.getIndex()));
        n++;
      }
    }

    // fill remaining move slots with -1
    for (; n < 30; n++) {
      System.out.print(String.format("%2d ", -1));
    }

    System.out.println(String.format("%d %d %d %4d %3d %2d ",
                                   s.getResigned(0), s.getResigned(1), s.getResigned(2),
                                   declScore, declCardPoints, declTricks));
  }
  
}
