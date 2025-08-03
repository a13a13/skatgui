/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ChooseCardPanel.java
 *
 * Created on 6-Jun-2009, 10:50:48 PM
 */

package gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.*;
import common.*;

/**
 *
 * @author mburo
 */
public class ChooseCardPanel extends javax.swing.JPanel {

  private final String startHTMLDiamonds = "<html><body><b><font color=\"#FFCC00\">";
  private final String startHTMLHearts = "<html><body><b><font color=\"#FF0000\">";
  private final String startHTMLSpades = "<html><body><b><font color=\"#006600\">";
  private final String startHTMLClubs = "<html><body><b><font color=\"#000000\">";
  private final String endHTML = "</font></b></body></html>";
  
  /** Creates new form ChooseCardPanel */
  public ChooseCardPanel() {
    initComponents();
    cardTable.setFont(new java.awt.Font("Liberation Sans", 0, 16));

    int rowNum = 8;
    int colNum = 4;

    Object[][] entries = new Object[rowNum][];
    for (int index = 0; index < rowNum; index++)
      entries[index] = new Object[colNum];

    entries[0][0] = startHTMLClubs + "J" + endHTML;
    entries[0][1] = startHTMLSpades + "J" + endHTML;
    entries[0][2] = startHTMLHearts + "J" + endHTML;
    entries[0][3] = startHTMLDiamonds + "J" + endHTML;
    entries[1][0] = startHTMLClubs + "A" + endHTML;
    entries[1][1] = startHTMLSpades + "A" + endHTML;
    entries[1][2] = startHTMLHearts + "A" + endHTML;
    entries[1][3] = startHTMLDiamonds + "A" + endHTML;
    entries[2][0] = startHTMLClubs + "10" + endHTML;
    entries[2][1] = startHTMLSpades + "10" + endHTML;
    entries[2][2] = startHTMLHearts + "10" + endHTML;
    entries[2][3] = startHTMLDiamonds + "10" + endHTML;
    entries[3][0] = startHTMLClubs + "K" + endHTML;
    entries[3][1] = startHTMLSpades + "K" + endHTML;
    entries[3][2] = startHTMLHearts + "K" + endHTML;
    entries[3][3] = startHTMLDiamonds + "K" + endHTML;
    entries[4][0] = startHTMLClubs + "Q" + endHTML;
    entries[4][1] = startHTMLSpades + "Q" + endHTML;
    entries[4][2] = startHTMLHearts + "Q" + endHTML;
    entries[4][3] = startHTMLDiamonds + "Q" + endHTML;
    entries[5][0] = startHTMLClubs + "9" + endHTML;
    entries[5][1] = startHTMLSpades + "9" + endHTML;
    entries[5][2] = startHTMLHearts + "9" + endHTML;
    entries[5][3] = startHTMLDiamonds + "9" + endHTML;
    entries[6][0] = startHTMLClubs + "8" + endHTML;
    entries[6][1] = startHTMLSpades + "8" + endHTML;
    entries[6][2] = startHTMLHearts + "8" + endHTML;
    entries[6][3] = startHTMLDiamonds + "8" + endHTML;
    entries[7][0] = startHTMLClubs + "7" + endHTML;
    entries[7][1] = startHTMLSpades + "7" + endHTML;
    entries[7][2] = startHTMLHearts + "7" + endHTML;
    entries[7][3] = startHTMLDiamonds + "7" + endHTML;

    String[] tableHeaders = new String[] { startHTMLClubs + "\u2663" + endHTML,
                                           startHTMLSpades + "\u2660" + endHTML,
                                           startHTMLHearts + "\u2665" + endHTML,
                                           startHTMLDiamonds + "\u2666" + endHTML };
    
    cardTable.setModel(new javax.swing.table.DefaultTableModel(entries, tableHeaders) {
        @Override
          public Class getColumnClass(int columnIndex) {
          return String.class;
        }

        @Override
          public boolean isCellEditable(int rowIndex, int columnIndex) {
          return false;
        }
      });
    
    DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
        @Override
          public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
	  Component renderer =
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

          int bit = bitFromRowColumn(row, column);
          setBackground(Color.WHITE);            
          
	  if (value != null && (availableCards & bit) != 0) {
            if ((selectedCards & availableCards & bit) != 0) {
              setBackground(selectColor);
            } 
	    setText(value.toString());
	  } else {
	    setText("");
          }
	  setHorizontalAlignment(JLabel.CENTER);

	  return renderer;
	}
      };

    for (int i=0; i < 4; i++) {
      cardTable.getColumnModel().getColumn(i).setCellRenderer(center);
    }

    // replace existing mouse listeners by customized one
    
    MouseListener[] mls = (cardTable.getListeners(MouseListener.class));

    for (MouseListener ml : mls) {
      cardTable.removeMouseListener(ml);
    }

    cardTable.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e)
        {
          if (!isEnabled()) return;
          
          Point p = e.getPoint();
          int row = cardTable.rowAtPoint(p);
          int column = cardTable.columnAtPoint(p); // This is the view column!
          
          // Misc.msg("clicked " + row + " " + column);

          int bit = bitFromRowColumn(row, column);

          if ((availableCards & bit) == 0)
            return; // not available

          if (cardNum == 1) {

            selectedCards = bit; // pick new card
            
          } else {
            
            if ((selectedCards & bit) == 0 && Hand.numCards(selectedCards) == cardNum)
              return; // cardNum reached
            
            selectedCards ^= bit;
          }
          
          invalidate();
          repaint();

          if (handler != null)
            handler.handleEvent(new EventMsg(sender, ""+bit)); // bit changed
        }
      });
  }

  public void setHandler(String name, EventHandler h)
  {
    sender = name;
    handler = h;
  }

  public void setCardNum(int n)
  {
    // n=1: always select new card
    cardNum = n;
  }
  
  public void setSelected(int bits)
  {
    selectedCards = availableCards & bits;
    invalidate();
    repaint();
  }

  public int getSelected()
  {
    return selectedCards;
  }

  public void setAvailable(int bits)
  {
    availableCards = bits;
    selectedCards &= availableCards;
    invalidate();
    repaint();
  }

  public int getAvailable() {
    return availableCards;
  }

  int bitFromRowColumn(int row, int column)
  {
    return 1 << 8*(3-column) + rankMap[row];
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        cardTable = new javax.swing.JTable();

        setMinimumSize(new java.awt.Dimension(126, 149));
        setPreferredSize(new java.awt.Dimension(300, 149));
        setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        cardTable.setAutoCreateRowSorter(true);
        cardTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "", "", "", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        cardTable.setDoubleBuffered(true);
        cardTable.setMinimumSize(new java.awt.Dimension(300, 144));
        cardTable.setRowHeight(18);
        cardTable.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(cardTable);

        add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 120, 150));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable cardTable;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

  // map table row to rank; 0..7 -> J A T K Q 9 8 7
  final private int[] rankMap = new int[] { 4, 7, 3, 6, 5, 2, 1, 0 };
  final private Color selectColor = new Color(210,210,210);
  private int availableCards = 0xffffffff, selectedCards = 0;
  private int cardNum = 10;

  private EventHandler handler;
  private String sender;
}