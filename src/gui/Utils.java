// GUI utility functions

// (c) Michael Buro, licensed under GPLv3

package gui;

import common.*;
import java.util.*;
import java.awt.*;

public class Utils
{
  /* From a hand, generate a Vector of ImagePos to display in the GUI
   */
  public static ArrayList<ImagePos> cardsToImages(ArrayList<Card> cards, CardImages cardImages)
  {
    ArrayList<ImagePos> imagepos = new ArrayList<ImagePos>();

    for (Card c : cards) {

      if (c == null)
	Misc.err("CARD NULL");
      
      Image im = cardImages.getCardImage(c.getSuit(), c.getRank());
      imagepos.add(new ImagePos(im, c.getSuit(), c.getRank(), 0, 0));
    }

    return imagepos;
  }


  public static void sortCards(ArrayList<Card> cards, int gameType, boolean redBlack)
  {
    Collections.sort(cards, new CardComparer(gameType, redBlack));
  }
  
  public static void arrangeImagesStraight(ArrayList<ImagePos> deck, int w, int ydisp, int maxNum, boolean center)
  {
    int dx = 0;

    if (center) {
      int e = maxNum - deck.size();
      if (e < 0) e = 0;
      dx = ((e * w)/maxNum)/2;
    }
    
    for (int i=0; i < deck.size(); i++) {
      ImagePos imp = deck.get(i);

      imp.x = dx + i * w / maxNum;
      imp.y = ydisp;
    }
  }
}
