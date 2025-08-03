/*
 * CardPanel.java
 *
 * Created on September 14, 2006, 10:54 PM
 */

package common;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  mburo  
 */
public class CardPanel extends javax.swing.JPanel implements MouseListener {

  public String name;
  
  /** Creates new form CardPanel */
  public CardPanel() {
    initComponents();
    setDoubleBuffered(true);
    setBackground(Color.LIGHT_GRAY);
    addMouseListener(this);
  }

  public void setHandler(String name, EventHandler h)
  {
    sender = name;
    handler = h;
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   */
  private void initComponents() {
    setBorder(javax.swing.BorderFactory.createTitledBorder(""));
    setBorder(javax.swing.BorderFactory.createTitledBorder("Card Panel"));

    /*
      org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
                                layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                .add(0, 390, Short.MAX_VALUE)
                                );
      layout.setVerticalGroup(
                              layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                              .add(0, 273, Short.MAX_VALUE)
                              );
    */
  }
  
  // Variables declaration - do not modify//GEN-BEGIN:variables
  // End of variables declaration//GEN-END:variables
  
  public ArrayList<ImagePos> images;
  public EventHandler handler;
  public String sender;
  public Color cardBackground = super.getBackground();

  // Marker centers in (x, y) coordinates, where x < 0 means no marker:
  public int m1x = -1;
  public int m2x = -1;
  public int m1y;
  public int m2y;
  // Marker width and height:
  public int mw;
  public int mh;
  // Matches each raised card to its index in the player's hand:
  private int raisedCard1 = -1;
  private int raisedCard2 = -1;

  public Color markerColor = new Color(0, 0, 0); 
  
  public ArrayList<ImagePos> getImages() { return this.images; }

  public void setImages(ArrayList<ImagePos> images) { this.images = images; }

  // Needed to implement MouseListener interface:
  public void mousePressed(MouseEvent e) { }
  public void mouseClicked(MouseEvent e) { } 

  public void mouseReleased(MouseEvent e) {
    int x = e.getX();
    int y = e.getY();
    
    System.out.println(e.getX() + " " + e.getY());
    
    if (images != null) {
  
      Insets insets = getInsets();
      for (int i=images.size()-1; i >= 0; i--) {
        ImagePos ip = images.get(i);

        if (ip.image == null)
          continue; // skip, nothing displayed
      
        int w = ip.image.getWidth(null);
        int h = ip.image.getHeight(null);
        if (w < 0 || h < 0) continue;
  
        if (x > ip.x + insets.left + dx &&
            x < ip.x + insets.left + dx + w &&
            y > ip.y + insets.top + dy &&
            y < ip.y + insets.top + dy + h) {
          System.out.println("clicked card " + i + " " + ip.suit + " " + ip.rank);
          if (handler != null && (!(ip.suit == -1 && ip.rank == -1) || sender.startsWith("historyPanel")))
            handler.handleEvent(new EventMsg(sender, ""+i, ""+ip.suit, ""+ip.rank));
          return;
        } 
      }
    }
    
    if (handler != null)
      handler.handleEvent(new EventMsg(sender, "")); // clicked
  } 

  // new:
  //
  //	if (Card.getCardImage(c.suit, c.rank) == null) continue; // nothing displayed
  //        
  //	int w = c.getThisCardImage().getWidth(null);
  //	int h = c.getThisCardImage().getHeight(null);
  //	if (w < 0 || h < 0) continue;
  //
  //	if (x > c.x && x < c.x + w && y > c.y && y < c.y + h) {
  //	  if (handler != null) 
  //	    handler.handle_event(new EventMsg(sender, ""+i, ""+c.suit, ""+c.rank));
  //	  return;
  //	}
  //      }
  //    }
  //    
  //    if (handler != null) {
  //      handler.handle_event(new EventMsg(sender, "MOUSE_NOT_OVER_CARD"));
  //    }
  //  } 

  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  static final int dx = 4;
  static final int dy = 4;

  public int getUsableWidth() {
    return getWidth() - getInsets().left - getInsets().right - 2*dx;
  }

  public int getUsableHeight() {
    return getHeight() - getInsets().top - getInsets().bottom - 2*dy;
  }

  public int getX0() {
    return getInsets().left + dx;
  }
  
  public int getY0() {
    return getInsets().top + dy;
  }
  
  public void setCardBackground(Color color) {
    cardBackground = color;
  }

  // The following 17 methods were added by Ryan for use in SkatTool.
  public int getFirstX() {
    return m1x;
  }

  public int getSecondX() {
    return m2x;
  }

  public int getFirstY() {
    return m1y;
  }

  public int getSecondY() {
    return m2y;
  }

  public int getMarkerWidth() {
    return mw;
  }

  public int getMarkerHeight() {
    return mh;
  }

  public void setFirstX(int num) {
    m1x = num;
  }

  public void setSecondX(int num) {
    m2x = num;
  }

  public void setFirstY(int num) {
    m1y = num;
  }

  public void setSecondY(int num) {
    m2y = num;
  }

  public void setMarkerWidth(int num) {
    mw = num;
  }

  public void setMarkerHeight(int num) {
    mh = num;
  }

  public void removeMarkers() {
    m1x = -1;
    m2x = -1;
  }

  public void setRaisedCard1(int num) {
    raisedCard1 = num;
  }

  public void setRaisedCard2(int num) {
    raisedCard2 = num;
  }

  public int getRaisedCard1() {
    return raisedCard1;
  }

  public int getRaisedCard2() {
    return raisedCard2;
  }

  @Override
  public void paint(Graphics g) //Graphics actualG)
  {
    // Image image = createImage(getWidth() + 1, getHeight() + 1);
    // Graphics g = image.getGraphics();

    super.paint(g);

    Insets insets = getInsets();

    // Misc.msg("XXXX " + name + " w=" + getWidth() + " h=" + getHeight());
    
    g.setColor(cardBackground);
    g.fillRect(insets.left, insets.top, getUsableWidth()+2*dx, getUsableHeight()+2*dy);
    
//     java.awt.Component[] children = getComponents();
//     for (int i = 0; i < children.length; i++) {
//       if (children[i].isVisible())
//         children[i].paint(g);
//     }

    int left = insets.left + dx;
    int top  = insets.top  + dy;
    
    if (images != null) {

      g.setClip(left, top, getUsableWidth(), getUsableHeight());

      for (ImagePos ip : images) {
	g.drawImage(ip.image, ip.x+left, ip.y+top, this);
        // Misc.msg("image: " + ip.x + " " + ip.y + " " + getWidth());
      }
       
    }

    g.setClip(0, 0, getWidth(), getHeight());

    g.setColor(markerColor);

    if (m1x >= 0) {
      g.fillRect(left+m1x-mw/2, top+m1y-mh/2, mw, mh);
    }

    if (m2x >= 0) {
      g.fillRect(left+m2x-mw/2, top+m2y-mh/2, mw, mh);
    }

    paintChildren(g);

    //Misc.msg(getWidth() + " " + getHeight());
    //actualG.drawImage(image, 0, 0, this);
  }
}