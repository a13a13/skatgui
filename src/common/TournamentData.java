/*
 * Tournament data 
 *
 * (c) Michael Buro
 * licensed under GPLv3
 */

package common;

import java.io.*;
import java.util.*;

public class TournamentData
{
  class TRound {
    long startTime;
    long maxDuration;
    ArrayList<TTable> tables;
  }

  class TTable {
    String name;
    String[] players = new String[4];
    ArrayList<SimpleGame> games = new ArrayList<SimpleGame>();
  }

  // interface

  public TournamentData() {
    currentRound = -1; // not started yet
    state = STATE_WAIT_START;
    players = new ArrayList<String>();
  }
  
  public String toInfoString()
  {
    String s = name + " " + creator + " " + numRounds + " " +
      numBlocksPerRound + " " + numRandomRounds + " " +
      (only3tables ? "1" : "0") + " " +
      startDate + " " + secPerRound + " " + numPlayers + " " +
      state + " " + currentRound;

    // schedule
    for (int i=0; i < numRounds; i++) {
      s += " " + dayOffsets[i] + " " + startTimes[i];
    }

    return s;
  }

  // return null iff OK, error messager otherwise
  public String fromInfoString(SReader sr)
  {
    try {
      name = sr.nextWord();
      creator = sr.nextWord();
      numRounds = Integer.parseInt(sr.nextWord());

      if (numRounds <= 0 || numRounds > 100)
        return "rounds > 100 or < 0";
      
      numBlocksPerRound = Integer.parseInt(sr.nextWord());
      numRandomRounds = Integer.parseInt(sr.nextWord());
      only3tables = Integer.parseInt(sr.nextWord()) != 0;
      startDate = sr.nextWord();
      secPerRound = Integer.parseInt(sr.nextWord());
      numPlayers = Integer.parseInt(sr.nextWord());
      state = Integer.parseInt(sr.nextWord());
      currentRound = Integer.parseInt(sr.nextWord());
      rounds = new TRound[numRounds];
      dayOffsets = new int[numRounds];
      startTimes = new int[numRounds];

      for (int i=0; i < numRounds; i++) {
        dayOffsets[i] = Integer.parseInt(sr.nextWord());
        startTimes[i] = Integer.parseInt(sr.nextWord());
      }
    }
    catch (Throwable e) {
      return "corrupt";
    }

    // check for sanity

    return sane();
  }

  public String sane()
  {
    if (name == null || name.length() < 3 || name.length() > 8)
      return "name too short or too long";
    
    if (numBlocksPerRound < 1 || numBlocksPerRound > 24)
      return "numBlocksPerRound out of range";

    if (numRandomRounds < 0 || numRandomRounds > numRounds)
      return "numRandomRounds out of range";

    if (secPerRound < 1 || secPerRound > 360*60)
      return "minPerRound suspect";

    if (numPlayers < 0 || numPlayers > 1000)
      return "numPlayers suspect";

    if (state < 0 || state >= STATE_NUM)
      return "state corrupt";

    if (currentRound < -1 || currentRound >= numRounds)
      return "currentRound out of range";
    
    // check date

    int y,m,d;

    try {
      y = year();
      m = month();
      d = day();
    }
    catch (Throwable x) { return "date corrupt"; }

    if (y < 2010 || y > 2050 ||
        m < 0 || m >= 12 ||
        d < 0 || d > 31)
      return "date corrupt";
    
    // dayoffsets non decreasing starting with 0

    if (dayOffsets[0] != 0)
      return "dayOffsets need to start with 0";

    for (int i=1; i < numRounds; i++) {
      
      if (dayOffsets[i]*24*60*60 + startTimes[i] < dayOffsets[i-1]*24*60*60 + startTimes[i-1])
        return "start times decreasing";

      if (dayOffsets[i]*24*60*60 + startTimes[i] <
          dayOffsets[i-1]*24*60*60 + startTimes[i-1] + secPerRound)
        return "time periods not disjoint";
    }

    return null; // ok
  }

  public int year() throws Exception
  {
    String[] p = startDate.split("-");
    if (p.length != 3)
      throw new Exception("date corrupt");

    return Integer.parseInt(p[0]);
  }

  // 0..11 !
  public int month() throws Exception
  {
    String[] p = startDate.split("-");
    if (p.length != 3)
      throw new Exception("date corrupt");

    return Integer.parseInt(p[1])-1;
  }

  public int day() throws Exception
  {
    String[] p = startDate.split("-");
    if (p.length != 3)
      throw new Exception("date corrupt");

    return Integer.parseInt(p[2]);
  }

  public int seats()
  {
    return only3tables ? 3 : 4;
  }    
  
  public int gamesPerRound()
  {
    return seats() * numBlocksPerRound;
  }

  public boolean isPlayer(String user)
  {
    for (String player : players) {
      if (player.equals(user))
        return true;
    }
    return false;
  }    

  // return UCT millis for next event
  public long nextTimeInMillis()
  {
    Calendar then = Calendar.getInstance();
    try {
      then.set(year(), month(), day(), 0, 0, 0); // 0:00 that day (same timezone)
    }
    catch (Exception e) { Misc.err("start date corrupt " + startDate); }

    long thenMillis = then.getTimeInMillis(); // UTC
    
    if (state == STATE_WAIT_START) {
      thenMillis += startTimes[currentRound+1] * 1000; // seconds * 1000
      thenMillis += dayOffsets[currentRound+1] * 24 * 60 * 60 * 1000;
    } else if (state == STATE_WAIT_END) {
      // endTime = startTime + minPerRound (in minutes)
      thenMillis += (startTimes[currentRound]+secPerRound) * 1000; // seconds * 1000
      thenMillis += dayOffsets[currentRound] * 24 * 60 * 60 * 1000;
    } else
      Misc.err("illegal state in nextTimeInMillis");

    return thenMillis;
  }

  public void nextState()
  {
    if (state == STATE_FINISHED) return;
    
    if (state == STATE_WAIT_START) {

      state = STATE_WAIT_END;
      currentRound++;

    } else {

      if (currentRound < numRounds-1)
        state = STATE_WAIT_START;
      else
        state = STATE_FINISHED;
    }
  }
  
  // data
  public String name;
  public String creator;
  public String password;
  public String seed;
  public int numRounds;
  public int numBlocksPerRound;
  public int numRandomRounds; // after this, players are seated by score
  public boolean only3tables; // false: maximize number of 4 tables, 3 tables at bottom
  public String startDate; // format: yyyy-mm-dd
  public int secPerRound;  // seconds per round
  public int[] dayOffsets; // date = start date + offset[round]
  public int[] startTimes; // seconds after midnight[round]
  public int numPlayers;   // update when player is added
  public int state;        // see STATE_* below
  public int currentRound; // -1: not started yet

  // server side only
  public ArrayList<String> players;  // all players in tournament
  public TRound[] rounds;            // round data

  // client side only
  public boolean userJoined; // user joined tournament

  public final static int STATE_WAIT_START = 0;
  public final static int STATE_WAIT_END   = 1;
  public final static int STATE_FINISHED   = 2;
  public final static int STATE_NUM = 3; // adjust after adding state
}
