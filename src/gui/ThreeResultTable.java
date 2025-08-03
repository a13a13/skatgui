/*
 * Creates a tournament-style result table in the "Results" tab of the Skat GUI.
 * This class is for 3-player tables only.  Both this and the class for the 4-player
 * tables extend TournamentResultTable.
 *
 * (c) Ryan Lagerquist
 */

package gui;

import common.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import net.miginfocom.swing.MigLayout;

public class ThreeResultTable extends TournamentResultTable {

    Table table;
    String[] resultTableHeaders;
    FontMetrics fontMetrics; // for resize() method

    java.util.ResourceBundle bundle;
    final static String resourceFile = "data/i18n/gui/TableWindow";

    /* These arrays should not be needed to print data to the result table.  Instead, I should
       be able to use methods in the Table class, such as playerInGame(int playerIndex),
       getWins(int playerIndex), and getTotalPts(int playerIndex).  But for some reason, these
       methods are acting as they're supposed to.  The integers that I was using as parameters
       in these methods were based on gameResult.declarer, which itself is an integer.  When trying
       to update the declarer's totals, I would pass gameResult.declarer as the parameter;
       when updating the totals of defenders (which happened only when defenders won the game),
       I would use all player indexes but gameResult.declarer.  However, for some reason, these
       indexes were pointing to the wrong players. */
    String[] players = { "", "", "" };
    int[] playerPts = { 0, 0, 0 };
    int[] playerWins = { 0, 0, 0 };
    int[] playerLosses = { 0, 0, 0 };

  public ThreeResultTable(Table table) {
    this.table = table;
    bundle = java.util.ResourceBundle.getBundle(resourceFile);
    
    initComponents();
  }

    String rbs0(String s) { return s; } // for development
                                        // strings passed as parameters to rbs0 need to be internationalized

  public void addGameData(GameResult gameResult_, GameDeclaration gameDeclaration_) {
    GameResult gameResult;
    GameDeclaration gameDeclaration;
    gameResult = gameResult_;
    gameDeclaration = gameDeclaration_;

    /* Adds player names to the first row of the table.  Each player name appears under column
       heading "Seat 1..3".  This is done after the first game.  It cannot be done in the constructor,
       because the table will not be full yet, so we won't know who the players are. */
    if (table.getGameNum() == 1) {
	this.getModel().setValueAt(table.playerInGame(0), 0, 6);
	players[0] = table.playerInGame(0);
	this.getModel().setValueAt(table.playerInGame(1), 0, 9);
	players[1] = table.playerInGame(1);
	this.getModel().setValueAt(table.playerInGame(2), 0, 12);
	players[2] = table.playerInGame(2);
    }

    int row = table.getGameNum();
    
    if (gameResult.passed) {
	// If all players passed, nothing to do but mark the last column with an "X".
	this.getModel().setValueAt("X", row, this.getColumnModel().getColumnCount() - 1);
    } else {
	if (gameDeclaration.type == GameDeclaration.DIAMONDS_GAME) 
	    this.getModel().setValueAt(9, row, 1);
	else if (gameDeclaration.type == GameDeclaration.HEARTS_GAME)
	    this.getModel().setValueAt(10, row, 1);
	else if (gameDeclaration.type == GameDeclaration.SPADES_GAME)
	    this.getModel().setValueAt(11, row, 1);
	else if (gameDeclaration.type == GameDeclaration.CLUBS_GAME)
	    this.getModel().setValueAt(12, row, 1);
	else if (gameDeclaration.type == GameDeclaration.NULL_GAME)
	    this.getModel().setValueAt(23, row, 1);
	else if (gameDeclaration.type == GameDeclaration.GRAND_GAME)
	    this.getModel().setValueAt(24, row, 1);

	// If the game type was null, we don't have to worry about jacks with or without.
	if (gameDeclaration.type != GameDeclaration.NULL_GAME) {
	    if (gameResult.matadors > 0)
		this.getModel().setValueAt(""+gameResult.matadors, row, 2);
	    else
		this.getModel().setValueAt(""+(-gameResult.matadors), row, 3);
	}

	/* "Modifiers" include: hand, schneider, schneider announced, schwarz, schwarz announced,
	   and ouvert.  Abbreviations are explained in a label beneath the result table, called
	   infoLabel, in TournamentTablePanel2. */
	String modifiers = "";

	if (gameDeclaration.hand)
	    modifiers += "H";
 
	if (gameResult.declCardPoints >= 90) {
	    // declarer earned schneider
	    modifiers += "S";
	    if (gameDeclaration.schneiderAnnounced)
		modifiers += "+A";
	} else {
	    // declarer did not earn schneider
	    if (gameDeclaration.schneiderAnnounced)
		modifiers += "SA";
	}

	if (gameResult.declCardPoints == 120) {
	    // declarer earned schwarz
	    modifiers += "Z";
	    if (gameDeclaration.schwarzAnnounced)
		modifiers += "+A";
	} else {
	    // declarer did not earn schwarz
	    if (gameDeclaration.schwarzAnnounced)
		modifiers += "ZA";
	}
	
	if (gameDeclaration.ouvert)
	    modifiers += "O";

	this.getModel().setValueAt(modifiers, row, 4);

	/* The "game value" right now appears as the total number of points earned or lost by the 
	   declarer. */
	this.getModel().setValueAt(""+(gameResult.declValue + (gameResult.declValue > 0  ?  50 : -50)), row, 5);

	/* As explained above, I tried to use functions in the Table class to fill this part of the result
	   table, but had problems. */

	String declarer = table.playerInGame(gameResult.declarer);

	for (int index = 0; index < players.length; index++) {
	    if (declarer.equals(players[index])) {
		int columnIncrementer = 3*index;

		playerPts[index] += gameResult.declValue + (gameResult.declValue > 0  ?  50 : -50);
		playerWins[index] += (gameResult.declValue > 0  ?  1 : 0);
		playerLosses[index] += (gameResult.declValue < 0  ?  1 : 0);
		
		// Updates wins, losses, and total points of declarer.
		this.getModel().setValueAt(""+playerPts[index], row, 6+columnIncrementer);
		if (gameResult.declValue > 0)
		    this.getModel().setValueAt(""+playerWins[index], row, 7+columnIncrementer);
		else {
		    this.getModel().setValueAt(""+playerLosses[index], row, 8+columnIncrementer);

		    int columnIncrementer2;

		    // If defenders won, updates their total points.
		    for (int index2 = 0; index2 < players.length - 1; index2++) {
			if (!declarer.equals(players[index2])) {
			    columnIncrementer2 = 3*index2;
			    playerPts[index2] += 40;
			    this.getModel().setValueAt(""+playerPts[index2], row, 6+columnIncrementer2);
			}
		    }
		}
		break;
	    }
	}

	// ***Why doesn't this code work?  It doesn't print out the right number of wins or points.

	// this.getModel().setValueAt(""+table.getTotalPts(table.playerInGameIndex(declarer)), row, 6+columnIncrementer);
	// if (gameResult.declValue > 0)
	//    this.getModel().setValueAt(""+table.getWins(table.playerInGameIndex(declarer)), row, 7+columnIncrementer);
	// else
	//    this.getModel().setValueAt(""+(table.getPlayed(table.playerInGameIndex(declarer)) - table.getWins(table.playerInGameIndex(declarer)), 
	//    row, 8+columnIncrementer);
    }

    // sets focus to the last row of the table, in case it's past the end of the scrollbar
    this.changeSelection(row, 0, false, false);
    this.scrollRectToVisible(this.getCellRect(row, 0, true));
  }

  String rbs(String s) { return bundle.getString(s); }  

  public void initComponents() {

      resultTableHeaders = new String[] {
	  "#",
	  rbs0("Value"),
	  rbs0("With"),
	  rbs0("W/out"),
	  rbs0("Modifiers"),
	  rbs0("Game Pts"),
	  rbs0("Seat") + " 1",
	  rbs0("W"),
	  rbs0("L"),
	  rbs0("Seat") + " 2",
	  rbs0("W"),
	  rbs0("L"),
	  rbs0("Seat") + " 3",
	  rbs0("W"), // for "wins"
	  rbs0("L"), // for "losses"
	  rbs0("GP") // for "game passed"
      };

    /* Could probably use the addRow method in DefaultTableModel, but as deleteRow
       has not worked for me in the past, I am trying to keep things simple right
       now by giving the table as many rows as it will need based on the number
       of players. */
      this.setModel(new javax.swing.table.DefaultTableModel(new Object [][] {
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},            
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},            
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null},
		  {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null}            
	      },
	      resultTableHeaders
	      ) {
	      Class [] types = new Class [] {
		  Integer.class,
		  Integer.class,
		  Integer.class,
		  Integer.class,
		  String.class,
		  Integer.class,
		  String.class,
		  Integer.class,
		  Integer.class,
		  String.class,
		  Integer.class,
		  Integer.class,
		  String.class,
		  Integer.class,
		  Integer.class,
		  String.class
	      };

	      @Override
		  public Class getColumnClass(int columnIndex) {
		  return types[columnIndex];
	      }
	      
	      @Override
		  public boolean isCellEditable(int rowIndex, int columnIndex) {
		  return false;
	      }
	  });

      DefaultTableCellRenderer left = new DefaultTableCellRenderer() {
	      @Override
		  public Component getTableCellRendererComponent(JTable table, Object value,
								 boolean isSelected, boolean hasFocus,
								 int row, int column) {
		  Component renderer =
		      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		  
		  if (value != null)
		      setText(value.toString());
		  else
		      setText("");
		  setHorizontalAlignment(JLabel.LEFT);
		  return renderer;
	      }
	  };
      
      DefaultTableCellRenderer right = new DefaultTableCellRenderer() {
	      @Override
		  public Component getTableCellRendererComponent(JTable table, Object value,
								 boolean isSelected, boolean hasFocus,
								 int row, int column) {
		  Component renderer =
		      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		  
		  if (value == null)
		      setText("");
		  else
		      setText(value.toString());
		  setHorizontalAlignment(JLabel.RIGHT);
		  return renderer;
	      }
	  };

      DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
	      @Override
		  public Component getTableCellRendererComponent(JTable table, Object value,
								 boolean isSelected, boolean hasFocus,
								 int row, int column) {
		  Component renderer =
		      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		  
		  if (value != null)
		      setText(value.toString());
		  else
		      setText("");
		  setHorizontalAlignment(JLabel.CENTER);
		  return renderer;
	      }
	  };

      this.getColumnModel().getColumn(0).setCellRenderer(center);
      this.getColumnModel().getColumn(1).setCellRenderer(right);    
      this.getColumnModel().getColumn(2).setCellRenderer(right);    
      this.getColumnModel().getColumn(3).setCellRenderer(right);
      this.getColumnModel().getColumn(4).setCellRenderer(center);
      this.getColumnModel().getColumn(5).setCellRenderer(right);
      this.getColumnModel().getColumn(6).setCellRenderer(center); 
      this.getColumnModel().getColumn(7).setCellRenderer(right);
      this.getColumnModel().getColumn(8).setCellRenderer(right);
      this.getColumnModel().getColumn(9).setCellRenderer(center);    
      this.getColumnModel().getColumn(10).setCellRenderer(right);    
      this.getColumnModel().getColumn(11).setCellRenderer(right);
      this.getColumnModel().getColumn(12).setCellRenderer(center);
      this.getColumnModel().getColumn(13).setCellRenderer(right);
      this.getColumnModel().getColumn(14).setCellRenderer(right);
      this.getColumnModel().getColumn(15).setCellRenderer(center);
      
      this.invalidate();
      this.validate();

      // fills first column with game numbers
      for (int index = 1; index < this.getModel().getRowCount(); index++) {
	  this.getModel().setValueAt(index, index, 0);
      }
  }

  public void resize(float scale, FontMetrics fontMetrics) {

    this.fontMetrics = fontMetrics;
    Font resultTableFont = this.getFont();

    String[] columnWidths = new String[] {
	    "99", "99", "99", "99", "MMMMMMMM", "-999", "MMMMxxxx", "99", "99",
	    "MMMMxxxx", "99", "99", "MMMMxxxx", "99", "99", "M"
	};

    for (int index = 0; index < this.getColumnModel().getColumnCount(); index++) {
      int width = Math.max(fontMetrics.stringWidth("I"+columnWidths[index]),
                           fontMetrics.stringWidth("I"+resultTableHeaders[index]));
      this.getColumnModel().getColumn(index).setPreferredWidth(width);
    }

    int height = (int)Math.round((resultTableFont.getSize() + 1)*1.1);
    this.setRowHeight(height);
    this.invalidate();
    this.revalidate();
    Dimension d1 = this.getPreferredSize();
    Dimension d2 = this.getTableHeader().getPreferredSize();
    this.setPreferredSize(new Dimension(d1.width, d1.height + d2.height + 5));
    this.invalidate();
    this.revalidate();   
  }
}