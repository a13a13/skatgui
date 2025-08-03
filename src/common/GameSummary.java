/*
 * GameSummary.java
 *
 * (c) Michael Buro, licensed under GPLv3
 *
 */

package common;

import java.util.*;
import java.io.*;
import java.text.*;

// summarizes game for faster analysis

public class GameSummary
{
  public int hand0, hand1, hand2;
  public Card skat0, skat1;
  public int maxBid0, maxBid1, maxBid2;
  public int declarer;
  public int gameType;
  public boolean hand;
  public boolean ouvert;
  public Card disc0, disc1;
  public Card first0, first1, first2;
  public int declCardPoints;
  public int declTricks;
  public int declScore;

  private GameResult r = new GameResult(); // scratch space to avoid new
    
  public String toStringColor()
  {
    String s = String.
      format("%s %s %s %s%s  %2d %2d %2d  ",
             Hand.toStringColor(hand0, Card.SUIT_CLUBS), // original hands
             Hand.toStringColor(hand1, Card.SUIT_CLUBS),
             Hand.toStringColor(hand2, Card.SUIT_CLUBS), 
             skat0.toStringColor(),              // skat
             skat1.toStringColor(),
             maxBid0, maxBid1, maxBid2           // maximum bids
             );

    if (declarer >= 0) {

      return s + String.
        format("%d %s %d %d %s%s %s%s%s %3d %2d %+4d",
               declarer,                           // declarer
               GameDeclaration.typeToStringColor(gameType), hand?1:0, ouvert?1:0,     // type, hand, ouvert
               disc0.toStringColor(),              // discarded cards
               disc1.toStringColor(),
               first0.toStringColor(),             // first trick
               first1.toStringColor(),
               first2.toStringColor(), 
               declCardPoints,                     // declarer card points
               declTricks,                         // declarer tricks
               declScore                           // declarer score (>0: win)
               );

    } else 

      return s + String.format("* * * * ???? ?????? %3d %2d %+4d", 0, 0, 0);
  }

  public String toString()
  {
    if (skat0 == null || skat1 == null) {
      Misc.err("skat = null");
    }
    
    String s = String.
      format("%s %s %s %s%s  %2d %2d %2d  ",
             Hand.toString(hand0, false),          // original hands
             Hand.toString(hand1, false),
             Hand.toString(hand2, false), 
             skat0.toString(),                     // skat
             skat1.toString(),
             maxBid0, maxBid1, maxBid2             // maximum bids
             );

    if (declarer >= 0) {

      return s + String.
        format("%d %s %d %d %s%s %s%s%s %3d %2d %+4d",
               declarer,                           // declarer
               GameDeclaration.typeToChar(gameType), hand?1:0, ouvert?1:0, // type, hand, ouvert
               disc0.toString(),                   // discarded cards
               disc1.toString(),
               first0.toString(),                  // first trick
               first1.toString(),
               first2.toString(), 
               declCardPoints,                     // declarer card points
               declTricks,                         // declarer tricks
               declScore                           // declarer score (>0: win)
               );

    } else 

      // no declarer
      return s + String.format("* * * * ???? ?????? %3d %2d %+4d", 0, 0, 0);
  }

  // return == null iff ok
  public String fromString(String s)
  {
    // format
    // JD CKT S9 HAK9 DA87  JC CAQ9 SK HQ7 DKQT  JS C87 SQT87 HT8 D9  SAHJ  18  0  0  0 G 0 0 CTCK HJCJSJ  74  4  +72

    hand0 = hand1 = hand2 = 0;
    skat0 = skat1 = null;
    maxBid0 = maxBid1 = maxBid2 = 0;
    declarer = -1;
    gameType = -1;
    hand = ouvert = false;
    disc0 = disc1 = first0 = first1 = first2 = null;
    declCardPoints = declTricks = declScore = 0;
    
    String[] parts = Misc.split(s, "\\s+");
    if (parts.length != 28)
      return "illegal game summary string : " + parts.length + " parts " + s;

    //for (int i=0; i < parts.length; i++)
    //  Misc.msg("" + i + " " + parts[i]);
    
    hand0 = Hand.fromString(parts[0] + " " + parts[1] + " " + parts[2] + " " + parts[3] + " " + parts[4]);
    hand1 = Hand.fromString(parts[5] + " " + parts[6] + " " + parts[7] + " " + parts[8] + " " + parts[9]);
    hand2 = Hand.fromString(parts[10] + " " + parts[11] + " " + parts[12] + " " + parts[13] + " " + parts[14]);

    skat0 = Card.fromString(parts[15].substring(0,2));
    skat1 = Card.fromString(parts[15].substring(2,4));

    //Misc.msg(parts[15].substring(0,2) + "  " + parts[15].substring(2,4));

    if (skat0 == null || skat1 == null)
      return "skat cards corrupt : " + parts[15];

    maxBid0 = Integer.parseInt(parts[16]);
    maxBid1 = Integer.parseInt(parts[17]);
    maxBid2 = Integer.parseInt(parts[18]);    

    if (parts[19].equals("*"))
      return null;

    declarer = Integer.parseInt(parts[19]);

    gameType = GameDeclaration.typeFromChar(parts[20]);

    if (gameType == GameDeclaration.NO_GAME)
      return "illegal game type " + parts[20];

    hand   = parts[21].equals("1");
    ouvert = parts[22].equals("1");

    disc0 = Card.fromString(parts[23].substring(0,2));
    disc1 = Card.fromString(parts[23].substring(2,4));

    first0 = Card.fromString(parts[24].substring(0,2));
    first1 = Card.fromString(parts[24].substring(2,4));
    first2 = Card.fromString(parts[24].substring(4,6));

    if (first0 == null || first1 == null || first2 == null)
      return "first trick cards corrupt : " + parts[24];
    
    declCardPoints = Integer.parseInt(parts[25]);
    declTricks     = Integer.parseInt(parts[26]);

    if (parts[27].charAt(0) == '+') {
      parts[27] = parts[27].substring(1);
    }
    declScore = Integer.parseInt(parts[27]);

    return null;
  }

  public void fromGame(SimpleGame g)
  {
    SimpleState s = g.getCurrentState();

    hand0 = g.getHandInitial(0);
    hand1 = g.getHandInitial(1);
    hand2 = g.getHandInitial(2);      
    skat0 = g.getOriginalSkat(0);
    skat1 = g.getOriginalSkat(1);
    maxBid0 = s.getMaxBid(0); if (maxBid0 == 1) maxBid0 = 0;
    maxBid1 = s.getMaxBid(1); if (maxBid1 == 1) maxBid1 = 0;
    maxBid2 = s.getMaxBid(2); if (maxBid2 == 1) maxBid2 = 0;
    declarer = g.getDeclarer();
    gameType = g.getGameDeclaration().type;
    hand     = g.getGameDeclaration().hand;
    ouvert   = g.getGameDeclaration().ouvert;
    disc0    = s.getSkat0();
    disc1    = s.getSkat1();

    first0 = first1 = first2 = Card.unknownCard;
    
    if (declarer >= 0) {
      ArrayList<Card> trick = g.getTrick(0);
      if (trick.size() >= 1) first0 = trick.get(0);
      if (trick.size() >= 2) first1 = trick.get(1);
      if (trick.size() >= 3) first2 = trick.get(2);
    }
    
    synchronized (r) {
      s.gameResult(r);
      declCardPoints = r.declCardPoints;
      declTricks = r.declTricks;
      declScore = r.declValue;
    }
  }
}
