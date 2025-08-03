/*
 * Scoresheet.java
 *
 * (c) Michael Buro, licensed under GPLv3
 *
 */

package common;

import java.util.*;
import java.io.*;
import java.text.*;

public class ScoreSheet
{
  public int playerNum;
  public String[] names;  
  public Vector<Row> rows = new Vector<Row>(); // gameNum = size
  public Row winLossRow = new Row();
  public Row defWinsRow = new Row();
  public Row timeoutRow = new Row(); // negative ...
  public Row totalsRow  = new Row(); 
    
  // score sheet player entry (cumulative values)
  public class Cumulative
  {
    public int score;
    public int wins;
    public int losses;
    public int penalties;

    public void init()
    {
      score = wins = losses = penalties = 0;
    }
    
    // copy values from other object    
    public void copyOf(Cumulative c)
    {
      score = c.score;
      wins = c.wins;
      losses = c.losses;
      penalties = c.penalties;
    }
  }

  // game information in score sheet

  public class Row
  {
    public int declarer; // FH=0,MH=1,RH=2; -1: passed
    public int baseValue;
    public int matadors;
    public boolean hand;
    public boolean schneider, schneiderAnnounced;
    public boolean schwarz, schwarzAnnounced;
    public boolean open, overbid;
    public int score;
    public int p0, p1, p2; // penalties

    public int cumuPass; // not serialized
    public Cumulative[] cumulative = new Cumulative[4]; // not serialized

    public Row()
    {
      for (int i=0; i < 4; i++) {
        cumulative[i] = new Cumulative();
      }
    }
    
    public void init()
    {
      declarer = -1; // passed
      baseValue = matadors = 0;
      hand = schneider = schneiderAnnounced = 
        schwarz = schwarzAnnounced = open = overbid = false;
      score = p0 = p1 = p2 = 0;
      for (int i=0; i < 4; i++) {
        cumulative[i].init();
      }
      cumuPass = 0;
    }
    
    // copy values from other object
    public void copyOf(Row l)
    {
      declarer = l.declarer;
      baseValue = l.baseValue;
      matadors = l.matadors;
      hand = l.hand;
      schneider = l.schneider;
      schneiderAnnounced = l.schneiderAnnounced;
      schwarz = l.schwarz;
      schwarzAnnounced = l.schwarzAnnounced;
      open = l.open;
      overbid = l.overbid;
      score = l.score;
      p0 = l.p0;
      p1 = l.p1;
      p2 = l.p2;
      cumuPass = l.cumuPass;
      for (int i=0; i < 4; i++) {
        cumulative[i].copyOf(l.cumulative[i]);
      }
    }

    public String toString()
    {
      return String.format("R: %d %d %d %d%d%d%d%d%d%d %d %d %d %d",
                           declarer,
                           baseValue,
                           matadors,
                           Misc.intOf(hand),
                           Misc.intOf(schneider),
                           Misc.intOf(schneiderAnnounced),
                           Misc.intOf(schwarz),
                           Misc.intOf(schwarzAnnounced),
                           Misc.intOf(open),
                           Misc.intOf(overbid),
                           p0, p1, p2,
                           score);
    }

    public void load(SReader sr)
    {
      init();

      try {
        String t = sr.nextWord();
        if (t == null || !t.equals("R:")) throw new Exception("expected R:, but got " + t);
        declarer = Integer.parseInt(sr.nextWord());
        baseValue = Integer.parseInt(sr.nextWord());        
        matadors = Integer.parseInt(sr.nextWord());
        t = sr.nextWord();
        if (t == null || t.length() != 7) throw new Exception("expected mod. string, but got " + t);
        hand = t.charAt(0) == '1';
        schneider = t.charAt(1) == '1';
        schneiderAnnounced = t.charAt(2) == '1';
        schwarz = t.charAt(3) == '1';
        schwarzAnnounced = t.charAt(4) == '1';
        open = t.charAt(5) == '1';
        overbid = t.charAt(6) == '1';
        p0 = Integer.parseInt(sr.nextWord());
        p1 = Integer.parseInt(sr.nextWord());        
        p2 = Integer.parseInt(sr.nextWord());
        score = Integer.parseInt(sr.nextWord());
      }
      catch (Throwable e) { Misc.err(""+e); return; }
    }
    
    // adjust cumulative values according to game result
    public void adjustCumulative(int fhIndex, int playerNum)
    {
      if (declarer < 0) {
        // passed
        cumuPass++;
        return; 
      }

      Cumulative c = cumulative[(fhIndex+declarer) % playerNum];
      c.score += score;
      if (score > 0) {
        c.wins++;
      } else {
        c.losses++;
      }
    }

    public void fromGame(SimpleGame g)
    {
      init();
      if (!g.isFinished()) Misc.err("game not finished");

      GameDeclaration gd = g.getGameDeclaration();
      GameResult gr = new GameResult();
      g.getCurrentState().gameResult(gr);

      if (gd.type != GameDeclaration.NO_GAME) {

        hand = gd.hand;
        schneiderAnnounced = gd.schneiderAnnounced;
        schwarzAnnounced = gd.schwarzAnnounced;
        open = gd.ouvert;

        switch (gd.type) {
        case GameDeclaration.DIAMONDS_GAME: baseValue =  9; break;
        case GameDeclaration.HEARTS_GAME:   baseValue = 10; break;
        case GameDeclaration.SPADES_GAME:   baseValue = 11; break;
        case GameDeclaration.CLUBS_GAME:    baseValue = 12; break;
        case GameDeclaration.NULL_GAME:     baseValue = 23; break;
        case GameDeclaration.GRAND_GAME:    baseValue = 24; break;
        default: Misc.err("unknown game type");
        }
      }

      declarer = gr.declarer;
      matadors = gr.matadors;
      schneider = gr.schneider;
      schwarz = gr.schwarz;
      overbid = gr.overbid;
      score = gr.declValue;
      p0 = gr.penalty0;
      p1 = gr.penalty1;
      p2 = gr.penalty2;      
    }

  }

  public ScoreSheet(int n)
  {
    playerNum = n;
    names = new String[playerNum];
    for (int i=0; i < playerNum; i++) {
      names[i] = "?";
    }
  }
  
  public ScoreSheet(int n, String[] players)
  {
    playerNum = n;
    names = new String[playerNum];
    for (int i=0; i < playerNum; i++) {
      names[i] = players[i];
    }
  }
  
  // add copy of line and adjust cumulative scores
  public void addRowCopy(Row l)
  {
    Row newL = new Row();
    newL.copyOf(l);
    addRow(newL);
  }

  // add line and adjust cumulative scores  
  public void addRow(Row newL)
  {
    int prev = rows.size()-1;
    rows.add(newL);

    for (int i=0; i < 4; i++) {
      if (prev < 0) {
        // first line: empty
        newL.cumulative[i].init();
        newL.cumuPass = 0;
      } else {
        // otherwise: copy from last
        newL.cumulative[i].copyOf(rows.get(prev).cumulative[i]);
        newL.cumuPass = rows.get(prev).cumuPass;
      }
    }

    newL.adjustCumulative((prev+2) % playerNum, playerNum);

    // recompute totals

    Row lastRow = rows.get(rows.size()-1);

    int totalLosses = 0;
    for (int i=0; i < playerNum; i++) {
      totalLosses += lastRow.cumulative[i].losses;
    }
    
    for (int i=0; i < playerNum; i++) {
      // win/loss
      winLossRow.cumulative[i].score = (lastRow.cumulative[i].wins - lastRow.cumulative[i].losses)*Table.DECL_WIN_PTS;

      // def loss
      int l = totalLosses - lastRow.cumulative[i].losses;
      defWinsRow.cumulative[i].score = playerNum == 3 ? l*Table.DEF3_WIN_PTS : l*Table.DEF4_WIN_PTS;

      // penalties
      timeoutRow.cumulative[i].score = -Table.PENALTY_PTS*lastRow.cumulative[i].penalties;

      // penalties
      totalsRow.cumulative[i].score =
        lastRow.cumulative[i].score +
        winLossRow.cumulative[i].score +
        defWinsRow.cumulative[i].score +
        timeoutRow.cumulative[i].score;
    }
  }

  // add new line derived from a game
  public void addRowForGame(SimpleGame g)
  {
    Row l = new Row();
    l.fromGame(g);
    addRow(l);
  }

  public String toString()
  {
    StringBuffer sb = new StringBuffer();

    sb.append("" + playerNum);
    for (int i=0; i < playerNum; i++) {
      sb.append(" " + names[i]);
    }

    int n = rows.size();

    sb.append(" " + n);
    
    for (int i=0; i < n; i++) {
      sb.append(" ");
      sb.append(rows.get(i).toString());
    }
    return sb.toString();
  }

  public void load(SReader sr)
  {
    playerNum = Integer.parseInt(sr.nextWord());
    names = new String[playerNum];

    for (int i=0; i < playerNum; i++) {
      names[i] = sr.nextWord();
    }

    int n = Integer.parseInt(sr.nextWord()); // #rows
    
    rows = new Vector<Row>();

    for (int i=0; i < n; i++) {
      Row l = new Row();
      l.load(sr);
      addRow(l);
    }
  }

  public Row getRow(int i) { return rows.get(i); }

  public int size()
  {
    return rows.size();
  }
}
