/*
 * ImagePos.java
 *
 * Created on September 14, 2006, 11:35 PM
 *
 * a class containing a card image, position, and card type
 */

package common;

import java.awt.*;

/**
 *
 * @author mburo
 */
public class ImagePos
{
  public Image image;
  public int x, y;
  public int suit, rank;
  
  public ImagePos(Image image_, int suit_, int rank_, int x_, int y_) {
    image = image_;
    x = x_;
    y = y_;
    suit = suit_;
    rank = rank_;
  }
  
  //public boolean equals(Object o)
  //{
  // if (o == null || !(o instanceof ImagePos))
  //   return false;

  //    ImagePos io = (ImagePos)o;
    
  //  return suit == io.suit && rank == io.rank;
  //}
  
}
