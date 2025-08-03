// $Id: CardImages.java 8160 2009-03-13 04:57:04Z mburo $

/**
 * The SkatGraphicRepository that holds all images used in Skat games
 *
 * (c) Michael Buro, licensed under GPLv3
 */

package common;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Canvas;
import java.net.*;

public class CardImages
{
  public CardImages() 
  {
    tracker = new MediaTracker(new Canvas());
    cards = new Image[4][8];
  }

  /**
   * Load all card images
   * 
   * @param cardType
   *            The directory name for the card set to be loaded
   */
  public void loadCards(String cardDeckDir, int w) 
  {
    width = w;
    String[] suitStrings = { "1","2","3","0" };
    String[] rankStrings = { "6","7","8","9","10","11","12","0" };    
    
    for (int suit = 0; suit < 4; suit++) {
      for (int rank = 0; rank < 8; rank++) {

        String file = cardDeckDir + "/" + suitStrings[suit] + "_" + rankStrings[rank] + ".gif";
        // Misc.msg("file = " + file);
	URL url = Thread.class.getResource(file);
	if (url == null) {
	  System.out.println("can't access card image");
	}
	
	cards[suit][rank] = Toolkit.getDefaultToolkit().getImage(url).
	  getScaledInstance(width, -1, Image.SCALE_SMOOTH);
	tracker.addImage(cards[suit][rank], 2);
      }
    }

    URL url = Thread.class.getResource(cardDeckDir + "/back.gif"); 
    if (url == null) {
      System.out.println("can't access card back image");
    }

    cardBack = Toolkit.getDefaultToolkit().getImage(url).
      getScaledInstance(width, -1, Image.SCALE_SMOOTH);
    
    tracker.addImage(cardBack, 2);

    try {
      tracker.waitForID(2);
    }
    catch (InterruptedException e) { }
  }

  /**
   * Gets the card image
   * 
   * @param suit
   *            The suit of the card
   * @param value
   *            The value of the card
   * @return The card image
   */
  public Image getCardImage(int suit, int value)
  {
    if (suit >= 0 && value >= 0) {

      try {
	return cards[suit][value];
      }

      catch (IndexOutOfBoundsException exc) {
	return null;
      }

    } else {

      return cardBack;
    }
  }

  /**
   * Gets the image for the skat table
   * 
   * @return The image for the skat table
   */
  public Image getSkatTableImage()
  {
    return skatTable;
  }

  /**
   * return card width in pixels
   *
   */
  public int getWidth()  { return width; }
  public int getHeight() { return (int)(1.5806*width); } // hack
  
  private MediaTracker tracker;
  private Image skatTable;
  private Image cards[][];
  private Image cardBack;
  private int width; // pixels
}
