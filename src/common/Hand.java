/** Collection of static function acting on hand represented as a bit
 * array
 */

package common;

import java.io.*;
import java.util.*;


public class Hand implements Serializable
{
  public Hand()
  {
    Misc.err("Hand is a static class");
  }
  
  /** Removes a particular card from the hand. */
  static public int clear(int hand, int s, int r)
  {
    assert(r >= 0 && r < 8);
    assert(s >= 0 && s < 4);
    // assert((hand & (s*8+r << r)) != 0);
    return hand & ~(1 << (s*8+r));
  }

  static public int clear(int hand, Card c) { return clear(hand, c.getSuit(), c.getRank()); }

  static public int clear(int hand, int hand2) { return hand & ~hand2; }

  /** @return a given suit in its original bit location, with a boolean indicating if that card is trump
  */
  static public int getSuitOrigLocation(int h, int s, boolean trump) {
    int mask = 255 << (8 * s);
    if (trump) {
      mask ^= Card.JACK_MASK;
    }
    else {
      mask &= ~Card.JACK_MASK;
    }

    return (h & mask);

  }

  /** @return a given suit as an 8-bit number in the 8 lowest bits of
   * an int
   */
  static public int getSuit(int h, int s) {
    return (h >>> (s << 3)) & 255;
  }

  /** @return a suit from the given hand, depending on whether or not
   * it is trump.
   */
  static public int getSuit(int h, int s, boolean trump) {
    int horig = h;
    h = Hand.getSuit(h, s) &  ~Card.JACK_MASK; // exclude jacks
    if (!trump) {
      return h;
    } else {
      return h | (Card.JACK_MASK & horig); // include jacks
      // Doesn't really work here right now, as jacks are still in their 'original' spots.
      // - fixed. /MB
    }
  }

  /* @return the same non-suited hand, but rearranged by suitRanks in bit-order
  */
  static public int sortByRank(int h) {
    int newHand = 0;
    for (int i = 0; i < 8; i++) {
      if ( ((1 << i) & h) != 0) { 
        newHand ^= (1 << Card.suitRanks[i]);
      }
    }

    return newHand;

  }

  /** @return the same non-suited hand, but rearranged so that the 10 is ahead of
   * the King and Queen
   */
  static public int rearrange(int h) {
    int answer = h;
    if (Hand.has(h, 0, Card.RANK_TEN)) {
      answer = answer | (1 << Card.RANK_KING); // putting the 10 in the King's spot
      answer ^= (1 << Card.RANK_TEN);
    }
    if (Hand.has(h, 0, Card.RANK_JACK)) {
      answer = answer | (1 << Card.RANK_TEN);
      answer ^= (1 << Card.RANK_JACK);
    }
    if (Hand.has(h, 0, Card.RANK_QUEEN)) {
      answer = answer | (1 << Card.RANK_JACK);
      answer ^= (1 << Card.RANK_QUEEN);
    }
    if (Hand.has(h, 0, Card.RANK_KING)) {
      answer = answer | (1 << Card.RANK_QUEEN);
      if (! Hand.has(h, 0, Card.RANK_TEN)) {
        answer ^= (1 << Card.RANK_KING);
      }
    }
    return answer;
  }

  /** @return the jacks as an int */
  static public int getJacks(int h) {
    return h & Card.JACK_MASK;
  }

  /** @return the jack index (0..15) */
  static public int getJackIndex(int h) {
    int index = 0;
    for (int i=0; i < 4; i++) {
      if ((h & (1 << Card.JACK_POS[i])) != 0) index |= (1 << i);
    }
    return index;
  }

  /**
   * @return The number of points contained in the hand (ie. how much all the cards are worth)
   */
  static public int value(int hand) 
  {
    int sum = 0;
  
    for (int suit = 0; suit < 4; suit++) {
      int suitBit = suit*8;
      for (int rank = 0; rank < 8; rank++) {
        if ( (hand & (1<<(suitBit+rank))) != 0) {
          sum += Card.value(rank);
        }
      }
    }
  
    return sum;
  }

   
  /** Adds a card to the hand.
   * @return true on success; false on invalid card or card is already in the hand. */
  static public int set(int hand, int s, int r)
  {
    assert(r >= 0 && r < 8);
    assert(s >= 0 && s < 4);
    return hand | (1 << (s*8+r));
  }

  
  static public int set(int hand, Card c) { return set(hand, c.getSuit(), c.getRank()); }

  static public boolean has(int hand, Card c)
  {
    return has(hand, c.getSuit(), c.getRank());
  }
  
  static public boolean has(int hand, int s, int r)
  {
    assert(r >= 0 && r < 8);
    assert(s >= 0 && s < 4);
    return (hand & (1 << (s*8+r))) != 0;
  }

  /** @return the hand designated by code, given that the card in cardsOut are known not to be in the hand
   */
  static public int getHandFromCode(int cardsOut, int code) {

    int codeindex = 0;
    int h = 0;
    for (int i = 0; i < 32; i++) {
      if ( (cardsOut & (1 << i)) == 0) {
        if ( (code & (1 << codeindex)) != 0) {
          h = h | (1 << codeindex);
        }
        codeindex++;
      }
    }

    return h;

  }


  /** @return 7-bit index for chosen suit */
  public static int sideSuitToIndex(int hand, int suit)
  {
    return sideSuitToIndex((hand >> (8*suit)) & 0xff & ~Card.JACK_MASK);
  }
  
  /** @return suit bits from index (no jacks) */
  public static int indexToSideSuit(int index, int suit)
  {
    return indexToSideSuit(index) << (8*suit);
  }
  
  /** @return 7-bit index from 8-bit suit code without jack */
  public static int sideSuitToIndex(int suitBits)
  {
    if (suitBits < 0 || suitBits >= 256) Misc.err("suitBits out of range : " + suitBits);
    if ((suitBits & Card.JACK_MASK) != 0) Misc.err("jack bit on");
    if ((suitBits & (1 << Card.RANK_ACE)) != 0) {
      suitBits &= ~(1 << Card.RANK_ACE);
      return suitBits |= (1 << Card.RANK_JACK); // use jack bit for ace
    }
    return suitBits;
  }
  
  /** @return 8-bit suit code without jack from 7-bit index */
  public static int indexToSideSuit(int index)
  {
    if (index < 0 || index >= 128) Misc.err("index out of range : " + index);
    if ((index & (1 << Card.RANK_JACK)) != 0) {
      index &= ~(1 << Card.RANK_JACK);       // clear jack bit
      return index |= (1 << Card.RANK_ACE);  // set ace bit
    }
    return index; // no change if JACK not set
  }

  /** @return index of trump suit cards in trumpCards */
  public static int suitTrumpToIndex(int trumpCards, int trumpSuit)
  {
    trumpCards &= Card.JACK_MASK | (0xff << (trumpSuit * 8));

    int index = (trumpCards >> (trumpSuit * 8)) & ~Card.JACK_MASK & 0xff;

    if ((trumpCards & (1 << Card.RANK_JACK)) != 0)
      index |= (1 << Card.RANK_JACK); // diamonds jack
    if ((trumpCards & (0x100 << Card.RANK_JACK)) != 0)
      index |= 0x100; // hearts jack
    if ((trumpCards & (0x10000 << Card.RANK_JACK)) != 0)
      index |= 0x200; // spades jack
    if ((trumpCards & (0x1000000 << Card.RANK_JACK)) != 0)
      index |= 0x400; // clubs jack
    
    return index;
  }

  /** @return trump cards from trump suit card index */
  public static int indexToSuitTrump(int index, int trumpSuit)
  {
    int trump = (index & Card.NO_JACK_SUIT_MASK) << (trumpSuit * 8);

    if ((index & (1 << Card.RANK_JACK)) != 0)
      trump |= (1 << Card.RANK_JACK); // diamonds
    if ((index & 0x100) != 0)
      trump |= (0x100 << Card.RANK_JACK); // hearts
    if ((index & 0x200) != 0)
      trump |= (0x10000 << Card.RANK_JACK); // spades
    if ((index & 0x400) != 0) 
      trump |= (0x1000000 << Card.RANK_JACK); // clubs
    
    return trump;
  }

  /** @return a uniform random hand of size n sampled from all cards not in cardsOut
  */
  public static int getRandomHand(int cardsOut, int n, Random rng) {
    int cards = -1 ^ cardsOut;
    ArrayList<Card> deck = Hand.toCardList(cards);
    SimpleState.shuffle(deck, rng);
    int hand = 0;
    for (int i = 0; i < n; i++) {
      hand = Hand.set(hand, deck.get(i));
    }
    return hand;

  }

/////////////////////////////////////////////////////////////////////////////////
//  Colex ranking functionality here
////////////////////////////////////////////////////////////////////////////////

  /** @return an array containing the indices of the n 'on-bits' in
   * the hand h
   */
  public static int[] getOnBits(int h, int n) {

    int[] indices = new int[n];
    int index = 0;
    for (int i = 0; i < 32; i++) {
      if ( (h & (1 << i)) != 0) {
        indices[index] = i;
        index++;
      }
    }
    return indices;
  }

  /** @return an array containing the indices of the 'on-bits' in the
   * hand h and its size
   */
  public static int getOnBitsFast(int h, int[] onBits)
  {
    int index = 0;
    for (int i = 0; i < onBits.length; i++) {
      onBits[i] = 0;
    }

    for (int i = 0; i < 32; i++) {
      if ((h & (1 << i)) != 0) {
        onBits[index++] = i;
      }
    }
    return index;
  }

  /** @return an array containing the indices of the k 'on-bits' in
   * the hand h, relative to the potential card set cards
   */
  public static int[] getValues(int h, int cards, int k) {

    int[] indices = new int[k];
    int index = 0;
    int value = numCards(cards);

    for (int i = 31; i >= 0; i--) {
      if ( (h & cards & (1 << i)) != 0) {
        indices[index] = value;
        index++;
      }
      if ( (cards & (1 << i)) != 0) {
        value--;
      }
    }
    return indices;
  }

  /** @return an array containing the indices of the k 'on-bits' in
   * the hand h, relative to the potential card set cards
   */
  public static int getValuesFast(int h, int cards, int[] values)
  {
    for (int i = 0; i < values.length; i++) {
      values[i] = 0;
    }

    int index = 0;
    int value = numCards(cards);
    int handAndCards = h & cards;
    for (int i = 31; i >= 0; i--) {
      int mask = 1 << i;
      if ((handAndCards & mask) != 0) {
        values[index++] = value;
      }
      if ((cards & mask) != 0) {
        value--;
      }
    }
    return index;
  }


  /** @return the hand corresponding to the rank r using a co-lex
   * ordering, given that cardsOut are the unavailable cards and we
   * want a k-card hand
   */
  static public int colexUnrank(int r, int k, int cardsOut) {
    int cards = -1;
    cards = cards ^ cardsOut;
    int n = numCards(cards);
    int[] onbits = getOnBits(cards, n);
    // int[] onbits = getOnBitsFast(cards, onBits);
    int answer = 0;

    int x = n;
    //Misc.msg("Unranking index: " + r);
    for (int i = 1; i <= k; i++) {     
      while (Misc.choose(x, k+1-i) > r) {
        x--;
      }
      answer = answer | (1 << (onbits[x]));
      r = r - Misc.choose(x, k+1-i);
    }

    return answer;
  }


  /** @return the co-lex rank of a hand h taken from the subset of
   * cards that exclude cardsOut
   */
  static public int colexRank(int h, int cardsOut)
  {
    int[] values = new int[32];
    return colexRankFast(h, cardsOut, values);
  }

  /** @return the co-lex rank of a hand h taken from the subset of
   * cards that exclude cardsOut
   */
  static public int colexRankFast(int h, int cardsOut, int[] values) {

    int k = numCards(h);
    int cards = -1 ^ cardsOut;
    getValuesFast(h, cards, values);
    
    int r = 0;
    for (int i = 1; i <= k; i++) {
      r += Misc.choose(values[i-1]-1, k+1-i);
    }

    return r;
  }

  /** @return The successor hand to this hand, given the cardsOut and
   * a co-lex ordering, returns -1 when end of ranking is reached
   */
  static public int colexSuccessor(int h, int cardsOut) {
    int h2 = h;
    int cards = -1;
    cards = cards ^ cardsOut;
    int n = numCards(cards);
    int k = numCards(h);
    int[] onbits = getOnBits(cards, n);
    int[] values = getValues(h, cards, k);

    if (k == 0) {
      return -1;
    }
    // First, check if this is the last element of the ranking
    if (values[k-1] == (n-k+1)) {
      return -1;
    }

    int j = k-1;
    // Until we find a 'jump' of more than 1, we just reset everything
    // to its lowest value, starting from the right
    if (j > 0) {
      while ( (1 == (values[j-1] - values[j]))  ) {
        
        h2 = h2 ^ (1 << onbits[values[j]-1]);
        h2 = h2 ^ (1 << onbits[k - 1 -j]);
        j--;
        if (j == 0) {
          break;
        }      
      }
    }
    // Then, we increment the element where a jump of 2 occurred (or
    // the very biggest element if none did)
    h2 = h2 ^ (1 << onbits[values[j]-1]);
    h2 = h2 ^ (1 << onbits[values[j]]);

    return h2;
  }

  /** same as colexSuccessor except that no memory is allocated */
  static public int colexSuccessorFast(int h, int cardsOut, int[] onBits, int[] values)
  {
    int h2 = h;
    int cards = -1 ^ cardsOut;
    int n  = numCards(cards);
    int k  = numCards(h);
    int n2 = getOnBitsFast(cards, onBits);
    int k2 = getValuesFast(h, cards, values);

    if (n != n2 || k != k2) Misc.err("uff");
    
    if (k == 0) {
      return -1;
    }
    // First, check if this is the last element of the ranking
    if (values[k-1] == (n-k+1)) {
      return -1;
    }

    int j = k-1;
    // Until we find a 'jump' of more than 1, we just reset everything
    // to its lowest value, starting from the right
    if (j > 0) {
      while ( (1 == (values[j-1] - values[j]))  ) {
        
        h2 = h2 ^ (1 << onBits[values[j]-1]);
        h2 = h2 ^ (1 << onBits[k - 1 -j]);
        j--;
        if (j == 0) {
          break;
        }      
      }
    }
    // Then, we increment the element where a jump of 2 occurred (or
    // the very biggest element if none did)
    h2 = h2 ^ (1 << onBits[values[j]-1]);
    h2 = h2 ^ (1 << onBits[values[j]]);

    return h2;
  }

//////////////////////////////////////////////////////////////////
//  End of ranking functionality
/////////////////////////////////////////////////////////////////


  /** @return the number of high cards (ace + 10) in a hand */
  static public int numHighCards(int hand) {

    int count = 0;
    for (int s = 0; s < 4; s++) {
      if (has(hand, s, Card.RANK_TEN)) {
        count++;
      }
      if (has(hand, s, Card.RANK_ACE)) {
        count++;
      }
    }
    return count;
  }

  /** @return the number of aces in a hand */
  static public int numAces(int hand) {
    return Misc.popCount(hand & Card.ACE_MASK);
  }

  /** @return the number of 10s in a hand */
  static public int num10s(int hand) {
    return Misc.popCount(hand & Card.TEN_MASK);
  }

  /** @return the number of Queens and Kings in a hand */
  static public int numQueenKing(int hand, int suit) {

    int count = 0;

    if (has(hand, suit, Card.RANK_QUEEN)) {
      count++;
    }
    if (has(hand, suit, Card.RANK_KING)) {
      count++;
    }
    return count;
  }

  /** @return the number of 10s in a hand */
  static public int num10sWithAce(int hand) {

    int count = 0;
    for (int s = 0; s < 4; s++) {
      if (has(hand, s, Card.RANK_TEN) && has(hand, s, Card.RANK_ACE)) {
        count++;
      }
    }
    return count;
  }

  /** @return the number of 10s in a hand */
  static public int num10sWithoutAce(int hand) {

    int count = 0;
    for (int s = 0; s < 4; s++) {
      if (has(hand, s, Card.RANK_TEN) && !has(hand, s, Card.RANK_ACE)) {
        count++;
      }
    }
    return count;
  }

  /** @return the number of cards in hand of the given suit */
  static public int numCards(int hand, int suit) {

    if (suit == Card.SUIT_NONE) return 0;

    int count = 0;
    
    for (int i = 0; i < 8; i++) {
      if ( (hand & (1 << (suit*8+i))) != 0)
      	count++;
    }

    return count;
  }

  /** @return the number of void suits in trump games */
  static public int numVoidsTrump(int hand, int type) {

    int count = 0;
    hand &= ~Card.JACK_MASK;
    for (int i = 0; i < 4; i++) {
      if (i == type) continue;
      if ( (hand & (0xff << (i<<3))) == 0)
      	count++;
    }

    return count;
  }

  /** @return the non-trump void suit mask */
  static public int voidSuitsTrump(int hand, int type) {
    int voidSuits = 0;
    hand &= ~Card.JACK_MASK;
    for (int i = 0; i < 4; i++) {
      if (i == type) continue;
      if ( (hand & (0xff << (i<<3))) == 0)
        voidSuits |= (0xff << (i<<3));
    }

    return voidSuits &= ~Card.JACK_MASK;
  }

  /** @return the number of singletons != ace in trump games */
  static public boolean noAceSingleton(int hand, int suit) {

    hand &= ~Card.JACK_MASK;
    hand >>>= (suit << 3);
    hand &= 0xff;

    if (Misc.popCount(hand) == 1)
      return (hand & (1 << Card.RANK_ACE)) == 0;

    return false;
  }

  /** @return the number of cards in hand of the given suit, which is
   * not trump (so don't include the jacks)
   */
  static public int numCardsNotTrump(int hand, int suit)  {

    if (suit == Card.SUIT_NONE) return 0;
    if (suit == GameDeclaration.GRAND_GAME) return 0;

    hand &= (0xff-(1 << Card.RANK_JACK)) << (suit << 3);
    return Misc.popCount(hand);
  }

  // also works for grand!
  static public int suitTrumpMask(int suit)
  {
    return Card.JACK_MASK | (0xff << (suit * 8));
  }
  
  /** @return the number of cards in hand of the given suit, which is
   * trump (so include the jacks)
   */
  static public int numCardsTrump(int hand, int suit)  {
    if (suit == Card.SUIT_NONE) return 0;

    // if (numCards(hand & suitTrumpMask(suit)) != numCardsTrump2(hand, suit))
    //  Misc.err("trumpmask");
    
    return numCards(hand & suitTrumpMask(suit));
  }

  /*
    old
    
    static public int numCardsTrump2(int hand, int suit)  {
    if (suit == Card.SUIT_NONE) return 0;

    return numCardsNotTrump(hand, suit) + numJacks(hand);
    }
  */
  
  /** @return The number of jacks in the player's hand (ie. the number
   * of trump in Grand games)
   */
  static public int numJacks(int hand) {
    hand &= Card.JACK_MASK;
    return Misc.popCount(hand);
  }

  /** @return the number of cards in hand */
  static public int numCards(int hand)  {
    return Misc.popCount(hand);
  }
  
  /** @return the number of cards in hand of the given suit, with a
   * boolean indicating if that suit is trump
   */
  static public int numCards(int hand, int suit, boolean trump)  {

    if (suit == Card.SUIT_NONE) return 0;

    if (trump) {
      return numCardsTrump(hand, suit);
    } else {
      return numCardsNotTrump(hand, suit);
    }
  }

  /** @return vector of cards */
  static public int toCardArray(int hand, Card[] cards)
  {
    int mn = 0;
    for (int s = 0; s < 4; s++) {
      for (int r = 0; r < 8; r++) {
        if ( (hand & (1 << (s*8+r))) != 0) {
          cards[mn++] = Card.newCard(s, r);
        }
      }
    }

    return mn;
  }

  /** @return vector of cards */
  static public ArrayList<Card> toCardList(int hand)
  {
    ArrayList<Card> cards = new ArrayList<Card>();
    for (int s = 0; s < 4; s++) {
      for (int r = 0; r < 8; r++) {
        if ( (hand & (1 << (s*8+r))) != 0) {
          cards.add(Card.newCard(s, r));
        }
      }
    }

    return cards;
  }

  /** @return hand from vector of cards */
  static public int fromCardList(ArrayList<Card> list, int begin, int end)
  {
    int hand = 0;
    for (int i=begin; i < end; i++) {
      Card c = list.get(i);
      hand |= c.toBit();
    }

    return hand;
  }

  static public int suitBits(int hand, int suit) {
    return (hand >> (8*suit)) & 255;
  }
  
  static public String toStringSuitColor(int hand, int suit) 
  {
    return toStringSuitColor(hand, suit, false);
  }
  
  static public String toStringSuitColor(int hand, int suit, boolean html) 
  {
    String value = "";
    for (int j=7; j >= 0; j--) {
      int r = Card.strengthOrder[j];
      if (r != Card.RANK_JACK) {
        if ((hand & (1<<(suit*8+r))) != 0) {
          Card c = Card.newCard(suit, r);
          value += c.toStringColor(html);
        }
      }
    }
    return value;
  }

  static public String toStringColor(int hand, int trumpSuit, boolean html) 
  {
    String value = "";

    if (trumpSuit == Card.SUIT_NONE) {
      
      // null ordering

      for (int i=3; i >= 0; i--) {
        for (int j = 0; j < 8; j++) {
          int r = Card.nullRanks[j];
          if ((hand & (1 << (i*8+r))) != 0) {
            Card c = new Card(i, r);
            value += c.toStringColor(html);
          }
        }
      }
      
      return value;
    }
    
    // trump game
    
    // jacks first
    
    for (int i=3; i >= 0; i--) {
      if ((hand & (1<<(i*8+Card.RANK_JACK))) != 0) {
        Card c = new Card(i, Card.RANK_JACK);
        value += c.toStringColor(html);
      }
    }
    
    // then trump suit
    value += toStringSuitColor(hand, trumpSuit, html); 
    
    // remaining suits
    
    for (int i=3; i >= 0; i--) { 
      if (i != trumpSuit) { value += toStringSuitColor(hand, i, html); }
    }
    return value;
  }

  static public String toStringColor(int hand, int trumpSuit)
  {
    return toStringColor(hand, trumpSuit, false);
  }

  static public String toStringSkatguiSuit(int hand, int suit) 
  {
    String value = "";
    for (int j=7; j >= 0; j--) {
      if (j != Card.RANK_JACK) {
        if ((hand & (1<<(suit*8+j))) != 0) {
          Card c = Card.newCard(suit, j);
          value += c.skatguiWrite();
        }
      }
    }
    return value;
  }


  static public String toStringSkatgui(int hand, int trumpSuit)
  {
    // jacks first

    String value = "";  

    for (int i=3; i >= 0; i--) {
      if ( (hand & (1<<(i*8+Card.RANK_JACK))) != 0) {
        Card c = new Card(i, Card.RANK_JACK);
        value += c.skatguiWrite();
      }
    }
  
    // then trump suit
    value += toStringSkatguiSuit(hand, trumpSuit);
  
    // remaining suits
    
    for (int i=3; i >= 0; i--) { 
      if (i != trumpSuit) { value += toStringSkatguiSuit(hand, i); }
    }

    return value;
  }


 
  static public String toString(int hand, boolean dots) 
  {
    // jacks first

    String value = "";
  
    value += "J";
  
    for (int i=3; i >= 0; i--) {
      if ( (hand & (1<<(i*8+Card.RANK_JACK))) != 0 )
        value += Card.suitNames[i];
      else if (dots)
        value += ".";
    }
  
    value += " ";
    
    // remaining suits
    
    for (int i=3; i >= 0; i--) { 
      value += writeSuit(hand, i, dots); 
      value += " ";
    }
  
    return value;

  }

  static public String toStringJacks(int hand, boolean dots) {

    String value = "";
    
    for (int i=3; i >= 0; i--) {
      if ( (hand & (1<<(i*8+Card.RANK_JACK))) != 0 )
        value += Card.suitNames[i];
      else if (dots)
        value += ".";
    }

    return value;

  }

  static public String writeSuit(int hand, int suit, boolean dots)
  {
    return Card.suitNames[suit] + writeSuitNoPrefix(hand, suit, dots);
  }

  static public String writeSuitNoPrefix(int hand, int suit, boolean dots)
  {
    String value = "";

    // cout << " dots = " << dots << endl;
    
    for (int j=7; j >= 0; j--) {
      int r = Card.strengthOrder[j];
      if (r != Card.RANK_JACK) {
        if ( (hand & (1<<(suit*8+r))) != 0)
          value += Card.rankNames[r];
        else if (dots)
          value += ".";
      }
    }
    return value;

  }

  static public String writeSuitNoPrefixNull(int hand, int suit, boolean dots)
  {
    String value = "";

    // cout << " dots = " << dots << endl;
    
    for (int j=7; j >= 0; j--) {
      int r = j; // null order
      if ( (hand & (1<<(suit*8+r))) != 0)
        value += Card.rankNames[r];
      else if (dots)
        value += ".";
    }
    return value;

  }

  /////////// simple card format : Suit-Rank

  static public String toStringSimpleSuit(int hand, int suit) 
  {
    String value = "";
    for (int j=7; j >= 0; j--) {
      int r = Card.strengthOrder[j];
      if (r != Card.RANK_JACK) {
        if ((hand & (1<<(suit*8+r))) != 0) {
          Card c = Card.newCard(suit, r);
          value += c.toString() + " ";
        }
      }
    }
    return value;
  }

  static public String toStringSimple(int hand, int trumpSuit)
  {
    String value = "";

    if (trumpSuit == Card.SUIT_NONE) {
      
      // null ordering

      for (int i=3; i >= 0; i--) {
        for (int j = 0; j < 8; j++) {
          int r = Card.nullRanks[j];
          if ((hand & (1 << (i*8+r))) != 0) {
            Card c = new Card(i, r);
            value += c.toString() + " ";
          }
        }
      }
      
      return value;
    }
    
    // trump game
    
    // jacks first
    
    for (int i=3; i >= 0; i--) {
      if ((hand & (1<<(i*8+Card.RANK_JACK))) != 0) {
        Card c = new Card(i, Card.RANK_JACK);
        value += c.toString() + " ";
      }
    }
    
    // then trump suit
    value += toStringSimpleSuit(hand, trumpSuit); 
    
    // remaining suits
    
    for (int i=3; i >= 0; i--) { 
      if (i != trumpSuit) { value += toStringSimpleSuit(hand, i); }
    }
    return value;
  }

  ///////////

  static private int fromStringSuit(int hand, int suit, String s)
  {
    char c;
  
    // cout << "fromStringSuit " << suit << endl;

    int mask = 255 & ~(1<<Card.RANK_JACK);
    hand &= ~(mask << suit*8); // clear out suit cards except jacks
    
    for (int j = 0; j < s.length(); j++) {
      c = s.charAt(j);
      if (c == '.') continue;
      
      //   cout << "READ hand::fromStringSuit: " << c << endl;
      
      int i=0;
      for (; i < 8; i++) {
        if (Card.rankNames[i] == c) break;
      }
  
      if (i >= 8) Misc.err("fromStringSuit error 2: " + s);
      
      hand |= 1 << (suit*8+i);
    }

    return hand;
  }

  public static int fromString(String s)
  {
    String[] parts = s.split("\\s+");

    // jacks first

    if (parts.length != 5)
      Misc.err("fromString error : not 5 parts : " + s);
    
    char c;
    int hand = 0;
    String t = parts[0]; // jacks
    
    // cout << "READ: " << c << endl;
    
    // read jacks
    
    if (t.length() < 1 || t.charAt(0) != 'J') Misc.err("fromString error 1: " + s);

    t = t.substring(1); // remove J

    for (int i=0; i < t.length(); i++) {
      c = t.charAt(i);
  
      // cout << " READ JACK: " << c << endl;
      
      if (c == '.') continue;
  
      int j;
      for (j=0; j < 4; j++) {
        if (c == Card.suitNames[j]) {
          hand |= 1<<(j*8+Card.RANK_JACK);
          break;
        }
      }
      if (j >= 4) Misc.err("fromString error 3: " + s);
    }
  
    // read suits (without jacks)
    
    for (int i=0; i < 4; i++) {

      t = parts[i+1];

      if (t.length() < 1 || t.charAt(0) != Card.suitNames[3-i]) Misc.err("fromString error: suit prefix " + s);

      //Misc.msg("suit = " + t.charAt(0));
      
      t = t.substring(1); // remove prefix

      //Misc.msg("rest = " + t);
      
      hand = fromStringSuit(hand, 3-i, t);
    }
  
    return hand;
  }

  /* Note: all null functions are currently untested in Java!  Remove
   * this once we are certain they work. -Jeff
   */

  /** @return true iff game is a safe depending on whether player is
   * in forehand
   */
  static public boolean safeNull(int hand, boolean forehand)
  {
    // all suits must be safe when not leading

    // cout << "---------" << endl;
    
    for (int s = 0; s < 4; s++) {
      boolean r = suitSafeNoLead(suitBits(hand, s));
      //  cout << "save: " << s << " " << r << endl;
      if (!r)
        return false;
    }
  
    if (forehand) {
  
      // there must be safe lead
  
      for (int s = 0; s < 4; s++) {
        boolean r = suitSafeLead(suitBits(hand, s));
        // cout << "SAVE: " << s << " " << r << endl;      
        if (r)
          return true;
      }
  
      // no safe lead
      return false;
    }
    
    return true;
  }

  
  

  /** @return number of suits that are safe when not leading */
  static public int safeNoLeadSuits(int hand)
  {
    int count = 0;
  
    for (int s = 0; s < 4; s++) {
      if (suitSafeNoLead(suitBits(hand, s))) {
        count++;
      }
    }
  
    return count;
  }
  
  /** @return number of suits that are safe when not leading and cards are gone */
  static public int safeNoLeadSuits(int hand, int gone)
  {
    int count = 0;
  
    for (int s = 0; s < 4; s++) {
      //      System.out.print("PAIRS: " + Hand.toStringColor(suitBits(hand, s), 0, false) + " - " + Hand.toStringColor(suitBits(gone, s), 0, false) + " : ");
      if (suitSafeNoLead(suitBits(hand, s), suitBits(gone, s))) {
        //Misc.msg(" safe!");
        count++;
      } else {
        //Misc.msg(" unsafe!");
      }
    }
  
    return count;
  }
  
  /** @return number of suits that are unsafe when not leading */
  static public int unsafeNoLeadSuits(int hand)
  {
    int count = 0;
  
    for (int s = 0; s < 4; s++) {
      if (!suitSafeNoLead(suitBits(hand, s))) {
        count ++;
      }
    }
  
    return count;
  }

  /** @return true iff configuration is safe when leading it */
  static public boolean suitSafeLead(int config)
  {
    return safeMatch(safeLead, config);
  }

  /** @return true iff configuration 0..255 is safe when not leading it */
  static public boolean suitSafeNoLead(int config)
  {
    return safeNoLeadBits[config];
  }


  /** @return true iff configuration 0..255 is safe when not leading and cards
      have been played already (gone 0..255)
  */
  static public boolean suitSafeNoLead(int config, int gone)
  {
    if ((gone & config) != 0) Misc.err("intersection");    
    return safeNoLeadGoneBits[config][gone];
  }

  /** @return true iff configuration 0..255 is safe when not leading and cards
      have been played already (gone 0..255)
  */
  static private boolean suitSafeNoLeadHelper(int config, int gone)
  {
    if ((gone & config) != 0) Misc.err("intersection");

    int[] bitsToLeft = new int[8];

    // each gone card moves all config cards to its right one position
    // to the left

    int bits = 0;
    for (int i=0; i < 8; i++) {
      if ((gone & (1<<i)) != 0) {
        bits++;
      }
      bitsToLeft[i] = bits;
    }

    int newConf = 0;
    for (int i=0; i < 8; i++) {
      if ((config & (1<<i)) != 0) {
        newConf |= (1<<(i-bitsToLeft[i]));
      }
    }

    // Misc.msg(String.format("%2x %2x -> %2x", config, gone, newConf));
    //Misc.msg(String.format("%d %d",
    //                      safeNoLeadBits[config] ? 1 : 0,
    //                       safeNoLeadBits[newConf] ? 1 : 0));

    if (safeNoLeadBits[config] && !safeNoLeadBits[newConf])
      Misc.err("corrupt");
    
    return safeNoLeadBits[newConf];
  }

  
  public static boolean safeMatch(String[] s, int config)
  {
    int bits = Misc.popCount(config);
    int N = s.length;
    
    for (int i = 0; i < N; i++) {
  
      if (s[i].length() == bits) {
  
        // lengths match
  
        boolean match = true;
        boolean done = false;
        int j;
  
        for (j=0; j < bits; j++) {
          switch (s[i].charAt(j)) {
          case '*': done = true; break;
          case '7': match &= ((config & (1 << Card.RANK_SEVEN)) != 0); break;
          case '8': match &= ((config & (1 << Card.RANK_EIGHT)) != 0); break;
          case '9': match &= ((config & (1 << Card.RANK_NINE))  != 0); break;
          case 'T': match &= ((config & (1 << Card.RANK_TEN))   != 0); break;
          case 'J': match &= ((config & (1 << Card.RANK_JACK))  != 0); break;
          case 'Q': match &= ((config & (1 << Card.RANK_QUEEN)) != 0); break;
          case 'K': match &= ((config & (1 << Card.RANK_KING))  != 0); break;
          case 'A': match &= ((config & (1 << Card.RANK_ACE))   != 0); break;
          default: Misc.err("illegal card");
          }
          if (!match || done) break;
        }
  
        if (match && (done || j >= bits)) return true;
      }
    }
  
    return false;
  }

  // convert spread out trump bits (lowest 8 bits + jacks)
  // into a compact 12 bit representation with the jacks leading
  // (11 bits is more work)
  public static int compactTrumpBits(int bits)
  {
    // this seems to be slow, can we do this faster?
    // 1. convert into trump order
    // 2. recode: jacks are spread, put them right in front of the ace
    int b = bits;
    bits &= ~Card.JACK_MASK; // clear old jack bits
    bits = nullToTrumpGameOrder[bits];
    
    if ((b & (1<<(Card.RANK_JACK+0 ))) != 0) bits |= 1 << (8+0);
    if ((b & (1<<(Card.RANK_JACK+8 ))) != 0) bits |= 1 << (8+1);
    if ((b & (1<<(Card.RANK_JACK+16))) != 0) bits |= 1 << (8+2);
    if ((b & (1<<(Card.RANK_JACK+24))) != 0) bits |= 1 << (8+3);    
    return bits;
  }

  // maps compact trump index to card index (diamonds + jacks)
  static public int[] compactTrumpToCardIndex;
  
  // null bits -> trump game bit (for comparison)
  static public int[] nullToTrumpGameOrder; 
  
  // table for patterns below
  static boolean[] safeNoLeadBits;

  // table for (config, gone) pairs
  static boolean[][] safeNoLeadGoneBits;
  
  // safe configurations when not leading
  static String[] safeNoLead = {
    "7",
  
    "78",
    "79",
  
    "789",
    "78T",
    "78J",    
    "79T",
    "79J",
  
    "789T",
    "789J",
    "789Q",
    "789K",    
    "78TJ",
    "78TQ",
    "78TK",
    "78JQ",
    "78JK",
    "79TJ",
    "79TQ",
    "79TK",    
    "79JQ",
    "79JK",
  
    "789**",
    "78T**",
    "78J**",
    "79T**",
    "79J**",
  
    "78****",
    "79****",
  
    "7******",
  
    "789TJQKA"
  };
  
  
  // safe configurations for leading
    
  static String[] safeLead = {
    "7",
    
    "78",
    
    "789",
    "78T",
    
    "789T",
    "789J",
    "789Q",    
    "78TJ", 
    "78TQ",
  
    "789T*",
  
    "789**",
    "78T**",
  
    "78****",
  
    "7******"
  };


  // create tables  

  static {

    safeNoLeadBits = new boolean[256];
    
    for (int i=0; i < 256; i++) {
      safeNoLeadBits[i] = safeMatch(safeNoLead, i);
    }

    safeNoLeadBits[0] = true; // empty config
    
    safeNoLeadGoneBits = new boolean[256][256];
    
    for (int i=0; i < 256; i++) {
      for (int j=0; j < 256; j++) {
        if ((i & j) != 0) continue;
        safeNoLeadGoneBits[i][j] = suitSafeNoLeadHelper(i, j);
      }
    }

    nullToTrumpGameOrder = new int[256];
    // 789T*QKA  -> 789*QKTA
    for (int i=0; i < 256; i++) {

      int j =
	(i & 0x87) | // 789 + A stay
	((i & 0x70) >> 1) | // *QK
	((i & 0x08) << 3);  // T

      nullToTrumpGameOrder[i] = j;
    }

    // maps compact trump index to card index (diamonds + jacks)
    compactTrumpToCardIndex = new int[] {
    //7 8 9 *  Q K T A J J J J
      0,1,2,-1,5,6,3,7,4,4+8,4+16,4+24
    };
  }

}
