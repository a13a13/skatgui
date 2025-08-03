package common;

import java.util.*;

public class ImagePosComparer implements Comparator<ImagePos>
{
  private int gameType;
    
  public ImagePosComparer(int gameType_) { gameType = gameType_; }
    
  public int compare(ImagePos i1, ImagePos i2)
  {
    if (i1.suit < 0 && i2.suit < 0) return 0;
    if (i1.suit < 0) return +1;
    if (i2.suit < 0) return -1;

    if (gameType == GameDeclaration.NULL_GAME) {

      int v1 = (i1.suit) * 10 + Card.nullRanks[i1.rank];
      int v2 = (i2.suit) * 10 + Card.nullRanks[i2.rank];

      return v2-v1;

    } else {

      int v1 = (i1.suit) * 10 + Card.suitRanks[i1.rank];
      int v2 = (i2.suit) * 10 + Card.suitRanks[i2.rank];

      // jacks to front

      if (i1.rank == Card.RANK_JACK)
        v1 += 100;
      if (i2.rank == Card.RANK_JACK)
        v2 += 100;

      if (gameType != GameDeclaration.GRAND_GAME) {

        // trump after jacks
        if (i1.rank != Card.RANK_JACK && i1.suit == gameType)
          v1 += 50;
        if (i2.rank != Card.RANK_JACK && i2.suit == gameType)
          v2 += 50;
      }
	  
      return v2-v1;
    }
  }
}
