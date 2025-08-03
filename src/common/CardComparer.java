// $Id$

// compares two cards depending on game type (for gfx hand sorting)

// (c) Michael Buro, licensed under GPLv3

package common;

import java.util.*;

public class CardComparer implements Comparator<Card> {

  private int gameType;
  private boolean redBlack;
  final int[] suitVals1 = { 0, 1, 2, 3 }; // regular order   (E4,G4,GG)
  final int[] suitVals2 = { 0, 2, 1, 3 }; // red/black order (E2,G2)
  
  public CardComparer(int gameType_, boolean redBlack_) {
    redBlack = redBlack_;
    gameType = gameType_;
  }
    
  public int compare(Card i1, Card i2)
  {
    if (i1.getSuit() < 0 && i2.getSuit() < 0) return 0;
    if (i1.getSuit() < 0) return +1;
    if (i2.getSuit() < 0) return -1;

    int[] suitVals = redBlack ? suitVals2 : suitVals1;
    
    if (gameType == GameDeclaration.NULL_GAME) {

      int v1 = suitVals[i1.getSuit()] * 10 + Card.nullRanks[i1.getRank()];
      int v2 = suitVals[i2.getSuit()] * 10 + Card.nullRanks[i2.getRank()];

      return v2-v1;

    } else {

      int d = 0; // suit delta (starts with trump suit and then alternates)

      if (gameType != GameDeclaration.NO_GAME   &&
          gameType != GameDeclaration.NULL_GAME &&
          gameType != GameDeclaration.GRAND_GAME)
        d = gameType;

      // jacks: orig. suit (otherwise: starting with trump suit C S H D or C H S D if redBlack is set)
      int v1 =
        ((i1.getRank() == Card.RANK_JACK) ? i1.getSuit() : ((suitVals[i1.getSuit()] + 4 - suitVals[d]) % 4)) * 10 +
        Card.suitRanks[i1.getRank()];
      int v2 =
        ((i2.getRank() == Card.RANK_JACK) ? i2.getSuit() : ((suitVals[i2.getSuit()] + 4 - suitVals[d]) % 4)) * 10 +
        Card.suitRanks[i2.getRank()];

      // jacks to front

      if (i1.getRank() == Card.RANK_JACK)
        v1 += 100;
      if (i2.getRank() == Card.RANK_JACK)
        v2 += 100;

      if (gameType != GameDeclaration.GRAND_GAME) {

        // trump after jacks
        if (i1.getRank() != Card.RANK_JACK && i1.getSuit() == gameType)
          v1 += 50;
        if (i2.getRank() != Card.RANK_JACK && i2.getSuit() == gameType)
          v2 += 50;
      }
	  
      return v2-v1;
    }
  }
}

