/*
 * Card.java
 *
 * (c) Michael Buro, licensed under GPLv3
 */

package common;

import java.io.*;
import java.util.*;

public class Card implements Serializable
{
  // read-only class
  
  private final int suit; // NO_SUIT = -1, DIAMONDS = 0, HEARTS = 1, SPADES = 2, CLUBS = 3
  private final int rank;

  final static String[][] cardStrings = new String[4][8];
  final static Card[][] allCards = new Card[4][8];
  public final static Card unknownCard = new Card();

  final static int CARD_HASH_N = 256; // power of 2 assumed
  final static Card[] cardHash = new Card[CARD_HASH_N];
    
  public Card() { suit = rank = -1; }
  
  public Card(int suit, int rank) 
  {
    this.suit = suit;
    this.rank = rank;
  }

  public static Card newCard(int suit, int rank) {
    if (suit < 0) return unknownCard;
    return allCards[suit][rank];
  }
  
  public final int getSuit() { return suit; }
  public final int getRank() { return rank; }  

  public final int getIndex() { return (suit << 3) + rank; }

  // return 0..7 for 789TQKA
  public final int getSideSuitRank()
  {
    assert rank != RANK_JACK;
    
    if (rank == RANK_ACE) return RANK_JACK;
    return rank;
  }

  static public int rankFromSideSuitRank(int ssCardRank)
  {
    if (ssCardRank == RANK_JACK)
      return RANK_ACE;

    return ssCardRank;
  }
  
  
  // 789QKTAJJJJ -> 0..10  or JJJJ -> 0..3
  public final int getTrumpSuitRank(int trumpSuit) {
    if (trumpSuit < 4) {
      int i = suitRanks[rank];
      if (i == 7) { // jack
	      return jackRanks[suit] + 7;
      }
      return i;
    } else {
      // grand
      if (rank != RANK_JACK) Misc.err("no jack");
      return suit;
    }
  }

  // 0..10,trump -> card
  public static Card fromTrumpSuitRank(int r, int trumpSuit) {
    int i = suitTrumpIndexes[0][r];
    return allCards[i >> 3][i & 7];
  }
  
  static public Card copy(Card c) {
    return c; // cards are read-only
  }
  
  public static final String unknownCardString = "??"; // displayed when card is unknown
  public static final String cardSep = ".";  // card separator when storing card vector
  public static final String handSep = "|";  // hand separator when storing card vector
  // change split() below accordingly and the following strings!!!
  public static final String unknownHand = "??.??.??.??.??.??.??.??.??.??"; // cardVectorToStr
  public static final String unknownSkat = "??.??";
  
  public static final char[] suitNames = { 'D', 'H', 'S', 'C' }; // ordered by value
  public static final char[] rankNames = { '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };

  public static final int[] nullRanks = {  0,   1,   2,   3,   4,   5,   6,   7 };
  public static final int[] suitRanks = {  0,   1,   2,   5,   7,   3,   4,   6 };
  public static final int[] strengthOrder = {  0,   1,   2,   5,   6,   3,   7,   4 };
  public static final int[] jackRanks = {  0,   1,   2,   3 };  // by suit
  public static final int[] points     = {  0,   0,   0,  10,   2,   3,   4,  11 };  

  public static final int[][] suitTrumpIndexes = {
    { 0   , 1   , 2   , 5   , 6   , 3   , 7   ,  4, 4+8, 4+16, 4+24 }, // 789QKTA JJJJ diamonds
    { 0+8 , 1+8 , 2+8 , 5+8 , 6+8 , 3+8 , 7+8 ,  4, 4+8, 4+16, 4+24 }, // 789QKTA JJJJ hearts
    { 0+16, 1+16, 2+16, 5+16, 6+16, 3+16, 7+16,  4, 4+8, 4+16, 4+24 }, // 789QKTA JJJJ spades
    { 0+24, 1+24, 2+24, 5+24, 6+24, 3+24, 7+24,  4, 4+8, 4+16, 4+24 }, // 789QKTA JJJJ clubs
    { 4, 4+8, 4+16, 4+24 } // JJJJ for grand
  };
    
  public static final int[] fg =  { 0, 7, 0, 7 } ;
  public static final int[] bg =  { 3, 1, 2, 0 } ;
  public static final String[] fgHTML =  { "orange", "red", "limegreen", "black" };
  public static final String[] suitHTML =  { "&diams;", "&hearts;", "&spades;", "&clubs;" };
  
  // fixme: Java's enumerations are horrible, but this isn't great either.  We need
  // mappings to integer values for array lookups but Java "helpfully" hides that from
  // us and using an EnumMap seems like overkill.  Maybe there's a happy medium....

  public static final int SUIT_NONE     = -1;
  public static final int SUIT_DIAMONDS = 0;
  public static final int SUIT_HEARTS   = 1;
  public static final int SUIT_SPADES   = 2;
  public static final int SUIT_CLUBS    = 3;

  public static final int RANK_NONE  = -1;
  public static final int RANK_SEVEN = 0;
  public static final int RANK_EIGHT = 1;
  public static final int RANK_NINE  = 2;
  public static final int RANK_TEN   = 3;
  public static final int RANK_JACK  = 4;
  public static final int RANK_QUEEN = 5;
  public static final int RANK_KING  = 6;
  public static final int RANK_ACE   = 7;

  public static final int CJ_BIT = 1 << (24+RANK_JACK);
  
  public static final int JACK_MASK =
    (1 << RANK_JACK) | (1 << (8+RANK_JACK)) |
    (1 << (16+RANK_JACK)) | (1 << (24+RANK_JACK));

  public static final int ACE_MASK =
    (1 << RANK_ACE)      | (1 << (8+RANK_ACE)) |
    (1 << (16+RANK_ACE)) | (1 << (24+RANK_ACE));

  public static final int TEN_MASK =
    (1 << RANK_TEN)      | (1 << (8+RANK_TEN)) |
    (1 << (16+RANK_TEN)) | (1 << (24+RANK_TEN));

  public static final int KING_MASK =
    (1 << RANK_KING)      | (1 << (8+RANK_KING)) |
    (1 << (16+RANK_KING)) | (1 << (24+RANK_KING));

  public static final int QUEEN_MASK =
    (1 << RANK_QUEEN)      | (1 << (8+RANK_QUEEN)) |
    (1 << (16+RANK_QUEEN)) | (1 << (24+RANK_QUEEN));

  public static final int NINE_MASK =
    (1 << RANK_NINE)      | (1 << (8+RANK_NINE)) |
    (1 << (16+RANK_NINE)) | (1 << (24+RANK_NINE));

  public static final int EIGHT_MASK =
    (1 << RANK_EIGHT)      | (1 << (8+RANK_EIGHT)) |
    (1 << (16+RANK_EIGHT)) | (1 << (24+RANK_EIGHT));

  public static final int SEVEN_MASK =
    (1 << RANK_SEVEN)      | (1 << (8+RANK_SEVEN)) |
    (1 << (16+RANK_SEVEN)) | (1 << (24+RANK_SEVEN));

  public static final int HIGH_CARD_MASK = ACE_MASK | TEN_MASK;
  
  public static final int QUEEN_KING_MASK = QUEEN_MASK | KING_MASK;

  public static final int LOW_CARD_MASK = SEVEN_MASK | EIGHT_MASK | NINE_MASK;

  public static final int NO_JACK_SUIT_MASK = 0xff - (1 << RANK_JACK);
  public static final int[] SUIT_MASK = new int[] { 255, 255 << 8, 255 << 16, 255 << 24 };
  public static final int[] SUIT_POS  = new int[] { 0, 8, 16, 24 };  
  public static final int[] JACK_POS  = new int[] { RANK_JACK, 8+RANK_JACK,
						    16+RANK_JACK, 24+RANK_JACK };    
  public static final String SKATGUI_PREFIX = "!";

  /**
   *  @return true iff card is known
   */
  public boolean isKnown() { 
    return suit >= 0;
  }

  public static void serialize(Card c, StringBuffer sb)
  {
    sb.append("\nCard");
    if (c == null) { sb.append("Null "); return; }
    sb.append(" " + c.suit+" ");
    sb.append(c.rank+" ");    
  }

  public int toBit()
  {
    if (!isKnown()) return 0;
    return 1 << ((suit << 3) + rank);
  }
  
  public String toString()
  {
    if (isKnown())
      return cardStrings[suit][rank];
    return unknownCardString;
  }

//   /** Creates a card's value from a numeric index.
//   */
//   public void fromIndex(int index) { 
//     suit = index / 8; 
//     rank = index % 8; 
//   }

  public static String suitColorWrite(int suit, String toprint)
  {
    if (suit < 0) return unknownCardString;

      return  "\u001B[0;" + (fg[suit]+30) + ";" + (bg[suit]+40) + "m" + toprint
      + "\u001B[m";
  }

  public String skatguiWrite()
  {
    return "" + SKATGUI_PREFIX + " " + suitNames[suit] + rankNames[rank] + "\n";
  }
    
  static public String toColor(int suit)
  {
    return "\u001B[0;" + (fg[suit]+30) + ";" + (bg[suit]+40) + "m " + " " + "\u001B[m";
  }
  
  public String toStringColor(boolean html)
  {
    // os << char(0x1b) << "[2;" + (fg[s]+30) + ';' + (bg[s]+40) + 'm'; // choose color

    if (suit < 0) return unknownCardString;

    if (html) {
      return "<font color=" +  fgHTML[suit] + ">" + " " + rankNames[rank] + "</font>";
      //return "<font color=" + fgHTML[suit] + ">" + suitHTML[suit] + "</font>" + rankNames[rank];      
    } else
      return "\u001B[0;" + (fg[suit]+30) + ";" + (bg[suit]+40) + "m " +
        rankNames[rank] +
        "\u001B[m";
    // os << char(0x1b) << "[0m"; // reset
  }

  public String toStringColor()
  {
    return toStringColor(false);
  }

  public boolean equals(Object o)
  {
    if (o == null || !(o instanceof Card))
      return false;

    Card co = (Card)o;
    
    return suit == co.suit && rank == co.rank;
  }

  
  public int value()
  {
    return points[rank];
  }

  public static int value(int rank)
  {
    return points[rank];
  }
  
  /**
   *  @return null if incorrect, card otherwise
   */
  static public Card fromString(String s)
  {
    if (s.length() != 2)
      return null;

    // fixme: this is fast but inaccurate
    // many random strings will pass as valid cards
    return cardHash[cardStringHash(s)];

  }

  static public Card fromStringAccurate(String s)
  {
    if (s.length() != 2)
      return null;

    if (s.equals(unknownCardString))
      return unknownCard;
    
    // read suit
    
    char c1 = s.charAt(0);
    int suit;
    for (suit=0; suit < 4; ++suit)
      if (suitNames[suit] == c1)
        break;

    if (suit >= 4)
      return null;

    // read rank
    
    char c2 = s.charAt(1);
    int rank;
    for (rank=0; rank < 8; ++rank)
      if (rankNames[rank] == c2)
        break;

    if (rank >= 8)
      return null;

    //    if (cardHash[cardStringHash(s)] != allCards[suit][rank])
    //  Misc.err("!equal");
    
    return allCards[suit][rank];
  }

  /**
   *  @return null if incorrect, card otherwise
   */
  static public Card fromIndex(int ind)
  {
    assert ind >= 0 && ind < 32 : "index " + ind;

    return allCards[ind >> 3][ind & 7];
  }


  static public int index2Rank(int ind)
  {
    return ind & 7;
  }

  static public int index2Suit(int ind)
  {
    return ind >> 3;
  }

  /**
   * creates string from card vector.
   * cards are separated by .
   * @return string representing card vector
   */
  static public String cardListToString(ArrayList<Card> cards)
  {
    StringBuffer sb = new StringBuffer();
    int n = cards.size();

    for (int i=0; i < n; ++i) {
      sb.append(cards.get(i).toString());
      if (i < n-1)
        sb.append(cardSep);
    }

    return sb.toString();
  }


  static public ArrayList<Card> cardArrayToList(int mn, Card[] cards)
  {
    ArrayList<Card> cards2 = new ArrayList<Card>();
    for (int i=0; i < mn; i++) {
      cards2.add(cards[i]);
    }
    return cards2;
  }

  
  /**
   * creates card vector from string. delimters:  . _/
   * @return null on success; error msg otherwise
   */
  static public ArrayList<Card> cardListFromString(String s)
  {
    ArrayList<Card> cards = new ArrayList<Card>();
    String[] parts = s.split("\\.|\\|"); // refers to separators above!

    for (int i = 0; i < parts.length; ++i) {
      Card card = Card.fromString(parts[i]);
      if (card == null) Misc.err("invalid card " + parts[i]);
      cards.add(card);
    }
    return cards;
  }

  public static int getSuit(char c) {

    if (c == 'D') {
      return Card.SUIT_DIAMONDS;
    }
    else if (c == 'H') {
      return Card.SUIT_HEARTS;
    }
    else if (c == 'S') {
      return Card.SUIT_SPADES;
    }
    else if (c == 'C') {
      return Card.SUIT_CLUBS;
    }
    else {
      return -1;
    }  

  }

  public static int cardStringHash(String t)
  {
    return ((int)t.charAt(0) ^ (int)t.charAt(1) << 2) & (CARD_HASH_N-1);
  }
  
  static {

    // generate cards and card strings

    for (int s=0; s < 4; s++) {
      for (int r=0; r < 8; r++) {
        Card c = new Card(s, r);
        allCards[s][r] = c;
	cardStrings[s][r] = ""+ Card.suitNames[s] + Card.rankNames[r];

        int hash = cardStringHash(c.toString());
        if (cardHash[hash] != null) {
          Misc.err("entry occupied");
        }
        cardHash[hash] = c;
      }
    }

    int hash = cardStringHash(unknownCard.toString());
    if (cardHash[hash] != null) {
      Misc.err("entry occupied");
    }
    cardHash[hash] = unknownCard;
  }
  
}


class GreaterCard implements Comparator<Card>
{
  public int compare(Card first, Card second)
  {
    int r = first.getSuit() - second.getSuit();
    if (r != 0)
      return r;
    return first.getRank() - second.getRank();
  }
}

