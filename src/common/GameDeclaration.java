/*
 * GameDeclaration.java
 *
 * (c) Michael Buro, licensed under GPLv3
 *
 */

package common;

import java.io.*;
import java.text.*;
import java.util.*;
//import javax.swing.plaf.*;

public class GameDeclaration implements Serializable
{
  // don't change  
  public static final int NO_GAME = Card.SUIT_NONE;
  public static final int DIAMONDS_GAME = Card.SUIT_DIAMONDS; 
  public static final int HEARTS_GAME = Card.SUIT_HEARTS;
  public static final int SPADES_GAME = Card.SUIT_SPADES;
  public static final int CLUBS_GAME = Card.SUIT_CLUBS;
  public static final int GRAND_GAME = Card.SUIT_CLUBS + 1;
  public static final int NULL_GAME = Card.SUIT_CLUBS + 2;

  // only needed in Eval12Suit (for hand game declaration)
  public static final int NULL_OUVERT_GAME = Card.SUIT_CLUBS + 3;  

  public static final String[] gameNames = { "D", "H", "S", "C", "G", "N" }; // ordered by value

  public static final int[] nullVals = { 23, 35, 46, 59 };

  //long p1,p2,p3,p4,p5,p6,p7,p8; // mitigates false sharing!  
  public int type;
  public boolean hand, ouvert, schneiderAnnounced, schwarzAnnounced;
  //long q1,q2,q3,q4,q5,q6,q7,q8; // mitigates false sharing!

  public void reset()
  {
    type = NO_GAME;
    ouvert = schneiderAnnounced = schwarzAnnounced = false;
    hand = true; // Jeff: I initialize this to true, as it's needed
                 // for the hashValue; also, it makes more sense that
                 // an 'empty' game declaration is indeed hand
  }

  public GameDeclaration()
  {
    reset();
  }

  public GameDeclaration(int type, boolean hand, boolean ouvert,
			 boolean schneiderAnnounced, boolean schwarzAnnounced)
  {
    // Misc.msg("TYPE = " + type);

    this.type = type;
    this.hand = hand;
    this.ouvert = ouvert;
    this.schneiderAnnounced = schneiderAnnounced;
    this.schwarzAnnounced = schwarzAnnounced;
  }
			 
  public void copy2(GameDeclaration d)
  {
    type = d.type;
    hand = d.hand;
    ouvert = d.ouvert;
    schneiderAnnounced = d.schneiderAnnounced;
    schwarzAnnounced = d.schwarzAnnounced;
  }

  static public GameDeclaration copy(GameDeclaration d)
  {
    if (d == null) return null;
    
    GameDeclaration x = new GameDeclaration();
    x.copy2(d);
    return x;
  }

  public boolean equal(GameDeclaration d)
  {
    if (type != d.type) return false;
    if (hand != d.hand) return false;
    if (ouvert != d.ouvert) return false;
    if (schneiderAnnounced != d.schneiderAnnounced) return false;
    if (schwarzAnnounced != d.schwarzAnnounced) return false;
    return true;
  }
  
  public int trumpSuit()
  {
    if (type == GRAND_GAME || type == NULL_GAME || type == NO_GAME)
      return -1;

    else return type;
  }

  public static int trumpSuit(int gameType)
  {
    if (gameType == GRAND_GAME || gameType == NULL_GAME || gameType == NO_GAME)
      return -1;

    else return gameType;
  }

  /** @return The suit of a card in the context of the current game type (4 for Jacks in Grand game)
  */
  public int tellCardSuit(Card c) {
    if (c.getRank() == Card.RANK_JACK) {
      if (type == GRAND_GAME || type == NO_GAME) {
        return 4;
      }
      else if (type == NULL_GAME) {
        return c.getSuit();
      }
      else {
      // Suit game
        return type;
      }
    }
    else {
      return c.getSuit();
    }
  }

  public static void serialize(GameDeclaration d, StringBuffer sb)
  {
    sb.append("\nGameDeclaration");
    if (d == null) { sb.append("Null "); return; }
    sb.append(" " + d.type + " ");
    sb.append(d.hand + " ");
    sb.append(d.ouvert  + " ");
    sb.append(d.schneiderAnnounced + " ");
    sb.append(d.schwarzAnnounced + " ");
  }

  public static String typeToStringColor(int type)
  {
    if (type == GRAND_GAME) return "G";
    if (type == NULL_GAME)  return "N";
    return Card.suitColorWrite(type, " ");
  }

  public static String typeToChar(int type)
  {
    switch(type) {
    case DIAMONDS_GAME: return "D";
    case HEARTS_GAME:   return "H";
    case SPADES_GAME:   return "S";
    case CLUBS_GAME:    return "C";
    case GRAND_GAME:    return "G";
    case NULL_GAME:     return "N";
    }
    return "?";
  }

  // @return game type
  public static int typeFromChar(String s)
  {
    if      (s.equals("D"))
      return DIAMONDS_GAME;
    else if (s.equals("H"))
      return HEARTS_GAME;
    else if (s.equals("S"))
      return SPADES_GAME;
    else if (s.equals("C"))
      return CLUBS_GAME;
    else if (s.equals("N"))
      return NULL_GAME;
    else if (s.equals("G"))
      return GRAND_GAME;
    else
      return NO_GAME;
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer();
    
    switch (type) {

    case DIAMONDS_GAME: s.append("D"); break;
    case HEARTS_GAME:   s.append("H"); break;
    case SPADES_GAME:   s.append("S"); break;
    case CLUBS_GAME:    s.append("C"); break;
    case GRAND_GAME:    s.append("G"); break;      
    case NULL_GAME:     s.append("N"); break;
    default:            s.append("?"); break;
    }

    for (;;) {
      if (ouvert) {
        s.append("O");
        if (type != NULL_GAME) break;
      }

      if (hand) 
        s.append("H");

      if (type == NULL_GAME) break;

      if (schwarzAnnounced) {
        s.append("Z");
        break;
      }

      if (schneiderAnnounced)
        s.append("S");

      break;
    }

    return s.toString();
  }

  public String typeToVerboseString()
  {
      if (type == GRAND_GAME) return "Grand";
      if (type == NULL_GAME) return "Null";

    switch (type) {
    case DIAMONDS_GAME: return "Diamonds";
    case HEARTS_GAME: return "Hearts";
    case SPADES_GAME: return "Spades";
    case CLUBS_GAME: return "Clubs";
    }

    return "?";
  }

  /**
     @return The game modifiers in human-readable form. leading space
  */
  public String modifiersToVerboseString()
  {
    StringBuffer s = new StringBuffer();

    for (;;) {
      
      if (ouvert) {
	  s.append(" Ouvert");
        if (type != NULL_GAME) break;
      }
      
      if ((!ouvert || type == NULL_GAME) && hand)
        s.append(" Hand");
      
      if (type == NULL_GAME) break;

      if (schwarzAnnounced) {
	  s.append(" Schwarz");
        break;
      }

      if (!schwarzAnnounced && schneiderAnnounced)
        s.append(" Schneider");

      break;
    }

    return s.toString();
  }

  
  /**
     @return null if correct, error message otherwise
  */
  public String fromString(String s)
  {
    StringCharacterIterator sci = new StringCharacterIterator(s);
    char c = sci.first();

    ouvert = hand = schneiderAnnounced = schwarzAnnounced = false;
    
    if (c == CharacterIterator.DONE) return "empty";

    switch (c) {

    case 'D': type = DIAMONDS_GAME; break;
    case 'H': type = HEARTS_GAME; break;
    case 'S': type = SPADES_GAME; break;
    case 'C': type = CLUBS_GAME; break;
    case 'G': type = GRAND_GAME; break;      
    case 'N': type = NULL_GAME; break;

    default: return "type '" + c + "' not recognized";
    }

    for (;;) {
      c = sci.next();
      if (c == CharacterIterator.DONE) break;

      switch (c) {
      case 'O': ouvert = true; continue;
      case 'H': hand = true; continue;
      case 'S': schneiderAnnounced = true; continue;
      case 'Z': schwarzAnnounced = true; continue;
      default: return "class '" + c + "' not recognized";
      }
    }

    if (ouvert && type != NULL_GAME) {
      hand = schneiderAnnounced = schwarzAnnounced = true;
    }
    
    if (schwarzAnnounced)
      schneiderAnnounced = true;

    if (type == NULL_GAME && (schneiderAnnounced || schwarzAnnounced))
      return "null game but schneider or schwarz announced";

    if (!hand && schneiderAnnounced)
      return "not hand but schneider or schwarz announced";

    return null;
  }


  public static int trumpMultiplier(int h, int type)
  {
    if (type == NO_GAME || type == NULL_GAME)
      Misc.err("no trump type");
    
    int m = 2;
    boolean with = Hand.has(h, Card.SUIT_CLUBS, Card.RANK_JACK);
    
    if (with == Hand.has(h, Card.SUIT_SPADES, Card.RANK_JACK)) {
      ++m;
      if (with == Hand.has(h, Card.SUIT_HEARTS, Card.RANK_JACK)) {
        ++m;
        if (with == Hand.has(h, Card.SUIT_DIAMONDS, Card.RANK_JACK)) {
          ++m;
        }}}
    
    // Continue counting trumps if we can.
    if (m == 5 && type != GRAND_GAME) {
      if (with == Hand.has(h, type, Card.RANK_ACE)) {
        ++m;
        if (with == Hand.has(h, type, Card.RANK_TEN)) {
          ++m;
          if (with == Hand.has(h, type, Card.RANK_KING)) {
            ++m;
            if (with == Hand.has(h, type, Card.RANK_QUEEN)) {
              ++m;
              if (with == Hand.has(h, type, Card.RANK_NINE)) {
                ++m;
                if (with == Hand.has(h, type, Card.RANK_EIGHT)) {
                  ++m;
                  if (with == Hand.has(h, type, Card.RANK_SEVEN)) {
                    ++m; // With 11!  Holy crap!
                  }}}}}}}}
    return m;
  }

  // Returns the game value of a given game type for the declarer given a 12-card hand
  public static int staticWinValue(int h, int type, boolean hand, boolean ouvert) {
    if (type == GameDeclaration.NULL_GAME) {
      int index = 0;
      if (hand && ouvert) {
        index = 3;
      }
      else if (ouvert) {
        index = 2;
      }
      else if (hand) {
        index = 1;
      }
      return GameDeclaration.nullVals[index];
    }
    
    int mult = GameDeclaration.trumpMultiplier(h, type);
    if (hand) mult++;
    return mult * GameDeclaration.baseValue(type);

  }  

  // Returns the win-value for the declarer of this game declaration given a hand and skat.
  public int winValue(int h, int skat) {
    if (type == GameDeclaration.NULL_GAME) {
      int index = 0;
      if (this.hand && this.ouvert) {
        index = 3;
      }
      else if (this.ouvert) {
        index = 2;
      }
      else if (this.hand) {
        index = 1;
      }
      return GameDeclaration.nullVals[index];
    }
    
    int mult = GameDeclaration.trumpMultiplier(h ^ skat, type);
    if (this.hand) mult++;
    return mult * GameDeclaration.baseValue(type);

  }

  /** @return store value,overbid,matadors in game result */
  public void value(int cards, Card skat1, Card skat2,
                    int points, int declTricks, int bid, boolean declLoss,
		    int allTricks,
                    GameResult gr)
  {
    // Misc.msg("PTS=" + points + " T=" + tricks + " B=" + bid + " R=" + declResigned);

    gr.overbid = false;
    gr.declValue = 0;
    gr.matadors = 0;
    gr.schneider = gr.schwarz = false;
    
    // Sanity checks
    if (bid < 18) throw new RuntimeException("in GameDeclaration.value(): bid less than 18");
    
    // Null games are simple to calculate.
    if (type == NULL_GAME) {

      int v = nullVals[(ouvert? 2 : 0) + (hand? 1 : 0)];
      
      if (declTricks > 0 || declLoss) {
        gr.declValue = -2*v;
      } else {
        gr.declValue = v;
      }
      return;
    }

    // Count matadors

    int h = cards;

    if (skat1.isKnown()) {
      h = Hand.set(h, skat1);
      h = Hand.set(h, skat2);
    }

    int m = trumpMultiplier(h, type);
    boolean with = Hand.has(h, Card.SUIT_CLUBS, Card.RANK_JACK);

    if (with) gr.matadors =   m-1;
    else      gr.matadors = -(m-1);
    
    // But after all that, did we win?
    boolean won = points >= 61;
    
    if (schwarzAnnounced) {
      won = declTricks == 10;
    } else if (schneiderAnnounced) {
      won = points >= 90;
    }
    
    if (hand) ++m;
    if (schneiderAnnounced) ++m;
    if (schwarzAnnounced) ++m;
    if (ouvert) ++m;

    int bv = baseValue(type);
    
    if (won) {
      
      if (points >= 90) {
        ++m; // schneider
        gr.schneider = true;
      }
      if (declTricks == 10) {
        ++m; // schwarz
        gr.schneider = gr.schwarz = true;
      }
        
      // no overbid -> win!
      if (m * bv >= bid) {
        gr.declValue = m * bv;
        return;
      }
      // overbid

    } else {
      
      // lost

      if (!schneiderAnnounced && allTricks >= 1 && points <= 30) {
	++m; // declarer schneider, but no Eigenschneider and no
	     // penalty for resigning during first trick
        gr.schneider = true;
      }

      if (!schwarzAnnounced && allTricks >= 1 && declTricks == 0) {
	++m; // declarer schwarz, but no Eigenschwarz and no penalty
	     // for resigning during first trick
        gr.schneider = gr.schwarz = true;
      }

      if (m * bv >= bid) {
        gr.declValue = - 2 * m * bv;
        return;
      }

      // overbid
    }
    
    // overbid

    gr.overbid = true;
    int v = ((bid+bv-1)/bv) * bv; // smallest game value >= bid
    gr.declValue = -2 * v;
  }
  

  static public int baseValue(int t)
  {
    switch (t) {
    case DIAMONDS_GAME: return 9;
    case HEARTS_GAME:   return 10;
    case SPADES_GAME:   return 11;
    case CLUBS_GAME:    return 12;
    case NULL_GAME:     return 23;
    case GRAND_GAME:    return 24;
    }
    return -1;
  }
  
}

