/*
 * $Id: SimpleState.java 10994 2012-01-10 23:13:06Z mburo $
 *
 * Skat state (holds enough information to compute successor state and score)
 *
 * (c) Michael Buro, Nathan Taylor, Jeff Long, Ryan Lagerquist
 * licensed under GPLv3
 */

package common;

import java.io.*;
import java.util.*;


public class SimpleState implements Serializable
{
  // constants rather than enums for C compatibility

  // to-move/game owner constants, don't change!
  public static final int VIEW_NUM    = 5;
  public static final int FORE_HAND   = 0; // don't change
  public static final int MIDDLE_HAND = 1; // don't change
  public static final int REAR_HAND   = 2; // don't change
  public static final int PUBLIC_VIEW = 3; // don't change
  public static final int WORLD_VIEW  = 4; // same
  public static final int WORLD_MOVE  = 4; // same

  // game phase
  public static final int DEAL = 0;
  public static final int BID  = 1;
  public static final int ANSWER = 2;
  public static final int SKAT_OR_HAND_DECL = 3;
  public static final int GET_SKAT = 4;
  public static final int DISCARD_AND_DECL = 5;
  public static final int CARDPLAY = 6;
  public static final int FINISHED = 7;

  // void bit vector (bits 0..3 suits, 4 jacks)
  public static final int JACK_VOID_BIT = (1<<4);

  public static final int DEF3WIN = 40; 
  
  public static final int[] BIDS = new int[] {
    18, 20, 22, 23, 24, 27, 30, 33, 35, 36, 40, 44, 45, 46, 48, 50, 54, 55, 59, 60,
    63, 66, 70, 72, 77, 80, 81, 84, 88, 90, 96, 99, 100, 108, 110, 117, 120, 121,
    126, 130, 132, 135, 140, 143, 144, 150, 153, 154, 156, 160, 162, 165, 168,
    170, 171, 176, 180, 187, 189, 190, 192, 198, 204, 216, 240, 264
  };

  private String i18n(String s) {
    return "_" + s;
  }

  /** @return the index of a given bid in the BIDS array */
  public static int getBidIndex(int bid) {

    // 0 and 1 should probably be differentiated...doesn't 0 mean "has not bid yet"?
    if (bid == 0 || bid == 1) {
      return -1; // a 'pass' bid
    }

    for (int i = 0; i < BIDS.length; i++) {
      if (BIDS[i] == bid) {
        return i;
      }
    }

    Misc.err("Warning - bid had no index: " + bid);
    return -2;
  }

  static public String[][][] suitCardStrings;
  static int[][] voidMasks;

  // IMPORTANT: if you change members here, you need to update
  // serialize() and copy() below and also the C++ wrapper in src/c++

  class PlayerInfo
  {
    long p1,p2,p3,p4,p5,p6,p7,p8; // mitigates false sharing!    
    int  hand;
    int  playedCards;
    byte tricksWon;
    byte trickPoints;
    byte resigned;
    byte voids;       // flags: 1<<suit, 1<<5 = jacks (in trump games)
    int  maxBid;      // 0: no bid yet, 1: passed right away
    // int sloughs; // cards that the player threw off when they were losing the trick
    // store this somewhere else. SimpleState only contains data necessary for making moves
    // ALSO: if you add things here you need to change serialize() AND deserialize() in
    // Wrapper.h! Otherwise, C++ clients BREAK!!!
    long q1,q2,q3,q4,q5,q6,q7,q8; // mitigates false sharing!
  }

  // all data members -------------------
  
  long q1,q2,q3,q4,q5,q6,q7,q8; // mitigates false sharing!  
  private byte view; // view on game, see VIEW_* above
  private byte phase, prevPhase; // prev for scoring
  private byte declarer; // >=0 just means bidding is over
  // (decl.type != NO_GAME indicates that a game was announced)
  private int maxBid;
  private byte asked, toMove, bidder, trickWinner;
  private byte left, timeout; // >= 0: player left or exceeded time
  private Card winningCard;
  private GameDeclaration decl;
  private int declarerHandBeforeCardplay; // needed in declResult
  private PlayerInfo[] pinfos; // information about individual players
  private Card skat0, skat1;  // original skat or discarded cards
  private Card trick0, trick1, trick2;  // cards in trick
  private byte trickCardNum; // number of cards in trick
  private boolean discardHalfPhase;

  long p1,p2,p3,p4,p5,p6,p7,p8; // mitigates false sharing!

  // ---------------------------------------
  
  public static boolean isPlayer(int v) { return v >= FORE_HAND && v <= REAR_HAND; }
  
  // serialize entire object (for C++ backend)
  static public void serialize(SimpleState x, StringBuffer sb)
  {
    sb.append("\nSimpleState");
    if (x == null) { sb.append("Null "); return; }

    sb.append(" " + x.view + " ");
    sb.append(x.phase + " ");                   
    sb.append(x.prevPhase + " ");                 
    sb.append(x.declarer + " ");                  
    sb.append(x.maxBid + " ");
    // is needed for bidding
    sb.append(x.nextBid() + " ");
    //
    sb.append(x.asked + " ");                     
    sb.append(x.toMove + " ");                    
    sb.append(x.bidder + " ");                    
    sb.append(x.trickWinner + " ");
    sb.append(x.left + " ");
    sb.append(x.timeout + " ");
    Card.serialize(x.winningCard, sb);
    GameDeclaration.serialize(x.decl, sb);
    sb.append(x.declarerHandBeforeCardplay + " ");

    for (int i=0; i < 3; ++i) {
      PlayerInfo pi = x.pinfos[i];
      sb.append(pi.tricksWon + " ");
      sb.append(pi.trickPoints + " ");
      sb.append(pi.maxBid + " ");
      sb.append(pi.resigned + " ");
      sb.append(pi.hand + " ");
      sb.append(pi.playedCards + " ");
      sb.append(pi.voids + " ");
    }

    Card.serialize(x.skat0, sb);
    Card.serialize(x.skat1, sb);
    
    sb.append(x.trickCardNum + " ");

    if (x.trickCardNum >= 1) {
      Card.serialize(x.trick0, sb);
      if (x.trickCardNum >= 2) {      
        Card.serialize(x.trick1, sb);
        if (x.trickCardNum >= 3) {
          Card.serialize(x.trick2, sb);
        }
      }
    }
  }

  // copy s to this
  public void inPlaceCopy(SimpleState s)
  {
    view        = s.view;
    phase       = s.phase;
    prevPhase   = s.prevPhase; 
    declarer    = s.declarer;
    maxBid      = s.maxBid;
    asked       = s.asked;
    toMove      = s.toMove;
    bidder      = s.bidder;
    trickWinner = s.trickWinner;
    winningCard = s.winningCard;
    decl.copy2(s.decl);
    declarerHandBeforeCardplay = s.declarerHandBeforeCardplay;
    timeout     = s.timeout;
    left        = s.left;
    
    for (int i=0; i < 3; ++i) {
      PlayerInfo xpi = pinfos[i];
      PlayerInfo spi = s.pinfos[i];
      
      xpi.tricksWon   = spi.tricksWon;
      xpi.trickPoints = spi.trickPoints;
      xpi.maxBid      = spi.maxBid;
      xpi.resigned    = spi.resigned;
      xpi.hand        = spi.hand;
      xpi.playedCards = spi.playedCards;
      xpi.voids       = spi.voids;  
    }

    skat0 = s.skat0;
    skat1 = s.skat1;

    trickCardNum = s.trickCardNum;
    trick0 = s.trick0;
    trick1 = s.trick1;
    trick2 = s.trick2;   
    discardHalfPhase = s.discardHalfPhase; 

  }
  
  // copy s to this, test parallelization
  public void copyTest(SimpleState s)
  {
    if (false) {
      for (int i=0; i < 10; i++) {
        p1++;
      }
    }

    view = s.view;
    if (true) return;
    
    
    view        = s.view;

    //phase       = s.phase;
    //prevPhase   = s.prevPhase; 
    //declarer    = s.declarer;
    //    maxBid      = s.maxBid;
    //    asked       = s.asked;
    //    toMove      = s.toMove;
    //    bidder      = s.bidder;
    //    trickWinner = s.trickWinner;
    //    winningCard = s.winningCard;
    // decl.copy2(s.decl);
    // declarerHandBeforeCardplay = s.declarerHandBeforeCardplay;
    // timeout     = s.timeout;
    // left        = s.left;

    if (false) {
      for (int i=0; i < 3; ++i) {
        PlayerInfo xpi = pinfos[i];
        PlayerInfo spi = s.pinfos[i];
      
        xpi.tricksWon   = spi.tricksWon;
        xpi.trickPoints = spi.trickPoints;
        xpi.maxBid      = spi.maxBid;
        xpi.resigned    = spi.resigned;
        xpi.hand        = spi.hand;
        xpi.playedCards = spi.playedCards;
        xpi.voids       = spi.voids; 
      }
    }
    //skat0 = s.skat0;
    //skat1 = s.skat1;

    //trickCardNum = s.trickCardNum;
    //trick0 = s.trick0;
    //trick1 = s.trick1;
    //trick2 = s.trick2;   
    //discardHalfPhase = s.discardHalfPhase; 
  }
  
  
  static public SimpleState createCopy(SimpleState s)
  {
    if (s == null) return null;
    
    SimpleState x = new SimpleState(s.view);
    x.inPlaceCopy(s);
    return x;
  }


  // compare with s, return !=null if not equal
  public String equals(SimpleState s)
  {
    if (view != s.view) return "view";
    if (phase != s.phase) return "phase";
    if (prevPhase != s.prevPhase) return "prevPhase";
    if (declarer != s.declarer) return "declarer";
    if (maxBid != s.maxBid) return "maxBid";
    if (asked != s.asked) return "asked";
    if (toMove != s.toMove) return "toMove";
    if (bidder != s.bidder) return "bidder";
    if (trickWinner != s.trickWinner) return "trickWinner";
    if (!decl.equal(s.decl)) return "decl";
    if (declarerHandBeforeCardplay != s.declarerHandBeforeCardplay) return "declarerHandBeforeCardplay";
    if (timeout != s.timeout) return "timeout";
    if (left != s.left) return "";
    
    for (int i=0; i < 3; ++i) {
      PlayerInfo xpi = pinfos[i];
      PlayerInfo spi = s.pinfos[i];
      
      if (xpi.tricksWon != spi.tricksWon) return "trickswon " + i;
      if (xpi.trickPoints != spi.trickPoints) return "tricksPoints " + i;
      if (xpi.maxBid      != spi.maxBid) return "maxBid " + i;
      if (xpi.resigned    != spi.resigned) return "resigned " + i;
      if (xpi.hand        != spi.hand) return "hand " + i;
      if (xpi.playedCards != spi.playedCards) return "playedCards " + i;
      if (xpi.voids       != spi.voids) return "voids " + i;
    }

    if (skat0 != s.skat0) return "skat0";
    if (skat1 != s.skat1) return "skat1";

    if (trickCardNum != s.trickCardNum) return "trickCardNum";
    if (trick0 != s.trick0) return "trick0";
    if (trick1 != s.trick1) return "trick1";
    if (trick2 != s.trick2) return "trick2";
    
    if (discardHalfPhase != s.discardHalfPhase) return "HALF";
    return null;
  }

  /** Initializes a "dummy" simplestate, as we would expect to see at
   *  the very start of a game.
   *  @parm view: state view 
   */
  
  public SimpleState(int view)
  {
    decl = new GameDeclaration();
    phase = DEAL;
    this.view = (byte)view;
    toMove = WORLD_VIEW;
    bidder = MIDDLE_HAND;
    asked = FORE_HAND;
    // maxBid = 0;     // implicit = 0 (< 18 indicates no bid yet)
    declarer = left = timeout = -1; // no player

    pinfos = new PlayerInfo[3];
    
    for (int i=0; i < 3; ++i) {
      pinfos[i] = new PlayerInfo();
    }
  
    skat0 = skat1 = Card.newCard(-1, -1);
    discardHalfPhase = false;
  }
  
  //Accessor/mutators.
  public int getPhase() { return phase; }  
  public int getDeclarer() { return declarer; }
  public boolean getDiscardHalfPhase() { return discardHalfPhase; }
  public int getResigned(int player) { return pinfos[player].resigned; }
  public int getVoids(int player) { return pinfos[player].voids; }
  public int getLeft() { return left; }
  public int getTimeOut() { return timeout; }  
  public int getMaxBid() { return maxBid; }
  public int getMaxBid(int player) { return pinfos[player].maxBid; }
  public int getBidder() { return bidder; }
  public int getAsked() { return asked; }  
  public int getTricksWon(int player) { return pinfos[player].tricksWon; }
  public int getTrickPoints(int player) { return pinfos[player].trickPoints; }

  /** Returns the intial deal of this state as a string.
      Fails for non-World-View, since the deal is unknown.
  */
  public String getInitialDeal() {
    int[] origHands = new int[3];
    String result = "";
    ArrayList<Card> list;
    for (int i = 0; i < 3; i++) {
      origHands[i] = 0;
      origHands[i] = pinfos[i].playedCards | pinfos[i].hand;
      list = Hand.toCardList(origHands[i]);
      result += Card.cardListToString(list) + ".";
    }
    list = Hand.toCardList(getSkat());
    result += Card.cardListToString(list);

    return result;
  }

  // number of points in current trick
  public int getPointsInTrick() {
    int sum = 0;
    if (trickCardNum > 0) {
      sum += trick0.value();
      if (trickCardNum > 1) {
	sum += trick1.value();
	if (trickCardNum > 2) {
	  sum += trick2.value();
	}
      }
    }
    return sum;
  }
  
  // return points for party (1 decl, 0 def)
  public int getPartyPoints(int party) {
    assert party == 0 || party == 1 : "party out of range";
    if (party == 1) {
      int p = pinfos[declarer].trickPoints;
      if (skat0.isKnown()) p += skat0.value(); // imperfect information ...
      if (skat1.isKnown()) p += skat1.value();
      return p;
    }

    int pts = 0;
    if (0 != declarer) pts += pinfos[0].trickPoints;
    if (1 != declarer) pts += pinfos[1].trickPoints;
    if (2 != declarer) pts += pinfos[2].trickPoints;
    return pts;
  }

  // return point differential
  public static int diffFromDeclPts(int declPts)
  {
    return declPts - (120-declPts);
  }
  
  // return total points so far
  public int getTotalPoints() {
    return pinfos[0].trickPoints + pinfos[1].trickPoints + pinfos[2].trickPoints +
      skat0.value() + skat1.value();
  }
  
  public GameDeclaration getGameDeclaration() { return decl; }
  public int getDeclarerHandBeforeCardplay() { return declarerHandBeforeCardplay; }
  public int getTrickCardNum() { return trickCardNum; }           // Indicates number of cards in current trick.
  public void setTrickCardNum(int n) { trickCardNum = (byte)n; } // careful! can corrupt state
  public void setTrickPoints(int player, int n) { pinfos[player].trickPoints = (byte)n; }
  public void setTricksWon(int player, int n) { pinfos[player].tricksWon = (byte)n; }
  public void setPlayedCards(int player, int n) { pinfos[player].playedCards = n; }
  public void setDeclarer(int n) { declarer = (byte)n; }

  public Card getTrickCard0() {
    assert trickCardNum > 0 && trickCardNum < 3 : "trickcardnum";
    return trick0;
  }
  public Card getTrickCard0NoCheck() { return trick0; }
  public void setTrickCard0(Card c) { trick0 = c; } // careful! can corrupt state

  public Card getTrickCard1() {
    assert trickCardNum > 1 && trickCardNum < 3 : "trickcardnum";
    return trick1;
  }
  public Card getTrickCard2() {
    assert trickCardNum > 2 : "trickcardnum";
    return trick2;
  }
  public Card getTrickCard(int i) {
    assert trickCardNum > i : "trickcardnum";
    if (i == 0) return trick0;
    if (i == 1) return trick1;
    return trick2;
  }


  public int getTrickWinner() { return trickWinner; }
  public Card getWinningCard() { return winningCard; } 
  
  public void setView(int view_) {
    view = (byte) view_;
  }

  /** @return true if the given player is void in the given suit (use 4 for jacks) */
  public boolean isVoid(int player, int suit) {
    if (suit < 0 || suit >= 5) Misc.err("suit out of range " + suit);
    return ( (pinfos[player].voids & (1<<suit)) != 0); 
  }
    
  public void getCurrentTrick(ArrayList<Card> al)
  {
    al.clear();
      
    if (trickCardNum >= 1) {
      al.add(trick0);
      if (trickCardNum >= 2) {      
        al.add(trick1);
        if (trickCardNum >= 3) {
          al.add(trick2);
        }
      }
    }
  }
  
  public int getTrickNum() {
    if (phase < CARDPLAY) return -1; // changed from != to < : hope this doesn't break anything - Jeff
    return pinfos[0].tricksWon + pinfos[1].tricksWon + pinfos[2].tricksWon;
  }
  
  public void setToMove(int i) { toMove = (byte)i; }
  public int  getToMove() { return toMove; }
  public boolean isViewerToMove() { return toMove == view; }
  public int getView() { return view; }

  /** @return next highest bid or -1 if none exists */
  public int nextBid() {
    for (int i=0; i < BIDS.length; i++) {
      if (BIDS[i] > maxBid)
        return BIDS[i];
    }
    return -1; // no higher bid
  }

  // find bid higher than bid
  public int nextBid(int bid) {
    for (int i = 0; i < BIDS.length; i++) {
      if (BIDS[i] > bid) {
        return BIDS[i];
      }
    }
    return -1;
  }
  
  public int nextBidInc(int numIncrements) {  
    /* If numIncrements > 0, this means that the bidder increased his
       bid beyond the next legal bid (the next legal bid being the
       level at which numIncrements = 0).  Therefore, we want to step
       through the BIDS array and find the index of the next legal
       bid, which will be the first index with an element greater than
       maxBid.  Then the value at the index which is numIncrements
       larger than this will become the next maxBid. - Ryan */
    
    for (int index = 0; index < BIDS.length; index++) {
      if (BIDS[index] > maxBid) {
        if (index + numIncrements >= BIDS.length)
          return -1;
        
        return BIDS[index + numIncrements];
      }
    }
    
    return -1;
  }

  /** @return legal bid given any integer between 18 and 264 inclusive */
  public static int getLegalBid(int testBid) {
    for (int index = 0; index < BIDS.length; index++) {
      if (BIDS[index] == testBid)
        return testBid;

      if (BIDS[index] > testBid)
        return BIDS[index];
    }

    return -1;
  }

  public static int nextLegalBid(int initialBid) {
    for (int index = 0; index < BIDS.length; index++) {
      if (BIDS[index] > initialBid)
        return BIDS[index];
    }

    return -1;
  }

  public static int prevLegalBid(int initialBid) {
    for (int index = BIDS.length - 1; index >= 0; index--) {
      if (BIDS[index] < initialBid)
        return BIDS[index];
    }

    return -1;
  }

  public static boolean isLegalBid(int testBid) {
    for (int index = 0; index < BIDS.length; index++) {
      if (BIDS[index] == testBid)
        return true;
    }

    return false;
  }

  /** @return true if highBid is one legal bidding level up from lowBid
      or if highBid is 18 and lowBid is a pass */
  public static boolean areBidsAdjacent(int lowBid, int highBid) {
    if (highBid == 18) {
      if (lowBid < 18)
        return true;

      return false;
    }

    if (lowBid < 18) // highBid is not 18
      return false;
    if (lowBid == 264)
      return false;

    for (int index = 0; index < BIDS.length; index++) {
      if (BIDS[index] == lowBid) {
        if (BIDS[index + 1] == highBid)
          return true;

        return false;
      }
    }

    return false;
  }
  
  public void setPhase(int p) { this.phase = (byte)p; }
  
  /** @return the hand of the declarer, or null if it hasn't been decided yet. */
  public int getDeclarerHand()
  {
    return pinfos[declarer].hand;
  }
  
  /** @return the hand of the passed player, or null if we were given a bad ID. */
  public int getHand(int player)
  {
    return pinfos[player].hand;
  }

  public void setHand(int player, int hand)
  {
    pinfos[player].hand = hand;
  }

  // return 1 iff declarer, 0 otherwise
  public int getParty(int player)
  {
    if (declarer == player) return 1;
    return 0;
  }
  
  /** @return the cards player played so far */
  public int getPlayedCards(int player)
  {
    return pinfos[player].playedCards;
  }

  /**   @return cards played by all players
   */
  public int getPlayedCards() {
    int answer = 0;
    for (int i = 0; i < 3; i++) {
      answer ^= pinfos[i].playedCards;
    }
    return answer;
  }

  /** @return true if player p knows the skat in this state, false otherwise */
  public boolean skatKnown(int p) {
    if ( (p == this.getDeclarer()) && (!this.getGameDeclaration().hand)) {
      return true;
    }
    else {
      return false;
    }
  }

  /** @return The skat as a 'hand' (integer) format
   *  if skat not known, return 0
   */
  public int getSkat() {
    int skat = 0;

    if (!skat0.isKnown()) return 0;
    
    skat = Hand.set(skat, skat0);
    skat = Hand.set(skat, skat1);
    return skat;
  }

  public void setSkat(int skat) {

    // Misc.msg("setSkat new!!!");
    
    Card[] c = new Card[2];
    int num = Hand.toCardArray(skat, c);
    setSkat0(c[0]);
    setSkat1(c[1]);

  }
  
  /** @return the skat card 0/1 */
  public Card getSkat0()
  {
    return skat0;
  }

  /** @return the skat card 0/1 */
  public Card getSkat1()
  {
    return skat1;
  }

  public void setSkat0(Card c) {
    skat0 = c;
  }

  public void setSkat1(Card c) {
    skat1 = c;
  }
  
  //   public int getSkatBits()
  //   {
  //     if (skat[0] == null || skat[1] == null)
  //       return 0;
  //     return
  //       ((1 << skat[0].getRank()) << (8 * skat[0].getSuit())) |
  //       ((1 << skat[1].getRank()) << (8 * skat[1].getSuit()));
  //   }
  
  //   public int getTrickBits()
  //   {
  //     int bits = 0;
  //     for (int i = 0; i < trick.size(); ++i)
  //       bits |= (1 << trick.get(i).getRank()) << (8 * trick.get(i).getSuit());
  //     return bits;
  //   }

  public boolean handKnown(int player)
  {
    // empty hand in game indicates no knowledge    
    if (pinfos[player].hand != 0) return true;
    return numCards(player) == 0;
  }

  /** return number of cards in hand (if empty, compute actual number) */
  public int numCards(int player)
  {
    int n = Hand.numCards(pinfos[player].hand);
    if (n > 0) return n;

    // empty hand could be empty or indicating that we don't know the
    // cards -> count them by considering played tricks
    
    return numCardsByTricks(player);
  }

  /** @return number of cards in player's hand computed by what has been played */
  public int numCardsByTricks(int player)
  {
    int n = 10 - (pinfos[0].tricksWon+pinfos[1].tricksWon+pinfos[2].tricksWon);

    // played in current trick -> subtract 1

    if (trickCardNum == 1) {
      if ((toMove + 2) % 3 == player) {
        return n-1;
      }
      
    } else if (trickCardNum == 2) {

      if (((toMove + 2) % 3 == player) || ((toMove + 1) % 3 == player)) {
        return n-1;
      }
    }

    return n;
  }
  
  /**
   *  compute world move
   *  @return null if world is not to move, move otherwise.
   */
  public String generateWorldMove(Random rgen)
  {
    if (phase == DEAL) {

      ArrayList<Card> cards = fullDeck();
      shuffle(cards, rgen);
      return Card.cardListToString(cards);
      
    } else if (phase == GET_SKAT) {

      return skat0.toString() + "." + skat1.toString();
      
    } else
      return null;
  }

  public static ArrayList<Card> fullDeck()
  {
    // Misc.msg("fullDeck new!!!");
    
    ArrayList<Card> cards = new ArrayList<Card>();

    for (int s=0; s < 4; s++) {
      for (int r=0; r < 8; r++) {      
        cards.add(Card.newCard(s, r));
      }
    }
    return cards;
  }

  // use higher order bits for generating number in 0..n-1
  static public int rndInt(Random rgen, int n)
  {
    assert n > 0;
    return (int)(rgen.nextDouble() * n);
  }
  
  // shuffle deck
  public static void shuffle(ArrayList<Card> deck, Random rgen)
  {
    for (int i=deck.size(); i > 0; i--) {
      Card t = deck.get(i-1);
      int r = rndInt(rgen, i);
      deck.set(i-1, deck.get(r));
      deck.set(r, t);
    }

    //testRnd(rgen);
  }

  // shuffle [start,size)
  public static void shuffle(ArrayList<Card> deck, Random rgen, int start)
  {
    //   for (int i=deck.size(); i > start; i--) {
    //     Card t = deck.get(i-1);
    //     int r = random(rgen, i-start)+start;
    //     deck.set(i-1, deck.get(r));
    //     deck.set(r, t);
    //   }
  }

  public void assignRandomCards(Random rng)
  {
    ArrayList<Card> deck = fullDeck();
    shuffle(deck, rng);

    int k = 0;

    for (int i=0; i < 3; i++) {
      int hand = 0;
      for (int j=0; j < 10; j++)
	hand = Hand.set(hand, deck.get(k++));
      pinfos[i].hand = hand;
    }

    skat0 = deck.get(k++);
    skat1 = deck.get(k++);

    assert k == 32;
  }
  
  // save state prior to (Card)
  
  public void saveState(SimpleStateUndo ui, String move)
  {
    ui.card = Card.fromString(move);

    ui.prevToMove = toMove;
    ui.toMoveHand = pinfos[toMove].hand;
    ui.toMovePlayedCards = pinfos[toMove].playedCards;
    ui.toMoveVoids = pinfos[toMove].voids;
    ui.trickCardNum = trickCardNum;
    ui.trickWinner = trickWinner;
    ui.winningCard = winningCard;
    ui.tricksWon0 = pinfos[0].tricksWon;
    ui.tricksWon1 = pinfos[1].tricksWon;
    ui.tricksWon2 = pinfos[2].tricksWon;    
    ui.trickPoints0 = pinfos[0].trickPoints;
    ui.trickPoints1 = pinfos[1].trickPoints;
    ui.trickPoints2 = pinfos[2].trickPoints;    
    ui.trick0 = trick0;
    ui.trick1 = trick1;
    ui.trick2 = trick2;    
  }

  // restore state after makeMove(Card)
  
  public void restoreState(SimpleStateUndo ui)
  {
    toMove = ui.prevToMove;
    pinfos[toMove].hand = ui.toMoveHand;
    pinfos[toMove].playedCards = ui.toMovePlayedCards;
    pinfos[toMove].voids = ui.toMoveVoids;
    trickCardNum = ui.trickCardNum;
    trickWinner = ui.trickWinner;
    winningCard = ui.winningCard;
    pinfos[0].tricksWon   = ui.tricksWon0;
    pinfos[1].tricksWon   = ui.tricksWon1;  
    pinfos[2].tricksWon   = ui.tricksWon2;    
    pinfos[0].trickPoints = ui.trickPoints0;
    pinfos[1].trickPoints = ui.trickPoints1;
    pinfos[2].trickPoints = ui.trickPoints2;   
    trick0 = ui.trick0;
    trick1 = ui.trick1;
    trick2 = ui.trick2;
    phase = CARDPLAY;
  }


  /** @return null if move could be made, or error message otherwise */
  public String makeCardMove(int player, Card card)
  {
    // We won't be able to tell if this move is legal or not if it's
    // not made by us or we are using server view

    if (player != toMove) return i18n("not_your_turn");
    
    if (view == WORLD_VIEW || view == player) {
      if (!Hand.has(pinfos[player].hand, card))
        return i18n("you_do_not_have_card") + " " + card.toString() + ".";
      
      if (!cardOK(card))
        return i18n("invalid_move_colon") + " " + player + " " + card.toString() + ".";
    }
    
    // move OK

    // update voids

    if (trickCardNum == 1 || trickCardNum == 2) {

      if (!followsSuit(card)) {
        // System.out.println("Card didn't follow suit!");
        Card fc = trick0;

        if (decl.type == GameDeclaration.GRAND_GAME) {
          // grand
          if (fc.getRank() == Card.RANK_JACK) {
            pinfos[player].voids |= JACK_VOID_BIT;
          } else {
            pinfos[player].voids |= 1 << fc.getSuit();
          }
        } else if (decl.type == GameDeclaration.NULL_GAME) {
          // null
          pinfos[player].voids |= 1 << fc.getSuit();
        } else {
          // suit is trump
          if (trump(fc)) {
            pinfos[player].voids |= 1 << decl.type;
          } else {
            pinfos[player].voids |= 1 << fc.getSuit();
          }
        }
      }
    }
      
    if (trickCardNum == 3) {
      trickCardNum = 0;
    }
      
    // play card

    if (trickCardNum == 0) {
      trick0 = card;
    } else if (trickCardNum == 1) {
      trick1 = card;
    } else
      trick2 = card;

    trickCardNum++;
      
    pinfos[player].hand = Hand.clear(pinfos[player].hand, card);
    pinfos[player].playedCards = Hand.set(pinfos[player].playedCards, card);

    if (trickCardNum == 1) {
      trickWinner = toMove;
      winningCard = card;
      toMove = (byte)((toMove + 1) % 3);
      return null;
    }

    if (highestCard(card)) {
      trickWinner = toMove;
      winningCard = card;
    }

    if (trickCardNum == 2) {
      toMove = (byte)((toMove + 1) % 3);
      return null;
    }

    // don't clear trick yet (for gui)
      
    pinfos[trickWinner].tricksWon++;
    pinfos[trickWinner].trickPoints += currentTrickPoints();

    

    // Misc.msg("add " + currentTrickPoints());
      
    toMove = trickWinner;

    // For when trickCardNum == 3
      
    // last trick?
    if (pinfos[0].tricksWon + pinfos[1].tricksWon + pinfos[2].tricksWon == 10) {
      phase = FINISHED;
      return null;
    }

    if (decl.type == GameDeclaration.NULL_GAME) {

      // null game: game ends when declarer gets first trick
      if (trickWinner == declarer)
        phase = FINISHED;

    } else {

      // non-null game 

      if (trickWinner != declarer) { // defenders won trick
        
        if (decl.schwarzAnnounced) {

          // schwarz announced: game ends when defenders get a trick
          phase = FINISHED;

        } else if (decl.schneiderAnnounced) {
            
          // schneider announced: game ends when defenders get > 30 points
          int defPts = 0;
          for (int i=0; i < 3; i++) {
            if (i != declarer) {
              defPts += pinfos[i].trickPoints;
              if (defPts > 30) {
                phase = FINISHED;
                break;
              }
            }
          }
        }
      }
    }
      
    return null;
  }
  
  /** Makes a move corresponding to view, if views != null generate 5 game views of move
   */
  public String makeMove(int player, String move, String[] views)
  {
    //     Misc.msg("MAKEMOVE: " + player + " " + move + " " + toMove + " " + phase);

    //     if (player < -1 || player > 2) {
    //       Misc.err("player out of bounds: " + player);
    //     }
    
    if (views != null) {
      // default view: public move

      if (views.length != 5)
        Misc.err("view.length != 5");

      // views:  player 0,1,2, public view, server view
      views[0] = views[1] = views[2] = views[3] = views[4] = move;
    }      
    
    if (phase == FINISHED)
      return i18n("game_finished");

    // resign?
    if (move.equals("RE")) {
      
      if (phase != CARDPLAY)
        return i18n("resignation_only_in_play");
      
      if (pinfos[player].resigned > 0)
        return i18n("you_already_resigned");
      
      pinfos[player].resigned = 1;
      
      if (player == declarer ||
          pinfos[0].resigned + pinfos[1].resigned + pinfos[2].resigned
          - pinfos[declarer].resigned >= 2)
        phase = FINISHED;

      // This is necessary when going through data to not get duplicate tricks!
      if (trickCardNum == 3) {
        trickCardNum = 0;
      }      

      return null;
    }

    // show cards?
    if (move.startsWith("SC")) {

      if (player != declarer)
	return i18n("only_declarer_can_show");

      if (phase != CARDPLAY)
        return i18n("show_allowed_only_in_play");
      
      String[] parts = move.split("\\.");      

      // we allow the declarer to show his cards
      
      if (view != WORLD_VIEW) {
        
        if (view != declarer) {
          
          // reveal declarer's cards if player != declarer
          
          pinfos[declarer].hand = 0;
          
          for (int i=1; i < parts.length; i++) {
            Card card = Card.fromString(parts[i]);
            if (card == null)
              return i18n("show_cards_error_colon") + " " + move;
            pinfos[declarer].hand = Hand.set(pinfos[declarer].hand, card);
          }
        }
        
      } else if (views != null) {
        
        // append declarer's cards to player and public view

        String h = "."+Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
        
        for (int i=0; i < 4; i++) { // includes PUBLIC_VIEW
          views[i] += h;
        }
      }
      
      return null;
    }

    if (player == WORLD_MOVE) {

      // timeout or leave move?

      timeout = -1;
      left = -1;
      if      (move.equals("TI.0"))
        timeout = 0;
      else if (move.equals("TI.1"))
        timeout = 1;
      else if (move.equals("TI.2"))
        timeout = 2;
      else if (move.equals("LE.0"))
        left = 0;
      else if (move.equals("LE.1"))
        left = 1;
      else if (move.equals("LE.2"))
        left = 2;

      if (timeout >= 0 || left >= 0) {
        // game ends
        phase = FINISHED;
        return null;
      }
    }

    if (toMove != player) {
      return i18n("not_your_turn");
    }

    if (phase == CARDPLAY ) {

      // card move sanity checks
      Card card = Card.fromString(move);
      if (card == null || card == Card.unknownCard)
        return i18n("error_parsing_card") + " " + move;

      return makeCardMove(player, card);
    }
    
    if (phase == DEAL) {
      
      // If we're in the dealing phase, we'll be passed a world move
      // which is a list of colon-delimited cards

      ArrayList<Card> cv = Card.cardListFromString(move);
      if (cv == null)
        return i18n("card_error_in_deal");

      if (cv.size() != 32)
        return i18n("not_32_cards");

      // assign cards to individual hands and skat

      pinfos[0].hand = pinfos[1].hand = pinfos[2].hand = 0;

      for (int i=0; i < 10; i++) {
        Card c;
        c = cv.get(i+ 0); if (view == WORLD_VIEW || view == 0) {
          pinfos[0].hand = Hand.set(pinfos[0].hand, c);
        }
        c = cv.get(i+10); if (view == WORLD_VIEW || view == 1) {
          pinfos[1].hand = Hand.set(pinfos[1].hand, c);
        }
        c = cv.get(i+20); if (view == WORLD_VIEW || view == 2) {
          pinfos[2].hand = Hand.set(pinfos[2].hand, c);          
        }
      }

      //    for (int i=0; i < 3; i++) {
      //	Misc.msg("Hand " + i + " " + hands[i].toStringColor(Card.SUIT_CLUBS) + " " + (view == i));
      // }
      
      skat0 = cv.get(30);
      skat1 = cv.get(31);      

      if (views != null) {
      
        // compute views (blanking out hidden information)
      
        ArrayList<Card> cards0 = new ArrayList<Card>();
        ArrayList<Card> cards1 = new ArrayList<Card>();
        ArrayList<Card> cards2 = new ArrayList<Card>();
      
        for (int i=0; i < 10; i++) {
          Card c;
          c = cv.get(i+ 0); cards0.add(c);
          c = cv.get(i+10); cards1.add(c);
          c = cv.get(i+20); cards2.add(c);
        }

        views[0] =
          Card.cardListToString(cards0) +
          Card.handSep +
          Card.unknownHand +
          Card.handSep +
          Card.unknownHand +
          Card.handSep +
          Card.unknownSkat;
      
        views[1] =
          Card.unknownHand +
          Card.handSep +
          Card.cardListToString(cards1) +
          Card.handSep +
          Card.unknownHand +
          Card.handSep +
          Card.unknownSkat;

        views[2] =
          Card.unknownHand +
          Card.handSep +
          Card.unknownHand +
          Card.handSep +
          Card.cardListToString(cards2) +
          Card.handSep +
          Card.unknownSkat;

        views[PUBLIC_VIEW] =
          Card.unknownHand +
          Card.handSep +
          Card.unknownHand +
          Card.handSep +
          Card.unknownHand +
          Card.handSep +
          Card.unknownSkat;
      }
	
      toMove = MIDDLE_HAND;
      phase = BID;
      return null;
    }

    if (phase == BID) {

      // Bidding phase

      if (move.equals("p")) {

        // If forehand passes, the game is over (all pass)

        if (pinfos[toMove].maxBid == 0)
          pinfos[toMove].maxBid = 1;
        
        if (bidder == FORE_HAND) {

          phase = FINISHED;

        } else if (bidder == MIDDLE_HAND) {

          bidder = REAR_HAND;
          asked = FORE_HAND;
          toMove = bidder;

        } else { // bidder = REAR_HAND
	  
          if (maxBid < 18) {

            // forehand hasn't bid yet - ask if he wants to play

            bidder = FORE_HAND;
            asked = WORLD_VIEW;  // noone to be asked
            toMove = bidder;

          } else  {

            // Otherwise asked player becomes declarer

            phase = SKAT_OR_HAND_DECL;
            declarer = asked;
            toMove = declarer;
            asked = -1;
            bidder = -1;
          }
        }
        return null;

      } else { // the move has to be a bid

        int bid;
        try { bid = Integer.parseInt(move); }
        catch(NumberFormatException ex) {return i18n("bid_not_number_colon") + " " + ex.toString(); }

        // The bid must be higher than the current maximum bid.
        if (bid <= maxBid) return i18n("bid_not_high_enough");

        // The bid must not be ridiculous.
        if ((bid >= 500) || (bid < 0)) return i18n("bid_ridiculous");

        // OK -> update the max bid and change the state. We're now
        // awaiting a response from the next player

        pinfos[player].maxBid = bid;
        maxBid = bid;
        phase = ANSWER;
        toMove = asked;
        // asked = bidder;
	
        // Special case: the forehand bids and the others passed. 
        if (player == FORE_HAND) {
          phase = SKAT_OR_HAND_DECL;
          declarer = FORE_HAND;
          toMove = declarer;
          asked = -1;
          bidder = -1;
        }
        return null;
      }
    }//BID

    if (phase == ANSWER) {

      // "have it" situations: we proceed normally back to
      // the bidding phase.

      if (move.equals("y")) {

        pinfos[player].maxBid = maxBid;

        phase = BID;
        toMove = bidder;
        return null;
	
      } else if (move.equals("p")) { // "I pass" situation:

	if (pinfos[toMove].maxBid == 0)
	  pinfos[toMove].maxBid = 1;
	
        if (bidder == MIDDLE_HAND) {

          phase = BID;
          bidder = REAR_HAND;
          asked = MIDDLE_HAND;
          toMove = bidder;
	  
        } else {
	  
          phase = SKAT_OR_HAND_DECL;
          declarer = bidder;
          toMove = declarer;
          asked = -1;
          bidder = -1;
        }
	
        return null;
	
      } else {
        return i18n("bad_bid_given_got") + " \"" + move + "\"";
      }
    }//ANSWER

    if (phase == SKAT_OR_HAND_DECL) {

      // If we're in the skat or hand declaration phase, we're
      // anticipating either the soloist asking to see the skat, the
      // world passing him/her those cards, or, the soloist choosing
      // to play a Hand game.
      
      if (move.equals("s")) {

        // request skat

        phase = GET_SKAT;
        toMove = WORLD_MOVE;
        decl.hand = false; // At this point, we can say the game is certainly not hand!  Needed for hashValue - Jeff
        return null;
	
      } else  {
        
        // hand game

        String[] parts = move.split("\\.");
        if (parts.length < 1)
          return i18n("illegal_move_colon") + " " + player + ", " + move;

        // Read in the game declaration (the first substring in the move)
        String declResult = decl.fromString(parts[0]);
        if (declResult != null)
          return i18n("game_decl_got") + " " + parts[0] + ", " + i18n("returned") + " " + declResult;
	
        if (!decl.hand)
          return i18n("must_declare_hand");

        if (decl.type == GameDeclaration.NULL_GAME) {
          if (maxBid > 35 && !decl.ouvert)
            return i18n("null_hand_illegal_with_bid") + " " + maxBid + ".";
          if (maxBid > 59 && decl.ouvert)
            return i18n("null_ouv_hand_illegal_with_bid") + " " + maxBid + ".";
        }
        
        if (decl.ouvert) {

          if (view != WORLD_VIEW) {

            if (view != declarer) {
            
              // reveal declarer's cards if player != declarer
              
              if (parts.length != 11)
                return i18n("move_needs_11_parts");
              
              pinfos[declarer].hand = 0;
              
              for (int i=1; i < 11; i++) {
                Card card = Card.fromString(parts[i]);
                if (card == null)
                  return i18n("ouvert_error_colon") + " " + move;
                pinfos[declarer].hand = Hand.set(pinfos[declarer].hand, card);
              }
            }
            
          } else if (views != null) {
            
            // WORLD view:
            // append declarer's cards in ouvert games
            String h = "."+Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
            
            for (int i=0; i < 4; i++) { // private + public
              // views[i] += h;  // Jeff: Changed this, perhaps dangerously
              views[i] = parts[0] + h;
            }
          }
        }

        declarerHandBeforeCardplay = pinfos[declarer].hand;
        phase = CARDPLAY;
        toMove = FORE_HAND;
        return null;
      }
    }//SKAT_OR_HAND_DECL

    if (phase == GET_SKAT) {

      ArrayList<Card> cv = Card.cardListFromString(move);
      if (cv == null)
        return i18n("card_error_in_GET_SKAT");

      if (cv.size() != 2)
        return i18n("need_2_in_skat");

      // if viewer is not declarer, we won't see anything in this move

      if (view == WORLD_VIEW || view == declarer) {
      
        skat0 = cv.get(0);
        skat1 = cv.get(1);

        if (views != null) {
          // hide skat from defenders and public view
          for (int i=0; i < 4; i++) {
            if (i != declarer) {
              views[i] = Card.unknownSkat;
            }
          }
        }
      }      
      phase = DISCARD_AND_DECL;
      toMove = declarer;
      return null;
      
    } // GET_SKAT

    if (phase == DISCARD_AND_DECL) {

      // We overload this part of the code, to potentially accept
      // 'half-moves' (game declaration comes separately from
      // discarded cards)

      assert(player == declarer);

      if (!discardHalfPhase) {

        if (move.length() <= 5) { // was 2, but NOH or GHS ...

	  // Read in the game declaration (the first substring in the move)
          String declResult = decl.fromString(move);
          if (declResult != null)
            return i18n("game_decl_got") + " " + move + ", " + i18n("returned") + " " + declResult;

          if (decl.hand) return i18n("hand_illegal_after_pickup");
          
          // check for overbid null games
    
          if (decl.type == GameDeclaration.NULL_GAME) {

            if (maxBid > 23 && !decl.ouvert)
              return i18n("null_illegal_with_bid") + " " + maxBid + ".";
            if (maxBid > 46 && decl.ouvert)
              return i18n("null_ouv_illegal_with_bid") + " " + maxBid + ".";

          }

          discardHalfPhase = true;
          return null;

        } else {
          
          // In the discard phase, the soloist has picked up the skat and
          // is now declaring the game type and discarding.
          
          String[] parts = move.split("\\.");
          if (parts.length < 3)
            return i18n("move_needs_3_parts_colon") + " " + player + ", " + move;
    
          // Read in the game declaration (the first substring in the move)
          String declResult = decl.fromString(parts[0]);
          if (declResult != null)
            return i18n("game_decl_got") + " " + parts[0] + ", " + i18n("returned") + " " + declResult;

          if (decl.hand) return i18n("hand_illegal_after_pickup");
          
          // check for overbid null games
    
          if (decl.type == GameDeclaration.NULL_GAME) {
            if (maxBid > 23 && !decl.ouvert)
              return i18n("null_illegal_with_bid") + " " + maxBid + ".";
            if (maxBid > 46 && decl.ouvert)
              return i18n("null_ouv_illegal_with_bid") + " " + maxBid + ".";
          }
    
          // does player have the "discarded" cards?
    
          Card disc1 = Card.fromString(parts[1]);
          Card disc2 = Card.fromString(parts[2]);
    
          if (disc1 == null) return i18n("illegal_discard_colon") + " " + parts[1] + ".";
          if (disc2 == null) return i18n("illegal_discard_colon") + " " + parts[2] + ".";
    
          // If we're not the declarer, we won't see the discarded cards
    
          if (view == WORLD_VIEW || view == declarer) {
            
            int newHand = pinfos[player].hand;
            if (Hand.has(newHand, skat0) || Hand.has(newHand, skat1))
              Misc.err("some of the skat cards already in player's hand?!");
    
            newHand = Hand.set(newHand, skat0);
            newHand = Hand.set(newHand, skat1);
            newHand = Hand.clear(newHand, disc1);
            newHand = Hand.clear(newHand, disc2);        
    
            if (Hand.numCards(newHand) != 10) {
              return i18n("illegal_discards");
            }
            
            // move ok
    
            pinfos[player].hand = newHand;
            skat0 = disc1;
            skat1 = disc2;
    
            declarerHandBeforeCardplay = newHand;
            
            if (views != null) {
              // hide discarded cards from opponents and public view
              for (int i=0; i < 4; i++) {
                if (i != player) {
                  views[i] = parts[0] + "." + Card.unknownSkat;
                } else {
                  views[i] = parts[0] + "." + parts[1] + "." + parts[2];
                }
              }
            }
          }
    
          if (decl.ouvert) {
    
            if (view != WORLD_VIEW) {
    
              if (view != declarer) {
    
                // reveal declarer's cards if player != declarer
                
                if (parts.length != 13)
                  return i18n("move_needs_13_parts");
                
                pinfos[declarer].hand = 0;
                
                for (int i=3; i < 13; i++) {
                  Card card = Card.fromString(parts[i]);
                  if (card == null)
                    return i18n("ouvert_error_colon") + " " + move;
                  pinfos[declarer].hand = Hand.set(pinfos[declarer].hand, card);
                }
              }
    
            } else if (views != null) {
    
              // append declarer's cards in ouvert games to player, public view
              String h = "."+Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
              
              for (int i=0; i < 4; i++) {
                views[i] += h;
              }
            }
          }
    
          declarerHandBeforeCardplay = pinfos[declarer].hand;
          phase = CARDPLAY;
          toMove = FORE_HAND;
          return null;
        }

      } else {

	// Now expecting just the discarded cards

        String[] parts = move.split("\\.");
	if (parts.length < 2)
	  return i18n("move_needs_2_parts_colon") + " " + player + ", " + move;
    
    
	// does player have the "discarded" cards?
    
	Card disc1 = Card.fromString(parts[0]);
	Card disc2 = Card.fromString(parts[1]);
    
	if (disc1 == null) return i18n("illegal_discard_colon") + " " + parts[0] + ".";
	if (disc2 == null) return i18n("illegal_discard_colon") + " " + parts[1] + ".";
    
	// If we're not the declarer, we won't see the discarded cards
    
	if (view == WORLD_VIEW || view == declarer) {
            
          int newHand = pinfos[player].hand;
          if (Hand.has(newHand, skat0) || Hand.has(newHand, skat1))
            Misc.err("some of the skat cards already in player's hand?!");
  
          newHand = Hand.set(newHand, skat0);
          newHand = Hand.set(newHand, skat1);
          newHand = Hand.clear(newHand, disc1);
          newHand = Hand.clear(newHand, disc2);        
  
          if (Hand.numCards(newHand) != 10) {
            return i18n("illegal_discards");
          }
          
          // move ok
  
          pinfos[player].hand = newHand;
          skat0 = disc1;
          skat1 = disc2;
  
          declarerHandBeforeCardplay = newHand;
          
          if (views != null) {
            // hide discarded cards from opponents and public view
            for (int i=0; i < 4; i++) {
              if (i != player) {
                views[i] = parts[0] + "." + Card.unknownSkat;
              }
            }
          }
        }
  
        if (decl.ouvert) {
  
          if (view != WORLD_VIEW) {
  
            if (view != declarer) {
  
              // reveal declarer's cards if player != declarer
              
              if (parts.length != 12)
                return i18n("move_needs_12_parts");
              
              pinfos[declarer].hand = 0;
              
              for (int i=2; i < 12; i++) {
                Card card = Card.fromString(parts[i]);
                if (card == null)
                  return i18n("ouvert_error_colon") + " " + move;
                pinfos[declarer].hand = Hand.set(pinfos[declarer].hand, card);
              }
            }
  
          } else if (views != null) {
  
            // append declarer's cards in ouvert games to player, public view
            String h = "."+Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
            
            for (int i=0; i < 4; i++) {
              views[i] += h;
            }
          }
        }
  
        declarerHandBeforeCardplay = pinfos[declarer].hand;
        phase = CARDPLAY;
        toMove = FORE_HAND;
        discardHalfPhase = false;
        return null;
      }
      
    }//DISCARD_AND_DECL

    return "invalid move - " + player + " | " + move;
  }

  /** updates the sloughs associated with the given player, given the played card in the current trick
   */
  //  public void updateSloughs(int player, Card c) {
  //    if (player == declarer) {
  //      // In declarer case, it's simple: if he doesn't take the trick, it is a slough
  //      if (trickWinner != player) {
  //        pinfos[player].sloughs = Hand.set(pinfos[player].sloughs, c);
  //      }
  //    }
  //    else {
  //      if (trickCardNum == 3) {
  //        if (trickWinner == declarer) {
  //          pinfos[player].sloughs = Hand.set(pinfos[player].sloughs, c);
  //        }
  //      }
  //      
  //      else if (trickCardNum == 2) {
  //        if (trickWinner == declarer) {
  //          if (winningCardUnbeatable(player)) {
  //            pinfos[player].sloughs = Hand.set(pinfos[player].sloughs, c);
  //          }
  //        }
  //      }
  //      
  //    }    
  //
  //}

  /**@  Return true if the trick's winning card is unbeatable as the 2nd player in the trick IF that player doesn't beat it
   */
  public boolean winningCardUnbeatable(int player) {
    if (decl.type < 0) {
      Misc.err("cardUnbeatable called with no game declaration!");
      return false;
    }
    if (trickCardNum != 2) {
      Misc.err("cardUnbeatable called at pointless time!");
    }
    Card c = winningCard;
    int playedCards = pinfos[0].playedCards | pinfos[1].playedCards | pinfos[2].playedCards;
    playedCards ^= getHand(player);
    int remainder = -1 ^ playedCards;
    int card = 0;
    int suit = 0;
    card = Hand.set(card, winningCard);
    if (decl.type > GameDeclaration.GRAND_GAME) {
      // Null game - hands return to their standard rank
      suit = Hand.getSuit(remainder, c.getSuit());
      card = Hand.getSuit(card, c.getSuit());
      if (suit < card) {
        // all cards in the suit are smaller than the card
        return true;
      }
      if (isVoid(toMove, c.getSuit())) {
        return true;
      }
      return false;
    }
    else {
      // not a null game
      if (c.getRank() == Card.RANK_JACK) {
        suit = Hand.getJacks(remainder);
        if (suit < card) {
          return true;
        }
      }
      // By this point, we know the winning card is not a jack
      if (isVoid(toMove, decl.tellCardSuit(c))) {
        // If to move is void in the necessary suit
        return true;
      }
      if ( (Hand.getJacks(remainder) != 0) && (!isVoid(toMove, decl.type))) {
        // Check if toMove is void in the jack suit to see if he might have jacks
        return false;
      }
      
      suit = Hand.getSuit(suit, c.getSuit(), false);
      suit = Hand.rearrange(suit);
      card = Hand.getSuit(card, c.getSuit(), false);
      card = Hand.rearrange(card);
      if (suit < card) {
        // If there aren't any cards in the suit that beat the winning card
        return true;
      }
      return false;
    
      
    }
      
      

  }

  /** @return Returns true if Card c1 is higher than c2 in the context of the current game.
   */
  public boolean higherCard(Card c1, Card c2) {

    if (trump(c1)) {
      if (!trump(c2)) 
        return true;
      else
        return (numRank(c1) > numRank(c2));
    } else {
      if (trump(c2))
        return false;
      else if (c1.getSuit() != c2.getSuit())
        return false;
      else
        return numRank(c1) > numRank(c2);
    }

  }

  /** @return Returns true if the given card is higher than the card
   *  currently winning the trick.
   */
  public boolean highestCard(Card card)
  {
    if (trickCardNum == 0)
      return true;

    if (trump(card)) {
      if (!trump(winningCard)) 
        return true;
      else
        return (numRank(card) > numRank(winningCard));
    } else {
      if (trump(winningCard) || !followsSuit(card))
        return false;
      else
        return numRank(card) > numRank(winningCard);
    }
  }
  
  /** @return Returns true if the given card is higher than the card
   *  currently winning the trick.
   *  Important: assumes trick0 to contain first card if one has played.
   *  This caused issues in DDS.
   */
  public boolean newCardWins(Card prevWinner, Card newCard)
  {
    if (trump(newCard)) {
      if (!trump(prevWinner)) 
        return true;
      else
        return (numRank(newCard) > numRank(prevWinner));
    } else {
      if (trump(prevWinner) || !followsSuit(newCard))
        return false;
      else
        return numRank(newCard) > numRank(prevWinner);
    }
  }
  
  /** @return Returns true if the given card follows suit in the
   *  current trick (of course, if there is no trick yet, it always
   *  follows suit!)
   */
  public boolean followsSuit(Card card)
  {
    if (trickCardNum == 0)
      return true;

    if (trump(trick0)) {
      return trump(card);
    } else if (trump(card)) {
      return false;
    }

    return card.getSuit() == trick0.getSuit();
  }


  public int winner(Card c0, Card c1, Card c2)
  {
    int wi = 0;
    Card winner = c0;
    Card tmpCard = trick0;
    trick0 = c0; // for newCardWins
    
    if (newCardWins(winner, c1)) {
      wi = 1;
      winner = c1;
    }
    if (newCardWins(winner, c2)) {
      wi = 2;
    }

    trick0 = tmpCard; // restore trick0
    return wi;
  }


  /** @return Returns true iff player from me's perspective can't possibly beat card c
   */
  public boolean cantOvertake(int me, int player, Card c)
  {
    int cards; // possible cards player has

    if (handKnown(player)) {
      
      cards = getHand(player); // I know the hand

    } else {
    
      // guess: inverse of seen cards and known 3rd player cards ...
      cards = cardsOthersCantHave(me);

      if (me != player && handKnown(3-me-player))
        cards |= getHand(3-me-player);

      cards = ~cards;

      // ... minus void suits
      
      int voids = 0; // void suit card bits
      int vMask = getVoids(player);
      
      for (int s=0; s < 4; s++) {
        if (isVoid(player, s)) {
          voids |= ((0xff) << (8*s));
        }
      }
      
      voids &= ~Card.JACK_MASK;
      
      if (decl.type == GameDeclaration.GRAND_GAME) {
        
        if (isVoid(player, 4)) voids |= Card.JACK_MASK;
        
      } else {
        
        assert decl.type != GameDeclaration.NULL_GAME;
        
        if (isVoid(player, decl.type)) voids |= Card.JACK_MASK;
      }
      
      cards &= ~voids;
    }

    if (false) {
      Misc.msg("!!! contains: " + me + " " + player + " " +
               Hand.toStringColor(cards, (decl.type < 4 ? decl.type : 3)) +
               " c:" + c.toStringColor() +
               " higher:" + containsHigherCard(cards, c));
    }
    
    return !containsHigherCard(cards, c);
  }
  
  /** @return Returns true if the card c is ok in the current trick.
   */
  public boolean cardOK(Card c)
  {
    if (trickCardNum == 0 || trickCardNum == 3)
      return true;
    
    if (followsSuit(c))
      return true;
  
    return !canFollowSuit(toMove, trick0);
  }

  /** @return Returns true if the card c is ok in the current trick.
   */
  public boolean cardOK(int player, Card c)
  {
    if (trickCardNum == 0 || trickCardNum == 3)
      return true;
    if (followsSuit(c))
      return true;
  
    return !canFollowSuit(player, trick0);
  }

  /** @return Whether or not the given player is capable of following
   * the suit of the given card.
   */
  public boolean canFollowSuit(int player, Card card) 
  {
    if (decl.type == GameDeclaration.NULL_GAME) {
      // no trump
      return Hand.suitBits(pinfos[player].hand, card.getSuit()) != 0;
    }

    if (trump(card)) {
      return hasTrump(player);
    
    } else {
      return (Hand.suitBits(pinfos[player].hand, card.getSuit()) & ~(1 << Card.RANK_JACK)) != 0;
    }
  }

  /** @returns true iff the given player has any trump cards
   */
  public boolean hasTrump(int player)
  {
    // In a null game, no trumps at all.
    if (decl.type == GameDeclaration.NULL_GAME)
      return false;

    for (int i = 0; i < 4; i++) {
      if ((Hand.suitBits(pinfos[player].hand, i) & (1 << Card.RANK_JACK)) != 0)
        return true; // has a jack
    }

    // grand: only jacks are trump
    if (decl.type == GameDeclaration.GRAND_GAME)
      return false;

    // otherwise, we're in a suit game
    return Hand.suitBits(pinfos[player].hand, decl.type) != 0;
  }

  /** @returns true iff hand contain a trump
   */
  public boolean containsTrump(int hand)
  {
    // In a null game, no trumps at all.
    if (decl.type == GameDeclaration.NULL_GAME)
      return false;

    if ((hand & Card.JACK_MASK) != 0) return true; // has jack

    // grand: only jacks are trump
    if (decl.type == GameDeclaration.GRAND_GAME)
      return false;

    // otherwise, we're in a suit game
    return Hand.suitBits(hand, decl.type) != 0;
  }

  /** @returns true iff hand contain a higher card than c
   */
  public boolean containsHigherCard(int hand, Card c)
  {
    // todo: speed this up

    for (int i=0; i < 32; i++) {
      if ((hand & (1 << i)) != 0) {
        Card c2 = Card.newCard(i / 8, i & 7);
        if (higherCard(c2, c)) return true;
      }
    }
    return false;
  }
  

  /** @return whether a card is a trump in the context of the game declaration */
  public boolean trump(Card card)
  {
    if (decl.type == GameDeclaration.NULL_GAME)
      return false;
    if (card.getRank() == Card.RANK_JACK)
      return true;
    if (decl.type == GameDeclaration.GRAND_GAME)
      return false;
    
    return card.getSuit() == decl.trumpSuit();
  }
  
  
  /** @return whether a card is a trump in the context of gameType */
  public static boolean trump(Card card, int gameType)
  {
    if (gameType == GameDeclaration.NULL_GAME)
      return false;
    if (card.getRank() == Card.RANK_JACK)
      return true;
    if (gameType == GameDeclaration.GRAND_GAME)
      return false;
    
    return card.getSuit() == GameDeclaration.trumpSuit(gameType);
  }
  
  
  /** @return the point value of the completed trick */
  public int currentTrickPoints()
  {
    if (trickCardNum != 3) return -1;
    
    return 
      Card.points[trick0.getRank()] +
      Card.points[trick1.getRank()] +
      Card.points[trick2.getRank()];
  }
  
  /** @return the point value of current partial trick */
  public int currentPartialTrickPoints()
  {
    int p = 0;
    if (trickCardNum >= 1) p += Card.points[trick0.getRank()];
    if (trickCardNum >= 2) p += Card.points[trick1.getRank()];
    if (trickCardNum >= 3) p += Card.points[trick2.getRank()];
    return p;
  }
  
  /** @return The numerical ranking of a supplied card */
  public int numRank(Card card)
  {
    if (decl.type == GameDeclaration.NULL_GAME) 
      return Card.nullRanks[card.getRank()];
    
    if (card.getRank() == Card.RANK_JACK)
      return Card.suitRanks[card.getRank()] + card.getSuit();
    
    return Card.suitRanks[card.getRank()];
  }

  /** @return True if the player to move is starting a new trick
   */
  public boolean leading() {

    if (phase != CARDPLAY) {
      return false;
    }

    if (trickCardNum == 0 || trickCardNum == 3) {
      return true;
    }
    else {
      return false;
    }

  }

  /** @return A vector containing all the legal moves (as strings) of the current position.
   */
  public int genMoves(String[] moves)
  {
    int mn = 0; // number of moves returned
    
    if (phase == CARDPLAY) {

      /*  Cardplay phase - getting the cards we can play
       */

      if (trickCardNum == 0 || trickCardNum == 3 ||
          !canFollowSuit(toMove, trick0)) {

        // all cards possible

        int hand = pinfos[toMove].hand;
        for (int s=0; s < 4; s++) {
          
          String[] cards = suitCardStrings[s][(hand >>> (8*s)) & 0xff];
          int n = cards.length;
          for (int i=0; i < n; i++) {
            moves[mn++] = cards[i];
          }
        }
        
      } else {

        // follow suit
        int suit = trick0.getSuit();

        if (decl.type == GameDeclaration.NULL_GAME) {

          // null game: need to play card of the same suit
          
          int scards = Hand.suitBits(pinfos[toMove].hand, suit);
          for (int r=0; r < 8; r++) {
            if ((scards & (1<<r)) != 0) {
              // moves[i++] = Card.newCard(suit, r).toString();
              moves[mn++] = Card.newCard(suit, r).toString();
            }
          }

        } else {

          // trump game

          if (trump(trick0)) {

            // jacks
            
            for (int s=0; s < 4; s++) {
              if ((Hand.suitBits(pinfos[toMove].hand, s) & (1 << Card.RANK_JACK)) != 0) {
                moves[mn++] = Card.newCard(s, Card.RANK_JACK).toString();
              }
            }
            
            if (decl.type != GameDeclaration.GRAND_GAME) {

              // trump suit (no jack)
              int trumpSuit = decl.trumpSuit();
              int scards = Hand.suitBits(pinfos[toMove].hand, trumpSuit) &
                ~(1 << Card.RANK_JACK);

              for (int r=0; r < 8; r++) {
                if ((scards & (1<<r)) != 0) {
                  moves[mn++] = Card.newCard(trumpSuit, r).toString();
                }
              }
            }

          } else {

            // need to play suit (no jacks)

            int scards = Hand.suitBits(pinfos[toMove].hand, suit) & ~(1 << Card.RANK_JACK);
            
            for (int r=0; r < 8; r++) {
              if ((scards & (1<<r)) != 0) {
                moves[mn++] = Card.newCard(suit, r).toString();
              }
            }
          }
        }
      }

      //       Misc.msg(toString());

      //       for (String s : theVector) {
      //         Misc.msg("move: " + s);
      //       }

      if (false) {

        // check move vector against slow implementation

        Card[] cards = new Card[12];
        int n = Hand.toCardArray(pinfos[toMove].hand, cards);
        Vector<String> newVector = new Vector<String>();
        
        // fixme: this needs to be sped-up significantly!

        for (int i=0; i < n; i++) {
          if (cardOK(cards[i])) {
            newVector.add(cards[i].toString());
          }
        }

        if (newVector.size() != mn) {
          Misc.err("different vector sizes");
        }
      }

    } else if (phase == BID) {
      
      /* BID - The player passes or makes the next highest bid (we are
       * simplifying by assuming they don't jump up by a ton of bid
       * numbers
       */
      
      moves[mn++] = "p";
      int bid = 0;
      
      for (int i = 0; i < BIDS.length; i++) {
        if (BIDS[i] > maxBid) {
          bid = BIDS[i];
          break;
        }
      }

      if (bid > 0) {
        moves[mn++] = "" + bid;
      }

    } else if (phase == ANSWER) {
    
      /* ANSWER - Player simply says yes or pass to the most recent bid.
       */
  
      moves[mn++] = "y";
      moves[mn++] = "p";

    } else if (phase == SKAT_OR_HAND_DECL) {

      /* SKAT_OR_HAND_DECL - Declarer says whether or not they want
       * the skat
       */

      String yes = "s";
      moves[mn++] = yes;

      for (int i = 0; i < GameDeclaration.gameNames.length; i++) {

        String suit = GameDeclaration.gameNames[i] + "H";

        if (GameDeclaration.gameNames[i].equals("N")) {
          if (pinfos[declarer].maxBid <= 35) {
            moves[mn++] = suit;
          }
        } else {
          moves[mn++] = suit;
        }

        if (i < GameDeclaration.gameNames.length - 1) {
	  // Not a null game
          moves[mn++] = suit + "O" + "." + Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));          
          moves[mn++] = suit + "S"+ "." + Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
          moves[mn++] = suit + "Z" + "." + Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
                   
        }
        else {
          if (pinfos[declarer].maxBid <= 59) {
            moves[mn++] = suit + "O" + "." + Card.cardListToString(Hand.toCardList(pinfos[declarer].hand));
          }
        }
      }
    } else if (phase == DISCARD_AND_DECL) {
      
      /* DISCARD_AND_DECL
	 Declarer picks up the skat; discards any two of the 12 cards.
      */

      if (discardHalfPhase) {
  
        Card[] cards = new Card[12]; // fixme: no new please
        int n = Hand.toCardArray(pinfos[declarer].hand, cards);
  
        // if (cards.size() != 12) Misc.err("not 12 cards in hand"); // Apparently we changed this, so the skat cards aren't in the declarer's hand.
  
        //ArrayList<String> discards = new ArrayList<String>();
        
        // for (int i=0; i < 11; i++) {
        for (int i = 0; i < 9; i++) {
          // for (int j=i+1; j < 12; j++) {
          for (int j = i + 1; j < 10; j++) {
            StringBuilder ss = new StringBuilder();
            ss.append(cards[i].toString());
            ss.append(".");
            ss.append(cards[j].toString());
            // discards.add(ss.toString());
            if (decl.ouvert) {
              ss.append(".");
              ss.append(Card.cardListToString(Hand.toCardList(pinfos[declarer].hand)));
            }
            moves[mn++] = ss.toString();
          }
          for (int j = 0; j < 2; j++) {
            StringBuilder ss = new StringBuilder();
            ss.append(cards[i].toString());
            ss.append(".");
            if (j == 0) ss.append(skat0.toString());
            else        ss.append(skat1.toString());
            // discards.add(ss.toString());
            if (decl.ouvert) {
              ss.append(".");
              ss.append(Card.cardListToString(Hand.toCardList(pinfos[declarer].hand)));
            }
            moves[mn++] = ss.toString();
          }
        }
  
        // At this point we will have missed the 10th card, plus the 2 skat cards together.
        for (int i = 0; i < 2; i++) {
          StringBuilder ss = new StringBuilder();
          ss.append(cards[9].toString());
          ss.append(".");
          if (i == 0) ss.append(skat0.toString());
          else        ss.append(skat1.toString());
          // discards.add(ss.toString());
          if (decl.ouvert) {
            ss.append(".");
            ss.append(Card.cardListToString(Hand.toCardList(pinfos[declarer].hand)));
          }
          moves[mn++] = ss.toString();
        }

        // Now we have missed only the 2 skat card together.
        StringBuilder ss = new StringBuilder();
        ss.append(skat0.toString());
        ss.append(".");
        ss.append(skat1.toString());

        if (decl.ouvert) {
          ss.append(".");
          ss.append(Card.cardListToString(Hand.toCardList(pinfos[declarer].hand)));
        }
        // discards.add(ss.toString());
        moves[mn++] = ss.toString();
  
        // if (discards.size() != 66) Misc.err("not 66");

        //for (String s : discards) {
        //  moves[mn++] = s;
        //}
      } // end HALFPHASE if

      else {
  
        for (int i = 0; i < GameDeclaration.gameNames.length; i++) {
          // StringBuilder suit = new StringBuilder(toMove + " " + GameDeclaration.gameNames[i]);
          String name = GameDeclaration.gameNames[i];
          if (name.equals("N")) {
            if (pinfos[declarer].maxBid > 23) {
              continue;
            }
          } 
          moves[mn++] = name;
          
        }
  
        // Null is the only game that can be played ouvert if we pick up
        // the skat.  String suit = toMove + " NO.";
        if (pinfos[declarer].maxBid <= 46) {
          
          moves[mn++] = "NO";
          
        }
      }

      //DISCARD_AND_DECL
    }

    return mn;
  }

  /** @return true if game has ended */
  public boolean isFinished() { return phase == FINISHED; }

  @Override
    public String toString() { return toString(false); }
  
  public String toString(boolean color)
  {
    int col = decl.type;
    String out = "";

    if (color) {
      if (decl.type == GameDeclaration.GRAND_GAME)
        col = Card.SUIT_CLUBS;
      else if (decl.type == GameDeclaration.NULL_GAME)
        col = Card.SUIT_NONE;
    }
    
    for (int i = 0; i < 3; i++) {
      out += i + ": ";
      if (color) {
        out += Hand.toStringColor(pinfos[i].hand, col, false);
      } else {
        out += Hand.toString(pinfos[i].hand, true);
      }
      out += " v:" + pinfos[i].voids + " ";
      if (color) {
        out += " played: " + Hand.toStringColor(pinfos[i].playedCards, col, false) + "\n";
      } else {
        out += " played: " + Hand.toString(pinfos[i].playedCards, true) + "\n";
      }
    }
    if (color) {
      out += "Skat: " + skat0.toStringColor() + skat1.toStringColor() + "\n";      
    } else {
      out += "Skat: " + skat0 + "." + skat1 + "\n";
    }
    out += "phase: " + phase + "\n";

    if (phase == SimpleState.CARDPLAY) {
      out += "trick num: " + getTrickNum() + "\n";
    }

    int tcn = trickCardNum;
    if (tcn == 3)
      tcn = 0;
    
    out += "trick cards: " + tcn + " ";
    for (int i=0; i < tcn; i++) {
      if (color)
        out += getTrickCard(i).toStringColor();
      else
        out += " " + getTrickCard(i).toString();
    }
      
    out += "\ndecl: " + declarer + "\n";
    out += "type: " + decl + "\n";
    out += "to move: " + toMove + "\n";
    return out;
  }

  /** @return the scores for the end of the game, based on whatever
   * measure we're currently using */

  public void tournamentScores(int[] scores, GameResult gr) {
    // tournamentScoresMaxmin(scores, gr);
    tournamentScoresMoney(scores, gr);
  }

  /** @return tournament money scores for all players when finished */
  public void tournamentScoresMoney(int[] scores, GameResult gr)
  {
    assert(phase == SimpleState.FINISHED);

    gameResult(gr);
    scores[0] = scores[1] = scores[2] = 0;
    // Jeff: added this statement, to account for when everybody passes
    if (gr.declarer < 0) {
      return;
    }

    if (gr.declValue < 0) {
      scores[0] = scores[1] = scores[2] = DEF3WIN;
      scores[gr.declarer] = gr.declValue - 50;
    } else {
      scores[gr.declarer] = gr.declValue + 50;
    }

    int sum = scores[0] + scores[1] + scores[2];
    for (int i=0; i < 3; i++) {
      scores[i] = 3*scores[i] - sum; // !!! error: was 2*scores !!!
    }

  }

  /** @return tournament point scores for all players when finished */
  public void tournamentScoresPoints(int[] scores, GameResult gr)
  {
    assert(phase == SimpleState.FINISHED);

    gameResult(gr);
    scores[0] = scores[1] = scores[2] = 0;
    // Jeff: added this statement, to account for when everybody passes
    if (gr.declarer < 0) {
      return;
    }

    if (gr.declValue < 0) {
      scores[0] = scores[1] = scores[2] = DEF3WIN;
      scores[gr.declarer] = gr.declValue - 50;
    } else {
      scores[gr.declarer] = gr.declValue + 50;
    }
  }

  /** @return Scores for players using max-min(difference) approach rather than the money game */
  public void tournamentScoresMaxmin(int[] scores, GameResult gr) {

    assert (phase == SimpleState.FINISHED);

    gameResult(gr);
    scores[0] = scores[1] = scores[2] = 0;
    if (gr.declarer < 0) {
      return;
    }
    if (gr.declValue < 0) {
      scores[gr.declarer] = gr.declValue - 50;
    }
    else {
      scores[0] = scores[1] = scores[2] = (-1 * gr.declValue) - 50;
      scores[gr.declarer] = gr.declValue + 50;
    }

  }


  /** @return value of game for declarer when finished */
  public void gameResult(GameResult gr)
  {
    gr.init(); // Jeff added this, as without zeroing, some wrong results were appearing

    if (!isFinished()) return;

    gr.unknown = false;
    gr.timeout = timeout;
    gr.left = left;
    
    if (decl.type != GameDeclaration.NO_GAME) {

      // game has been declared
      
      int declPoints = pinfos[declarer].trickPoints;

      Card newSkat0 = skat0, newSkat1 = skat1;

      if (!newSkat0.isKnown()) {

        // try to infer skat cards
        int h = ~(getPlayedCards(0) | getPlayedCards(1) | getPlayedCards(2));
        if (Hand.numCards(h) == 2) {
          // compute skat without using new
          for (int i=0; i < 32; i++) {
            if ((h & (1 << i)) != 0) {
              if (!newSkat0.isKnown()) {
                newSkat0 = Card.newCard(i/8, i%8);
              } else {
                newSkat1 = Card.newCard(i/8, i%8); break;
              }
            }
          }
        }
      }

      if (newSkat0.isKnown())
        declPoints += newSkat0.value() + newSkat1.value();
      
      int declTricks = pinfos[declarer].tricksWon;
      boolean declInstantLoss  =
        pinfos[declarer].resigned != 0 ||
        (timeout >= 0 && timeout == declarer) ||
        (left >= 0 && left == declarer);
      
      boolean defsInstantLoss  =
        (pinfos[0].resigned + pinfos[1].resigned + pinfos[2].resigned - pinfos[declarer].resigned)
        >= 2 || (timeout >= 0 && timeout != declarer) || (left >= 0 && left != declarer);

      gr.resigned =
        (pinfos[declarer].resigned != 0 ||
         (pinfos[0].resigned + pinfos[1].resigned + pinfos[2].resigned - pinfos[declarer].resigned) >= 2);
      
      if (declInstantLoss)
        defsInstantLoss = false; // simultaneous instant loss -> declarer loses
      
      // tally up the points
      
      if (decl.type != GameDeclaration.NULL_GAME) {
        
        if (defsInstantLoss) {
          
          // remaining points and tricks go to declarer
          
          int pointsLeft = 120 - declPoints;
          int tricksLeft = 10 - declTricks;
          
          for (int i = 0; i < 3; ++i) {
            if (i != declarer) {
              pointsLeft -= pinfos[i].trickPoints;
              tricksLeft -= pinfos[i].tricksWon;
            }
          }
          
          declPoints += pointsLeft;
          declTricks += tricksLeft;
        }
      }

      int dh = declarerHandBeforeCardplay;
      
      if (dh == 0) {

        // declarer hand unknown, try to infer: approximation: played cards

        dh = getPlayedCards(declarer);
      }
      
      decl.value(dh, newSkat0, newSkat1,
                 declPoints, declTricks, maxBid,
                 declInstantLoss,                  
                 pinfos[0].tricksWon + pinfos[1].tricksWon + pinfos[2].tricksWon,
                 gr);
      gr.declarer = declarer;
      gr.declCardPoints = declPoints;
      gr.declTricks = declTricks;

    } else {
      
      // no game declared

      if (left >= 0 || timeout >= 0) {

        // penalty

	//Misc.msg(left + " " + timeout);
	
	gr.penalty = true;
	
        if (declarer < 0) {
          
          // end in bidding phase:
          // left or timeout: penalties
          if (left >= 0) gr.setPenalty(left, 1);
          if (timeout >= 0) gr.setPenalty(timeout, 1);
          
	} else {

	  // end in declaration phase:

          // team penalties

	  gr.declarer = declarer;
	  
          if ((left >= 0 && left == declarer) || (timeout >= 0 && timeout == declarer)) {
            // highest bidder loses
            gr.setPenalty(declarer, 1);
          } else {
            // defenders lose
            gr.setPenalty(0, 1);
            gr.setPenalty(1, 1);
            gr.setPenalty(2, 1);
            gr.setPenalty(declarer, 0);
          }
        }

      } else
	gr.passed = true;
    }
  }

  /** @return cards the other players can't have */
  public int cardsOthersCantHave(int player)
  {
    int seen =
      pinfos[0].playedCards | pinfos[1].playedCards | pinfos[2].playedCards |
      pinfos[player].hand;

    if (player == declarer && !decl.hand && (view == player || view == WORLD_VIEW)) {
      seen = Hand.set(seen, skat0);
      seen = Hand.set(seen, skat1);      
    }
    return seen;
  }

  // returns true iff
  //   player can't overtake current trick winning card (trick0 + move)
  public boolean cantOvertake1(int player, Card move)
  {
    return cantOvertake(getToMove(), player, move);
  }

  // returns true iff
  //   next player can't overtake current trick winning card (trick0 + move)
  public boolean cantOvertake2(Card move)
  {
    Card winner = getWinningCard();
    if (newCardWins(winner, move)) winner = move;
    
    return cantOvertake(getToMove(), (getToMove() + 1) % 3, winner);
  }

  // returns true iff player to move in forehand gets remaining tricks in trump games
  // obsolete, call playerToMoveGetsAll
  public boolean getsRestX()
  {
    if (getPhase() != SimpleState.CARDPLAY)
      return false;
    
    if (getGameDeclaration().type == GameDeclaration.NULL_GAME)
      return false;

    if (getTrickCardNum() != 0 && getTrickCardNum() != 3)
      return false;
    
    int toMove = getToMove();
    String[] moves = new String[10];  // fixme: slow
    int mn = genMoves(moves);
    
    for (int i=0; i < mn; i++) {
      Card c = Card.fromString(moves[i]);
      if (!cantOvertake1((toMove+1) % 3, c) || 
          !cantOvertake1((toMove+2) % 3, c))
        return false;
    }

    return true;
  }

  // return true iff hand is safe when decl. does not lead
  public boolean safeNull()
  {
    if (decl.type != GameDeclaration.NULL_GAME) return false;
    if (phase != SimpleState.CARDPLAY) return false;
    int hand = getHand(declarer);
    if (hand == 0) return false; // don't know declarer's hand

    // consider decl. leading unsafe
    if ((toMove - trickCardNum + 3) % 3 == declarer) return false;
    
    int gone =
      getPlayedCards(0) | getPlayedCards(1) | getPlayedCards(2) |
      getSkat0().toBit() | getSkat1().toBit();

    if (trickCardNum == 1 || trickCardNum == 2) {
      // remove trick cards from gone
      for (int i=0; i < trickCardNum; i++) {
        Card c = getTrickCard(i);
        gone &= ~c.toBit();
        if ((toMove - trickCardNum + i + 3) % 3 == declarer) {
          // declarer played that card -> add to declarer hand
          hand |= c.toBit();
          // Misc.msg("" + i + ": " + c.toStringColor());
        }      
      }
    }

    return Hand.safeNoLeadSuits(hand, gone) == 4;
  }
  
  // return highest card if player to move in forehand position can win
  public Card getAllMove()
  {
    if (getPhase() != SimpleState.CARDPLAY)
      return null;

    int gameType = getGameDeclaration().type;
    int trickCardNum = getTrickCardNum();
    
    if (gameType != GameDeclaration.NULL_GAME &&
        (trickCardNum == 0 || trickCardNum == 3) &&
        trickLeaderGetsAll()) {

      // we get everything: play highest card
      
      // play highest trump if you have one
      int hand = getHand(getToMove());
      int tn = 11;
      if (gameType == GameDeclaration.GRAND_GAME)
        tn = 4;
      
      for (int i=tn-1; i >= 0; i--) {
        int ind = Card.suitTrumpIndexes[gameType][i];
        if ((hand & (1 << ind)) != 0) {
          return Card.fromIndex(ind);
        }
      }
      
      // play highest card in any suit
      for (int s=0; s < 4; s++) {
        if (s == gameType) continue; // skip trump
        
        for (int i=7; i >= 0; i--) {
          int ind = Card.suitTrumpIndexes[s][i];
          if ((hand & (1 << ind)) != 0) {
            return Card.fromIndex(ind);
          }
        }
      }
      
      Misc.err("shouldn't get here");
    }
    return null;
  }

  // return current trick leader
  public int trickLeader()
  {
    return (getToMove() - getTrickCardNum() + 3 ) % 3;
  }
  
  /** in trump games, return true if trick leader cards dominate
      (=> wins all tricks)
  */
  public boolean trickLeaderGetsAll()
  {
    if (getPhase() != SimpleState.CARDPLAY) Misc.err("not in cardplay phase");
    if (decl.type == GameDeclaration.NULL_GAME) return false;
    if (!handKnown(toMove)) return false;
    
    int leader = trickLeader();

    if (!handKnown(leader)) return false; // don't know anything about leader hand

    int otherCards  = ~cardsOthersCantHave(leader);
    int leaderCards = pinfos[leader].hand;

    if (getTrickCardNum() == 1 || getTrickCardNum() == 2) {

      // one or two cards have been played in trick
      // check whether a subsequent player can possibly overtake led card;
      
      if (!cantOvertake(toMove, (leader+1) % 3, trick0) || 
          !cantOvertake(toMove, (leader+2) % 3, trick0))
        return false;

      // correct other cards by undoing 2nd trick move (first card played
      // by trick leader doesn't change other cards)

      if (getTrickCardNum() == 2) {
        otherCards |= trick1.toBit();
      }
    } 

    //Misc.msg("!!! XXXXX");
    //Misc.msg(Hand.toStringColor(leaderCards, 3, false) + " " + Hand.toStringColor(otherCards, 3, false));
    
    // check trump
    
    int leaderTrump = leaderCards & Card.JACK_MASK;
    int otherTrump  = otherCards  & Card.JACK_MASK;

    if (decl.type != GameDeclaration.GRAND_GAME) {

      // add right shifted trump suit bits 

      leaderTrump |= (leaderCards >>> Card.SUIT_POS[decl.type]) & Card.NO_JACK_SUIT_MASK;
      otherTrump  |= (otherCards  >>> Card.SUIT_POS[decl.type]) & Card.NO_JACK_SUIT_MASK;
    }

    if (!playerToMoveGetsAllTrump(leaderTrump, otherTrump)) {
      // Misc.msg("TRUMP - NO!");
      return false;
    }

    // check suits

    for (int s=0; s < 4; s++) {

      int leaderSuit, otherSuit;
      
      if (decl.type == GameDeclaration.GRAND_GAME || decl.type != s) {

	leaderSuit = 
	  ((leaderCards & ~Card.JACK_MASK) >>> Card.SUIT_POS[s]) & Card.NO_JACK_SUIT_MASK;

	otherSuit =
	  ((otherCards & ~Card.JACK_MASK) >>> Card.SUIT_POS[s]) & Card.NO_JACK_SUIT_MASK;
	   
	if (!playerToMoveGetsAllNonTrumpSuit(leaderSuit, otherSuit)) {
          // Misc.msg("SUIT - " + s + " - NO!");          
	  return false;
        }
      }
    }
    
    return true;
  }

  // @return true iff player to move gets remaining tricks in that suit
  // play high, respond low until player has no cards anymore ...
  // @param player,otherBits: card sets (8 lowest bits / jack bit 0)
  private boolean playerToMoveGetsAllNonTrumpSuit(int playerBits, int otherBits)
  {
    if (playerBits == 0 || otherBits == 0) return true;
    if ((playerBits & ~Card.NO_JACK_SUIT_MASK) != 0) Misc.err("wrong bits 1 " + playerBits);
    if ((otherBits  & ~Card.NO_JACK_SUIT_MASK) != 0) Misc.err("wrong bits 2 " + otherBits);
    if ((playerBits & otherBits) != 0) Misc.err("card intersection");

    //System.out.print(" CARDS: " + Hand.toStringColor(playerBits, 3, false) + " " + Hand.toStringColor(otherBits, 3, false));

    // convert into trump order
    playerBits = Hand.nullToTrumpGameOrder[playerBits];
    otherBits = Hand.nullToTrumpGameOrder[otherBits];    

    int pbit = 0x80; // bit position in playerBits, high->low

    while (playerBits > 0 && otherBits > 0) {

      // both parties have at least one card
      
      if (playerBits < otherBits) {
        //Misc.msg(" UNSAFE");
        return false; // highest 1-bit in player < highest 1-bit in other => lose card
      }
        
      // remove highest bit in playerBits
      // and lowest bit in otherBits

      while ((pbit & playerBits) == 0) // skip 0s
        pbit >>>= 1;
      
      playerBits &= ~pbit;
      otherBits &= (otherBits - 1); // bit magic
    }

    // Misc.msg(" SAFE");
    return true;
  }

  // returns true if player to move can clean out trump
  // <=>  (number of player trumps >= highest opponent trumps) >= number opp. trumps
  // @param playerBits,otherBits: cards (Jack bits + 7 suit bits (lowest))
  private boolean playerToMoveGetsAllTrump(int playerBits, int otherBits)
  {
    if (otherBits == 0) return true; // others have no trump, I am good

    if ((playerBits & ~(Card.JACK_MASK | 0xff)) != 0) Misc.err("wrong bits 1");
    if ((otherBits  & ~(Card.JACK_MASK | 0xff)) != 0) Misc.err("wrong bits 2");
    if ((playerBits & otherBits) != 0) Misc.err("card intersection");

    if ((otherBits & (1<<(Card.RANK_JACK+24))) != 0)
      return false; // others have CJ, nobody beats that guy
    
    int pn = Misc.popCount(playerBits);
    int on = Misc.popCount(otherBits);
    if (on > pn) return false; // others have more trump, can't get them all

    // convert into compact 12-bit representation
    playerBits = Hand.compactTrumpBits(playerBits);
    otherBits  = Hand.compactTrumpBits(otherBits);
    
    // compute highest set bit in otherBits
    int hbo = 0x800;
    while ((hbo & otherBits) == 0)
      hbo >>>= 1;

    // how many cards are higher in playerBits?
    int k=0;
    while (hbo < 0x1000) {
      if ((playerBits & hbo) != 0) k++;
      hbo <<= 1;
    }

    return k >= on;
  }
  
  /** for null games */
  boolean playerToMoveGetsNothing()
  {
    Misc.err("implement"); // fixme
    return false;
  }


  public static int getVoidMask(int gameType, int voids)
  {
    return voidMasks[gameType][voids];
  }
  
  static {

    suitCardStrings = new String[4][256][];

    // generate card string arrays for each suit configuration
    for (int s=0; s < 4; s++) {
      for (int c=0; c < 256; c++) {
        int bitCount = Misc.popCount(c);
        int cn = 0;
        String[] cards = new String[bitCount];
        for (int r=0; r < 8; r++) {
          if ((c & (1 << r)) != 0) {
            cards[cn++] = Card.newCard(s, r).toString();
          }
        }
        suitCardStrings[s][c] = cards;
      }
    }

    // create void masks for fast lookup
    voidMasks = new int[7][32];

    if (GameDeclaration.DIAMONDS_GAME != 0) Misc.err("diamonds != 0");
    
    // suit games
    for (int t=0; t < 4; t++) {
      for (int i=0; i < 32; i++) {
        int mask = 0;
        for (int b=0; b < 4; b++) {
          if ((i & (1<<b)) != 0) {
            if (b == t) {
              // trump suit
              mask |= Card.JACK_MASK;
              mask |= Card.SUIT_MASK[b];
            } else {
              // side suit (no jacks)
              mask |= Card.SUIT_MASK[b] & ~Card.JACK_MASK;
            }
          }
        }
        voidMasks[t][i] = mask;
      }
    }

    // grand games
    for (int i=0; i < 32; i++) {
      int mask = 0;
      for (int b=0; b < 5; b++) {
        if ((i & (1<<b)) != 0) {
          if (b == 4) {
            // jacks
            mask |= Card.JACK_MASK;
          } else {
            // suit (no jacks)
            mask |= Card.SUIT_MASK[b] & ~Card.JACK_MASK;
          }
        }
      }
      voidMasks[GameDeclaration.GRAND_GAME][i] = mask;
    }

    // null games
    for (int i=0; i < 32; i++) {
      int mask = 0;
      for (int b=0; b < 4; b++) {
        if ((i & (1<<b)) != 0) {
          // suit
          mask |= Card.SUIT_MASK[b];
        }
      }
      voidMasks[GameDeclaration.NULL_GAME][i] = mask;
    }
  }

  /** @return != 0 iff the cards are corrupt in world view */
  public int perfectInfoCardsOK()
  {
    int inHands = getHand(0) | getHand(1) | getHand(2);
    int played  = getPlayedCards(0) | getPlayedCards(1) | getPlayedCards(2);
    int skat    = getSkat0().toBit() | getSkat1().toBit();

    if ((inHands | played | skat) != -1) return 1;

    int n0 = Hand.numCards(getHand(0));
    int n1 = Hand.numCards(getHand(1));
    int n2 = Hand.numCards(getHand(2));
    
    if (n0 != numCardsByTricks(0)) return 2;
    if (n1 != numCardsByTricks(1)) return 3;
    if (n2 != numCardsByTricks(2)) return 4;

    if (n0 + n1 + n2 + Hand.numCards(played) + Hand.numCards(skat) != 32) return 5;

    return 0;
  }


  // return bitset of possible cards based on void info
  public int getVoidPossible(int player)
  {
    int voids = 0; // void suit card bits
    int vMask = getVoids(player);
    
    for (int s=0; s < 4; s++) {
      if (isVoid(player, s)) {
        voids |= ((0xff) << (8*s));
      }
    }
    
    voids &= ~Card.JACK_MASK;
    
    if (decl.type == GameDeclaration.GRAND_GAME) {
      
      if (isVoid(player, 4)) voids |= Card.JACK_MASK;
      
    } else {
      
      assert decl.type != GameDeclaration.NULL_GAME;
      
      if (isVoid(player, decl.type)) voids |= Card.JACK_MASK;
    }
    return ~voids;
  }
  

  /** Assumes that at least one player knows his cards exactly. */
  public int numConsistent(int knownPos)
  {
    boolean knownSkat = !decl.hand;
    int[] hs_tmp = new int[3];
    getHandSizes(hs_tmp);

    // players with unknown cards
    int u0 = (knownPos+1) % 3; 
    int u1 = (knownPos+2) % 3;
    
    int hk = getHand(knownPos);
    int allPlayed = getPlayedCards(0) | getPlayedCards(1) | getPlayedCards(2);
    int avail = ~(allPlayed | hk);
    
    if (knownSkat) {
      int s = getSkat();
      if (s == 0) Misc.err("unknown skat");
      avail &= ~s;
    }
    
    int h0 = avail & getVoidPossible(u0); // can be in h0
    int h1 = avail & getVoidPossible(u1); // can be in h1
    int h01 = h0 & h1; // shared
    h0 &= ~h01;        // only in h0
    h1 &= ~h01;        // only in h1

    int count = 0;

    if (!knownSkat) {

      for (int i=0; i < 32; i++) {
        int c0 = 1 << i;
        if ((avail & c0) == 0) continue;
        for (int j=0; j < i; j++) {
          int c1 = 1 << j;
          if ((avail & c1) == 0) continue;
          int hh0  = h0  & ~(c0 | c1);
          int hh1  = h1  & ~(c0 | c1);
          int hh01 = h01 & ~(c0 | c1);
        
          int d = Misc.combCount(Misc.popCount(hh0),
                                 Misc.popCount(hh1),
                                 Misc.popCount(hh01),
                                 hs_tmp[u0], hs_tmp[u1]);
          count += d;

          if (testMatch())
            Misc.msg(Hand.toStringColor(c0 | c1, 3, false) + " " + d);

        }
      }

    } else {

      count += Misc.combCount(Misc.popCount(h0),
                              Misc.popCount(h1),
                              Misc.popCount(h01),
                              hs_tmp[u0], hs_tmp[u1]);
    }

    if (testMatch()) {
      Misc.msg("\nhs[0]: " + hs_tmp[0] +
               " hs[1]: " + hs_tmp[1] +
               " hs[2]: " + hs_tmp[2] + " u0: " + u0 + " u1: " + u1);
    
      Misc.msg("hk: "   + Misc.popCount(hk) + " " + Hand.toStringColor(hk, 3, false) +
               "\nav:   " + Hand.toStringColor(avail, 3, false) +
               "\nh0: " + hs_tmp[u0] + " " + Hand.toStringColor(h0, 3, false) +
               "\nh1: " + hs_tmp[u1] + " " + Hand.toStringColor(h1, 3, false) +
               "\nh01: " + Hand.toStringColor(h01, 3, false) + "\n# " + count);
    }

    return count;
  }

  //-------------------------------------------------------------------

  /** given the index, and the number of cards to allocate to p0,
      compute the partition and add the cards to the appropriate hands
  */
  static void consistent(int id, int n0, int h01, int[] h)
  {
    int n = Misc.popCount(h01);
    if (n0 > n) Misc.err("foo");
    assert Misc.choose(n, n0) > id;

    for (;;) {
      if (h01 == 0) return;
      int m = h01 - (h01 & (h01-1)); // extract lowest order bit
      //int c = getOnBitFast(h01);
      //int m = 1 << c;

      int a = Misc.choose(n-1, n0); // number of worlds where we don't give this card to p0
      //      Misc.msg("a= " + a + " " + (n-1) + " " + n0);
      //    uint8 b = choose(n-1, n0-1);
      //    cout << "a,b,id = " << a << " " << b << " " << id << endl;

      if (id < a) { // give the card to p1
        h[1] |= m;
      } else {
        h[0] |= m;
        --n0;
        id -= a;
      }
      h01 ^= m;
      --n;
    }
  }

  //...................................................................

  // assumes hand with index known_pos must be known
  public void consistent(SimpleState c, int knownPos, int id)
  {
    assert phase == CARDPLAY;
    int[] hs_tmp = new int[3];
    
    c.inPlaceCopy(this);
    
    boolean knownSkat = !decl.hand;
    getHandSizes(hs_tmp);
    
    // players with unknown cards
    int u0 = (knownPos+1) % 3; 
    int u1 = (knownPos+2) % 3;
    
    int hk = getHand(knownPos);
    int allPlayed = getPlayedCards(0) | getPlayedCards(1) | getPlayedCards(2);
    int avail = ~(allPlayed | hk);
    
    if (knownSkat) {
      int s = getSkat();
      if (s == 0) Misc.err("unknown skat");
      avail &= ~s;
    }
    
    int h0 = avail & getVoidPossible(u0);
    int h1 = avail & getVoidPossible(u1);
    int h01 = h0 & h1;
    h0 &= ~h01;
    h1 &= ~h01;

    int count = 0;

    c.setSkat(getSkat());

    if (!knownSkat) {
      int newSkat = 0;

      for (int i=0; i < 32; i++) {
        int c0 = 1 << i;
        if ((avail & c0) == 0) continue;
        for (int j=0; j < i; j++) {
          int c1 = 1 << j;
          if ((avail & c1) == 0) continue;
          int hh0  = h0  & ~(c0 | c1);
          int hh1  = h1  & ~(c0 | c1);
          int hh01 = h01 & ~(c0 | c1);
        
          int inc = Misc.combCount(Misc.popCount(hh0),
                                   Misc.popCount(hh1),
                                   Misc.popCount(hh01),
                                   hs_tmp[u0], hs_tmp[u1]);
          if (count + inc > id) {
            id -= count;
            newSkat = c0 | c1;
            knownSkat = true;
            break;
          }
          count += inc;
        }
        if (knownSkat) break;
      }

      c.setSkat(newSkat);
    }

    int s = c.getSkat();
    h0  &= ~s;
    h1  &= ~s;
    h01 &= ~s;

    if (testMatch()) {
      Misc.msg("hs[0]: " + hs_tmp[0] +
               " hs[1]: " + hs_tmp[1] +
               " hs[2]: " + hs_tmp[2] + " u0: " + u0 + " u1: " + u1);
    
      Misc.msg("hk: "  + Hand.toStringColor(hk, 3, false) +
               "\nav: "  + Hand.toStringColor(avail, 3, false) +
               "\nh0: " + Hand.toStringColor(h0, 3, false) +
               "\nh1: " + Hand.toStringColor(h1, 3, false) +
               "\nh01: " + Hand.toStringColor(h01, 3, false) + "\n\n");
    }

    int[] res_tmp = { h0, h1 };
    consistent(id, hs_tmp[u0] - Misc.popCount(h0), h01, res_tmp);
    c.setHand(u0, res_tmp[0]);
    c.setHand(u1, res_tmp[1]);

    if (false && testMatch()) {
      Misc.msg(id + " h0: " + Hand.toStringColor(c.getHand(u0), 3, false) +
               " h1: " + Hand.toStringColor(c.getHand(u1), 3, false) +
               " sk: " + Hand.toStringColor(c.getSkat(), 3, false));
    }

    assert Misc.popCount(c.getHand(0)) == hs_tmp[0] : "" + Misc.popCount(c.getHand(0)) + " " + hs_tmp[0];
    assert Misc.popCount(c.getHand(1)) == hs_tmp[1] : "" + Misc.popCount(c.getHand(1)) + " " + hs_tmp[1];
    assert Misc.popCount(c.getHand(2)) == hs_tmp[2] : "" + Misc.popCount(c.getHand(2)) + " " + hs_tmp[2];
  }

  //-------------------------------------------------------------------

  // get hand sizes assuming player to move knows his hand
  void getHandSizes(int[] hs)
  {
    hs[0] = hs[1] = hs[2] = Misc.popCount(getHand(toMove));

    assert hs[0] != 0;

    if (trickCardNum < 1 || trickCardNum > 2) return;
    
    int q = toMove;
    for (int i=0; i < trickCardNum; i++) {
      q = (q+2) % 3;
      --hs[q];
    }

    return;
  }

  static void testRnd(Random rgen)
  {
    for (int i=0; i < 1000000; i++) {
      Misc.msg("" + rndInt(rgen, 20) + "=");
    }
    System.exit(0);
  }

  public static void testCounting(SimpleGame g)
  {
    test_case++;

    GameDeclaration gd = g.getGameDeclaration();
    
    if (!g.isFinished() || g.getDeclarer() < 0 || gd.trumpSuit() < 0 ||
        !gd.hand)
      return;
        
    SimpleState nst = new SimpleState(0);
    
    for (int t=0; t < 10; t++) {
      for (int c=0; c < 3; c++) {
        int index = g.getTrickCardIndex(t, c);
        if (index < 0) return;

        SimpleState st = g.getState(index);
        int tm = st.getToMove();

        if (t < 7) continue;
        
        int cc = st.numConsistent(tm);
        Misc.msg("#cons= " + cc);

        if (true) continue;

        
        Misc.msg(">>>>>>>>>>>>>>>>>>>>>> # " + test_case + " t= " + t + " c= " + c);
        test_t = t;
        test_c = c;
        
        Misc.msg("mv=" + g.getMoveHist().get(index) + " | " + tm);

        if (true || testMatch()) {
          Misc.msg(st.toString(true));
        }

        long[] all = new long[cc];
        
        for (int i=0; i < cc; i++) {

          if ((i % 100000) == 0) Misc.msg("" + i);
          
          st.consistent(nst, tm, i);
          int u0 = (tm+1) % 3;
          int u1 = (tm+2) % 3;
          int h0 = nst.getHand(u0);
          int h1 = nst.getHand(u1);          
          int ac = h0 | h1 | nst.getHand(tm) | nst.getSkat() |
            nst.getPlayedCards(0) | nst.getPlayedCards(1) | nst.getPlayedCards(2);
          if (Misc.popCount(ac) != 32) Misc.msg("!= 32");

          long both = ((long)h0 << 32) | (((long)h1) & 0xffffffffL);
          //Misc.msg("b: " + both);
          all[i] = both;          
        }

        Arrays.sort(all);

        for (int i=1; i < cc; i++) {
          if (all[i] == all[i-1]) {
            Misc.err("duplicate");
          }
        }
      }
    }
  }

  static int test_case = 0, test_t = 0, test_c = 0;

  static boolean testMatch() { return false && (test_case == 1 && test_t == 6 && test_c == 2); }
}
