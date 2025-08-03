/*
 * displays table 
 *
 * (c) Michael Buro, licensed under GPLv3
 *
 * This class has been added to the Skat GUI so that it can be used at live
 * tournaments (to test the performance of kermit against top-level players).
 * Most of the previous layout issues for tournament tables have been resolved.
 * The "Talk/Results" tab might still be shuffled around a bit, but I think
 * that it's reasonably close to its final form to commit this.
 *
 * More tournament functionality will be added
 * later, including the ability to organize a full Skat tournament.  For
 * example, in the first round players will be randomly assigned to tables,
 * and subsequently they will be assigned to tables based on their tournament
 * ranking.  Also, some of the functionality which has been removed for live
 * tournaments -- most notably, the "noob" mode -- will be replaced for online
 * tournament mode.  In the case of noob mode, the rationale is that players
 * who cannot be seen by their opponents (as opposed to live tournaments, where
 * they will be sitting at the same table) can write down moves on paper
 * and thus keep track of cards without noob mode.
 *
 * The most notable modifications which have thus far been made, other than the
 * removal of noob mode, are the removal of the "34" button and all functionality
 * associated with it (there are separate buttons in the tournament mode of
 * ClientWindow to create either a 3-player or a 4-player tournament table, and the
 * number of players allowed at a given table cannot be changed thereafter) and the
 * splitting of the table-panel components into two tabs.  The main panel has only
 * essential game-play components, including the card panels, declaration and bidding
 * panels, as well as buttons to resign, show cards, invite, and "Ready".  The results
 * table, buttons such as "Leave" and "Help", and chat area have been moved to a
 * separate tab entitled "Talk/Results".
 *
 * - Ryan Lagerquist */

package gui;

import common.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.net.*;
import net.miginfocom.layout.PlatformDefaults;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;

public class TournamentTablePanel extends TablePanelBase
  implements EventHandler,WindowListener,ComponentListener
{
  ClientWindow clientWindow;
  JFrame tableFrame;
  String clientName;
  String playerName;
  String cardDeckPath;
  String cardDeck;
  String jackStr;
  boolean useGermanSuitNames; // club->eichel ...
  boolean redBlack; // alternate red/black for 2-color decks (E2,G2)
  boolean isPlayer;
  boolean noob;
  int gameType;
  Table table;
  int historyIndex;  // mouse clicks on tricks change this value
  int remCardsIndex; // for differentiating clicks on tricks / remaining cards
  int panelIndex;    // mouse clicks on hand/trick panel change this value, <0: trick
  int lastBeep;      // >= 0: to move clock TIMER events
  int numIncrements = 0; // to allow bidder to increment/decrement next bid  
  CardImages cardImagesTrickSmall, cardImagesTrickBig;
  CardImages[] cardImages = new CardImages[3];
  Color defaultBackgroundColor, defaultForegroundColor;
  CardPanel[] handPanels = new CardPanel[3];
  JLabel[] labels = new JLabel[10]; // trick annotations
  JButton pickUpButton = new JButton();
  Color color1 = new Color(255,83,83);  // me to move
  Color color2 = new Color(81,113,255); // other to move

  float scale = 1.0f; // magnification scale (changed in resize)
  int[] yOffsets = new int[] { 19, 38, 38 }; // for cards in panels (changed in resize)
  
  JTextField textFieldsA[] = new JTextField[3];
  JTextField textFieldsB[] = new JTextField[3];  
  
  final static String resourceFile = "data/i18n/gui/TableWindow";

  final static String[] posNames = new String[] {
    java.util.ResourceBundle.getBundle(resourceFile).getString("FH"),
    java.util.ResourceBundle.getBundle(resourceFile).getString("MH"),
    java.util.ResourceBundle.getBundle(resourceFile).getString("RH") };
  final static String[] posNamesShort = new String[] {
    java.util.ResourceBundle.getBundle(resourceFile).getString("F"),
    java.util.ResourceBundle.getBundle(resourceFile).getString("M"),
    java.util.ResourceBundle.getBundle(resourceFile).getString("R")
  };  

  static Map<String, CardImages> cardMap = new TreeMap<String, CardImages>();
  
  static String FROM_0 = "0";
  static String FROM_1 = "1";
  static String FROM_2 = "2";
  static String FROM_TRICK = "trick";  

  Card[] discardCards = new Card[2];   // cards selected for discard
  Card[] origSkatCards = new Card[2];  // original skat cards
  java.util.ResourceBundle bundle;
  
  String rbs(String s) { return bundle.getString(s); }

  String[] resultTableHeaders;
  
  /** Creates new form TournamentTablePanel */
  public TournamentTablePanel(ClientWindow clientWindow_,
                              JFrame tableFrame_,
                              String clientName_,
                              Table table_,
                              String cardDeckPath_,
                              String cardDeck_,
                              String invites)
  {

    clientWindow = clientWindow_;
    tableFrame = tableFrame_;
    clientName = clientName_;
    table = table_;
    playerName = table.getViewerName();
    isPlayer = !playerName.equals(Table.NO_NAME);
    if (table.getPlayerNum() == 3)
	tournamentResultTable = new ThreeResultTable(table);
    else
	tournamentResultTable = new FourResultTable(table);
      
    bundle = java.util.ResourceBundle.getBundle(resourceFile);

    cardDeckPath = cardDeckPath_;
    cardDeck = cardDeck_;    

    useGermanSuitNames = cardDeck.equals("GG");

    if      (cardDeck.charAt(0) == 'E') jackStr = "J";
    else if (cardDeck.equals("GG"))     jackStr = "U";
    else                                jackStr = "B";

    redBlack = cardDeck.equals("E2") || cardDeck.equals("G2") || cardDeck.equals("A2");

    // Misc.msg("XXX " + cardDeck + " " + jackStr + " " + redBlack);
    
    resultTableHeaders = new String [] {
      rbs("Player"),
      rbs("Lang."),
      rbs("Pos"),
      rbs("Wins"),
      rbs("Losses"),
      rbs("Prev"),
      rbs("Total"),
      "Ready" // will have to be changed for i18n - Ryan
    };
    
    initComponents();

    if (isPlayer) {
      textArea.append(rbs("invite_msg"));
    } else {
      textArea.append(rbs("observe_msg"));      
    }

    textFieldsA[0] = textField0A;
    textFieldsB[0] = textField0B;   
    textFieldsA[1] = textField1A;
    textFieldsB[1] = textField1B;   
    textFieldsA[2] = textField2A;
    textFieldsB[2] = textField2B;

    addComponentListener(this);
    
    playingLabel.setText("                 ");
      
    // create pickup button for trickCardPanel

    pickUpButton.setText(rbs("Pickup_Skat")); // NOI18N
    pickUpButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
    pickUpButton.setVisible(false);

    leaveButton.setEnabled(true);
    readyButton.setEnabled(true);    
    resignButton.setEnabled(false);
    showButton.setEnabled(false);
    declareButton.setEnabled(false);
    declareButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
    bidButton1.setMargin(new java.awt.Insets(2, 5, 2, 5));
    bidButton2.setMargin(new java.awt.Insets(2, 5, 2, 5));

    /* Gave small margins to the bottom and right sides of the button, because
       otherwise the "+" sign is off-center. - Ryan */
    bidIncrementButton.setMargin(new java.awt.Insets(0, 0, 1, 1));
    bidDecrementButton.setMargin(new java.awt.Insets(0, 0, 0, 0));

    // Enlarged margins so that the buttons would fill up space in the Talk/Results tab. - Ryan
    helpButton.setMargin(new java.awt.Insets(5, 20, 5, 20));
    leaveButton.setMargin(new java.awt.Insets(5, 20, 5, 20));    

    suitPanel0.setVisible(false);
    suitPanel1.setVisible(false);
    suitPanel2.setVisible(false);

    // set cell policies in result table
      
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

    /* For live tournament settings, the results table will not need IP addresses,
       the talk flag (since players will be able to talk across the table), or
       the 34 flag (since changing the number of players at the table will be
       disallowed).  Therefore, the results table will be without these 3 columns. */
    resultTable.setModel(new javax.swing.table.DefaultTableModel(new Object [][] {
          {null, null, null, null, null, null, null, null, null},
          {null, null, null, null, null, null, null, null, null},
          {null, null, null, null, null, null, null, null, null},
          {null, null, null, null, null, null, null, null, null}
        },
        resultTableHeaders
        ) {
        Class[] types = new Class [] {
          String.class,
          String.class,
          Integer.class,
          Integer.class,
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

    resultTable.getColumnModel().getColumn(0).setCellRenderer(left);
    resultTable.getColumnModel().getColumn(1).setCellRenderer(center);    
    resultTable.getColumnModel().getColumn(2).setCellRenderer(center);    
    resultTable.getColumnModel().getColumn(3).setCellRenderer(right);
    resultTable.getColumnModel().getColumn(4).setCellRenderer(right);
    resultTable.getColumnModel().getColumn(5).setCellRenderer(right);
    resultTable.getColumnModel().getColumn(6).setCellRenderer(right);
    resultTable.getColumnModel().getColumn(7).setCellRenderer(center);

    resultTable.invalidate();
    resultTable.validate();    

    handPanels[0] = cardPanel0;
    handPanels[1] = cardPanel1;
    handPanels[2] = cardPanel2;

    defaultBackgroundColor = getBackground();
    defaultForegroundColor = getForeground();    

    trickCardPanel.setCardBackground(defaultBackgroundColor);
    trickCardPanel.setBackground(defaultBackgroundColor);    

    for (int i=0; i < 3; i++) {
      handPanels[i].setCardBackground(defaultBackgroundColor);
      handPanels[i].setBackground(defaultBackgroundColor);      
    }

    // input: send tell message to server
    inputLine.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent e) {

          String input = inputLine.getText();
          
          send("tell " + input);
          inputLine.setText("");
          inputLine.setCaretPosition(inputLine.getDocument().getLength());
          inputLine.requestFocus();

          // restored invite functionality for tournament mode

          if (input.startsWith("@invite ")) {
	    send(input.substring(1)); // remove @ and send invitation
	    textArea.append("\n# " + input);
          }
        }}
      );
    
    // buttons
    
    pickUpButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("pickup clicked");

          synchronized (clientWindow.sc) {
            
            SimpleGame sg = table.getGame();
            if (sg == null) return;
	    
            SimpleState st = sg.getCurrentState();
            if (st.getPhase() == SimpleState.SKAT_OR_HAND_DECL) {
              send("play s");
              lastBeep = 0; // action stops beep timer
            }
          }
        }});

    bidButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("bid1 clicked");

          synchronized (clientWindow.sc) {          
            SimpleGame sg = table.getGame();
            if (sg == null || !table.getInProgress())
              return;
	    
            SimpleState st = sg.getCurrentState();
            if (st.getPhase() == SimpleState.BID) {

              if (st.nextBid(numIncrements) >= 0) {

                send("play " + st.nextBid(numIncrements)); // send bid
                lastBeep = 0; // action stops beep timer
              }
              
            } else if (st.getPhase() == SimpleState.ANSWER) {
              send("play y"); // yes
              lastBeep = 0; // action stops beep timer
            }

            numIncrements = 0;
          }
        }});

    bidButton2.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("bid2 clicked");

	  synchronized(clientWindow.sc) {
	    SimpleGame sg = table.getGame();
	    if (sg == null || !table.getInProgress())
	      return;
          }
          
          send("play p"); // pass 
          lastBeep = 0; // action stops beep timer
          numIncrements = 0;          
        }});


    // Allows bidder to make next bid larger than next legal bid. - Ryan
    bidIncrementButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          synchronized(clientWindow.sc) {
            SimpleGame sg = table.getGame();
            if (sg == null || !table.getInProgress()) {
              return;
            }

            SimpleState st = sg.getCurrentState();
            if (st.getPhase() != SimpleState.BID) {
              return;
            }

            if (st.nextBid(++numIncrements) < 0) --numIncrements;
            if (st.nextBid(++numIncrements) < 0) --numIncrements;
            if (st.nextBid(++numIncrements) < 0) --numIncrements;
            if (st.nextBid(++numIncrements) < 0) --numIncrements;            
            
            game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
          }
        }});

    bidDecrementButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          synchronized(clientWindow.sc) {
            SimpleGame sg = table.getGame();
            if (sg == null || !table.getInProgress()) {
              return;
            }

            SimpleState st = sg.getCurrentState();
            if (st.getPhase() != SimpleState.BID) {
              return;
            }

            if (numIncrements <= 0) return;
            --numIncrements;
            game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);            
          }
        }});

    grandButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("grand clicked");
          gameType = GameDeclaration.GRAND_GAME;
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});

    clubsButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("clubs clicked");
          gameType = GameDeclaration.CLUBS_GAME;
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});
    clubsLabel.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("clubs label clicked");
          gameType = GameDeclaration.CLUBS_GAME;          
          clubsButton.doClick();
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);          
        }});

    spadesButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("spades clicked");
          gameType = GameDeclaration.SPADES_GAME;
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});
    spadesLabel.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("spades label clicked");
          gameType = GameDeclaration.SPADES_GAME;          
          spadesButton.doClick();
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);          
        }});

    heartsButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("hearts clicked");
          gameType = GameDeclaration.HEARTS_GAME;
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});
    heartsLabel.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("hearts label clicked");
          gameType = GameDeclaration.HEARTS_GAME;          
          heartsButton.doClick();
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);          
        }});

    diamondsButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("diamonds clicked");
          gameType = GameDeclaration.DIAMONDS_GAME;
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});
    diamondsLabel.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("diamonds label_clicked");
          gameType = GameDeclaration.DIAMONDS_GAME;          
          diamondsButton.doClick();
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);          
        }});

    nullButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("null clicked");
          gameType = GameDeclaration.NULL_GAME;
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});

    handCheckBox.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("hand clicked");	  
        }});
    schneiderCheckBox.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("schneider clicked");	  
        }});
    schwarzCheckBox.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("schwarz clicked");	  
        }});
    
    //ouvertCheckBox.addMouseListener(new MouseAdapter() {
    //    @Override
    //      public void mouseClicked(MouseEvent evt) {
    //      Misc.msg("ouvert clicked");
    //      game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);	  
    //    }});

    declareButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("declare clicked");
	  GameDeclaration gd;
	  boolean hand;
	  int maxBid;
          boolean not2Cards = false;
          
	  synchronized (clientWindow.sc) {
	  
	    SimpleGame sg = table.getGame();
	    if (sg == null || !table.getInProgress())
	      return;
	    
	    SimpleState st = sg.getCurrentState();
	    if (st.getPhase() != SimpleState.SKAT_OR_HAND_DECL &&
		st.getPhase() != SimpleState.DISCARD_AND_DECL)
	      return;

	    maxBid = st.getMaxBid();
	    hand = st.getPhase() == SimpleState.SKAT_OR_HAND_DECL;

            int gt = gameTypeFromButtons();
            
	    gd = new GameDeclaration(gt, hand,
				     ouvertCheckBox.getSelectedObjects()    != null,
				     schneiderCheckBox.getSelectedObjects() != null,
				     schwarzCheckBox.getSelectedObjects()   != null);

	    if (!hand) {
	      if (discardCards == null ||
		  discardCards[0] == null ||
		  discardCards[1] == null)
                not2Cards = true;
	    }
	  }

          if (not2Cards) {

	    JOptionPane.showMessageDialog(getThis(),
					  "<html><b>"+rbs("discard_error_message")+"</html>",
					  rbs("Table_Error_Message"),
					  JOptionPane.ERROR_MESSAGE);
            return;
          }
          
	  Object[] options = { "<html><b>"+rbs("Yes")+"</html>",
			       "<html><b>"+rbs("No")+"</html>" };

	  int n = JOptionPane.
	    showOptionDialog(declarationPanel, //getThis(),
			     String.format("<html><b>" + rbs("want_to_play") + "</html>",
                                           rbs((useGermanSuitNames ? "GERMAN_" : "")+gd.typeToVerboseString()),
                                           gd.modifiersToVerboseString(),
                                           maxBid),
			     rbs("Announce_contract"),
			     JOptionPane.YES_NO_OPTION,
			     JOptionPane.QUESTION_MESSAGE,
			     null,     //do not use a custom Icon
			     options,  //the titles of buttons
			     options[0]); //default button title
	  if (n == 0) {

            lastBeep = 0;
            
	    if (hand)
	      send("play " + gd.toString());
	    else 
	      send("play " + gd.toString() + "." +
		   discardCards[0].toString() + "." +
		   discardCards[1].toString());
	  }
	}});


    helpButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Misc.msg("help clicked");
          Misc.openURL(Misc.issHome + "/" + rbs("skatgui-help.html"));
	}
      });
    leaveButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Misc.msg("leave clicked");
          send("leave");          
	}
      });

    plusButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("plus clicked");
          clientWindow.sizeDelta(+5);
        }});

    minusButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("minus clicked");
          clientWindow.sizeDelta(-5);
        }});

    inviteButton.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("invite clicked");
	  String s = clientWindow.getSelectedNames();
	  if (!s.equals("")) {
	    send("invite " + s);
	    textArea.append("\n# @invite" + s);
            //clientWindow.clearNameSelection();
	  } else {
	    // no selection
	    JOptionPane.showMessageDialog(getThis(),
					  "<html><b>"+rbs("invite_error_message")+"</html>",
					  rbs("Table_Error_Message"),
					  JOptionPane.ERROR_MESSAGE);
	  }
        }});    
    
    readyButton.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("ready clicked");
          send("ready");

          /* In game2Window, if all players are ready and the game is just beginning,
             the game tab will be automatically selected. - Ryan */
          game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
        }});

    resignButton.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("shortcut clicked");

	  synchronized (clientWindow.sc) {
	  
	    SimpleGame sg = table.getGame();
	    if (sg == null || !table.getInProgress() ||
                sg.getCurrentState().getPhase() != SimpleState.CARDPLAY)
	      return;
          }
          
	  Object[] options = { "<html><b>" + rbs("Yes") + "</html>",
			       "<html><b>" + rbs("No") + "</html>" };

	  int n = JOptionPane.
	    showOptionDialog(resignButton, //getThis(),
			     "<html><b>" + rbs("remaining_tricks?") + "</html>",
			     rbs("Game_shortcut"),
			     JOptionPane.YES_NO_OPTION,
			     JOptionPane.QUESTION_MESSAGE,
			     null,     //do not use a custom Icon
			     options,  //the titles of buttons
			     options[0]); //default button title

          if (n == 0) {
            send("play RE");
          }}});
    
    showButton.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("show clicked");

	  synchronized (clientWindow.sc) {
	  
	    SimpleGame sg = table.getGame();
	    if (sg == null || !table.getInProgress() ||
                sg.getCurrentState().getPhase() != SimpleState.CARDPLAY)
	      return;
          }

	  Object[] options = { "<html><b>" + rbs("Yes") + "</html>",
			       "<html><b>" + rbs("No")  + "</html>" };
	  int n = JOptionPane.
	    showOptionDialog(showButton, //getThis(),
			     "<html><b>" + rbs("want_show_cards?") + "</html>",
			     rbs("Show_cards"),
			     JOptionPane.YES_NO_OPTION,
			     JOptionPane.QUESTION_MESSAGE,
			     null,     //do not use a custom Icon
			     options,  //the titles of buttons
			     options[0]); //default button title

          if (n == 0) {
            send("play SC");
          }}});
    
    clubsButton.doClick();
    gameType = GameDeclaration.CLUBS_GAME;
    
    // cardpanel callback

    cardPanel0.setHandler(FROM_0, this);
    cardPanel1.setHandler(FROM_1, this);
    cardPanel2.setHandler(FROM_2, this);
    trickCardPanel.setHandler(FROM_TRICK, this);    
    
    setVisible(true);

    game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);

    Misc.msg("#PLAYERS = " + table.numOfPlayersAtTable());
    
    // restored invite functionality for tournament mode
    
    if (invites == null || invites.equals("")) return;

    send("invite " + invites);
    textArea.append("\n# @invite" + invites);
  }

  ImageIcon getSuitIcon(String suit, int width)
  {
    String file = cardDeckPath + "/" + cardDeck + "/" + suit+".gif";

    // Misc.msg("suit image file " + file);
    
    URL url = Thread.class.getResource(file);
    if (url == null) Misc.err(rbs("can't_access_suit_image " + suit));
    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                         getScaledInstance(width, -1, Image.SCALE_SMOOTH));
  }

  CardImages getCardImages(int width)
  {
    String key = cardDeck + " " + width;
    CardImages ci = cardMap.get(key);
    
    if (ci == null) {
      // create card images and cache them
      ci = new CardImages();
      ci.loadCards(cardDeckPath + "/" + cardDeck, width);
      cardMap.put(key, ci);
    }

    return ci;
  }
  
  void setCardImages()
  {
    // create card images

    cardPanel.invalidate(); // hack (resize doesn't do this?!)
    cardPanel.validate();

    CardImages cardImages0  = getCardImages(cardPanel0.getUsableWidth() / 8);  // big
    CardImages cardImages12 = getCardImages(cardPanel1.getUsableWidth() / 6);  // medium
    cardImagesTrickSmall = getCardImages(trickCardPanel.getUsableWidth() / 15); // trick small
    cardImagesTrickBig   = getCardImages((trickCardPanel.getUsableHeight() * 9) / 20); // trick big
    
    cardImages[0] = cardImages0;
    cardImages[1] = cardImages12;
    cardImages[2] = cardImages12;    
  }
  
  public void send(String msg)
  {
    clientWindow.sc.send("table " + table.getId() + " " + playerName + " " + msg);
  }

  // handle messages from components
  
  public boolean handleEvent(EventMsg msg)
  {
    synchronized (clientWindow.sc) {
    
      SimpleGame sg = table.getGame();

      if (sg != null) 
        Misc.msg("TABLE_WINDOW_HANDLE_EVENT: " + msg.getSender() + ": "
                 + msg.getArgs().get(0) + " phase=" + sg.getCurrentState().getPhase());
    
      if (msg.getSender().equals(FROM_0)) {

        // card panel 0

        panelIndex = 0;

        {
          for (;;) { // to use break
          
            if (sg == null)
              break; // no game

            if (msg.getArgs().get(0).equals("")) {
              break; // no card
            }
          
            SimpleState st = sg.getCurrentState();
            if (!st.isViewerToMove()) {
              break; // not me to move
            }
          
            Card card = new Card(Integer.parseInt(msg.getArgs().get(1)),
                                 Integer.parseInt(msg.getArgs().get(2)));
          
            int phase = st.getPhase();
          
            if (phase == SimpleState.CARDPLAY) {
            
              send("play " + card.toString());
              lastBeep = 0;
            
            } else if (phase == SimpleState.DISCARD_AND_DECL) {
            
              // discarding action:
              // move card from hand to skat, move skat to right, move skat card to hand
            
              if (card.equals(discardCards[0]))
                discardCards[0] = null;
              else if (card.equals(discardCards[1]))
                discardCards[1] = null;
              else if (discardCards[0] == null)
                discardCards[0] = card;
              else if (discardCards[1] == null)
                discardCards[1] = card;
            }

            break;
          }
        }

      } else if (msg.getSender().equals(FROM_1)) {

        // card panel 1

        panelIndex = 1;
      
      } else if (msg.getSender().equals(FROM_2)) {

        // card panel 2

        panelIndex = 2;
      
      } else if (msg.getSender().equals(FROM_TRICK)) {

        panelIndex = -1;

        for (;;) { // to use break
      
          // trick panel

          if (sg == null) break;
          if (msg.getArgs().get(0).equals(""))
            break; // no card

          if (table.getInProgress()) {

            // move
        
            SimpleState st = sg.getCurrentState();
            if (!st.isViewerToMove())
              break; // not me to move
        
            if (st.getPhase() == SimpleState.SKAT_OR_HAND_DECL) {
              send("play s");
              lastBeep = 0;
            }
        
          } else {

            // select history point

            int ind = Integer.parseInt(msg.getArgs().get(0));
            if (ind < remCardsIndex) {
              historyIndex = ind;
              if (historyIndex < 2) historyIndex = 0; // deal
              else if (historyIndex < 7) historyIndex = 1; // before cardplay
              else historyIndex = (historyIndex - 7) / 3 + 2;
            }
          }
          break;
        }
      }
    }      
    game2Window(ClientWindow.TablePanelUpdateAction.UNKNOWN, null);        
    return true;
  }

  public void windowClosing(WindowEvent e) {
    synchronized (clientWindow.sc) {
      if (!table.getInProgress() || table.playerInGameIndex(playerName) < 0) {
        send("leave");
      }
    }
  }

  public void windowClosed(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowActivated(WindowEvent e) {}
  public void windowDeactivated(WindowEvent e) {}
  public void windowGainedFocus(WindowEvent e) {}
  public void windowLostFocus(WindowEvent e) {}
  public void windowStateChanged(WindowEvent e) {}

  public void componentHidden(ComponentEvent evt) {}
  public void componentMoved(ComponentEvent evt) {}
  public void componentShown(ComponentEvent evt) {}  
  
  public void componentResized(ComponentEvent evt) {

    if (true) return;
    
    Component c = (Component)evt.getSource();
    // Get new size
    Dimension newSize = c.getSize();

    // Misc.msg("NEW SIZE = " + c.getWidth() + " " + c.getHeight());
    invalidate();
    validate();
    
    game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
  }
  
  // assumes table is locked
  private void setHandTitles(SimpleState st, int myIndex)
  {
    SimpleState ste = table.getGame().getCurrentState();

    for (int p=0; p < 3; p++) { // player index in game
      
      String titleA = posNames[p] + " " + table.playerInGame(p);
      String titleB = "";
      
      if (ste.getDeclarer() >= 0) {
        
        if (ste.getDeclarer() == p) {
          titleB += rbs("Decl.");
          if (ste.getGameDeclaration().type != GameDeclaration.NO_GAME)
            titleB += " | " + rbs((useGermanSuitNames ? "GERMAN_" : "")+ste.getGameDeclaration().typeToVerboseString()) +
              ste.getGameDeclaration().modifiersToVerboseString();
        } else {
          titleB += rbs("Def.");
        }
      }

      int mb = ste.getMaxBid(p);
      //     Misc.msg("MB " + p + " " + mb);
      if (mb == 0)
        titleA += " | ?";
      else if (mb == 1)
        titleA += " | " + rbs("Pass");
      else
        titleA += " | " + mb;

      if (ste.getResigned(p) != 0) { titleA += " | " + rbs("Resigned"); }
      if (ste.getLeft() == p)      { titleA += " | " + rbs("Left"); }
      if (ste.getTimeOut() == p)   { titleA += " | " + rbs("Timeout"); }      
      
      StringBuilder sb = new StringBuilder();
      Formatter formatter = new Formatter(sb, Locale.US);
      int secs = (int)Math.round(table.remainingTime(p));
      
      formatter.format("%s%d:%02d", (secs < 0 ? "-" : ""), Math.abs(secs)/60, Math.abs(secs) % 60);
      titleA += " | " + sb.toString();

      int toMove = st.getToMove(); // trick leader

      // tomove indication
      
      if (toMove == p) {
        int phase = ste.getPhase();

        if (titleB.length() != 0)
          titleB += " | ";
        
        switch (phase) {
        case SimpleState.CARDPLAY: titleB += rbs("card?"); break;
        case SimpleState.BID:      titleB += rbs("bid?"); break;
        case SimpleState.ANSWER:   titleB += rbs("answer?"); break;
        case SimpleState.SKAT_OR_HAND_DECL: titleB += rbs("skat_or_declare?"); break;
        case SimpleState.DISCARD_AND_DECL:  titleB += rbs("discard_and_declare"); break;
        }
      }

      Color fg, bg;
      Color toMoveColor =
        table.getInProgress() && SimpleState.isPlayer(table.getGame().getOwner()) &&
        myIndex == p ? color1 : color2;

      if (table.getInProgress() && !SimpleState.isPlayer(table.getGame().getOwner()))
        toMove = ste.getToMove(); // observing: consider current player to move
      
      if (toMove == p) {
        bg = fg = toMoveColor;
      } else {
        bg = defaultBackgroundColor;
        fg = defaultForegroundColor;
      }

      int pindex = (3-Math.max(myIndex,0)+p) % 3;
      textFieldsA[pindex].setText(titleA);
      textFieldsB[pindex].setText(titleB);
      handPanels[pindex].setCardBackground(bg);
    }
  }

  enum DisplayMode { HISTORY, OBSERVING, PLAYING };
    
  // get game type from radio buttons
  
  int gameTypeFromButtons()
  {
    if (diamondsButton.isSelected())
      return GameDeclaration.DIAMONDS_GAME;

    if (heartsButton.isSelected())
      return GameDeclaration.HEARTS_GAME;

    if (spadesButton.isSelected())
      return GameDeclaration.SPADES_GAME;

    if (clubsButton.isSelected())
      return GameDeclaration.CLUBS_GAME;

    if (grandButton.isSelected())
      return GameDeclaration.GRAND_GAME;

    if (nullButton.isSelected())
      return GameDeclaration.NULL_GAME;

    Misc.err("illegal game type button selection");
    return GameDeclaration.NO_GAME;
  }
  
  // set radio buttons to game type when playing cards
  // assumes table is locked
  void setDeclarationButtons()
  {
    SimpleGame sg = table.getGame();
    if (sg.getGameDeclaration().type != GameDeclaration.NO_GAME) {

      gameType = sg.getGameDeclaration().type;
      
      switch (gameType) {
      case GameDeclaration.DIAMONDS_GAME: diamondsButton.doClick(); break;
      case GameDeclaration.HEARTS_GAME:   heartsButton.doClick(); break;
      case GameDeclaration.SPADES_GAME:   spadesButton.doClick(); break;
      case GameDeclaration.CLUBS_GAME:	  clubsButton.doClick(); break;
      case GameDeclaration.NULL_GAME:	  nullButton.doClick(); break;
      case GameDeclaration.GRAND_GAME:	  grandButton.doClick(); break;
      }
      
      handCheckBox.setSelected(sg.getGameDeclaration().hand);
      ouvertCheckBox.setSelected(sg.getGameDeclaration().ouvert);
      schneiderCheckBox.setSelected(sg.getGameDeclaration().schneiderAnnounced);
      schwarzCheckBox.setSelected(sg.getGameDeclaration().schwarzAnnounced);
    }

    if (!sg.isFinished()) {
      int phase = sg.getCurrentState().getPhase();
      switch (phase) {
      case SimpleState.BID:
        bidDecrementButton.setEnabled(true);
        bidIncrementButton.setEnabled(true);
        break;
                
      case SimpleState.ANSWER:

        handCheckBox.setSelected(true);
        ouvertCheckBox.setSelected(false);
        schneiderCheckBox.setSelected(false);
        schwarzCheckBox.setSelected(false);
        bidDecrementButton.setEnabled(false);
        bidIncrementButton.setEnabled(false);        
        break;
        
      case SimpleState.DISCARD_AND_DECL:

        handCheckBox.setSelected(false);
        schneiderCheckBox.setSelected(false);
        schwarzCheckBox.setSelected(false);

        if (gameType != GameDeclaration.NULL_GAME) {
          ouvertCheckBox.setSelected(false);
        }
        break;

      case SimpleState.SKAT_OR_HAND_DECL: 

        handCheckBox.setSelected(true);
        if (gameType == GameDeclaration.NULL_GAME) {

          schneiderCheckBox.setSelected(false);
          schwarzCheckBox.setSelected(false);          

        } else {
          
          if (ouvertCheckBox.isSelected()) {
            schwarzCheckBox.setSelected(true);
          }
          if (schwarzCheckBox.isSelected()) {
            schneiderCheckBox.setSelected(true);
          }
        }
        
        break;
      }
    }
  }


  public void game2Window(ClientWindow.TablePanelUpdateAction action, String[] params)
  {
    
    synchronized (clientWindow.sc) {

      setCardImages();
      
      if (action == ClientWindow.TablePanelUpdateAction.TIMER) {

        if (!table.getInProgress()) {
          lastBeep = 0;
          return;
        }

        if (table.getGame() == null) {
          lastBeep = 0;
          return;
        }
        
        for (;;) { // for break
          
          SimpleState st = table.getGame().getCurrentState();
          if (!st.isViewerToMove()) { // not me to move
            lastBeep = 0;
            break;
          }
          
          if (lastBeep <= 0) {
            lastBeep = 1; // start ticking
            break;
          }
          
          lastBeep++;
          if ((lastBeep % 25) == 1) { // beep after x seconds
            Misc.beep(1500, 80, 64);
          }
          break;
        }
      
      } else if (action == ClientWindow.TablePanelUpdateAction.ERROR) {

        // report error in dialog

        JOptionPane.showMessageDialog(this,
                                      "<html><b>"+params[0]+"</html>",
                                      rbs("Table_Error_Message"),
                                      JOptionPane.ERROR_MESSAGE);

      } else if (action == ClientWindow.TablePanelUpdateAction.START) {        

        clubsButton.doClick();
        gameType = GameDeclaration.CLUBS_GAME;
        
      } else if (action == ClientWindow.TablePanelUpdateAction.TELL) {

        // append message to text area

        textArea.append("\n" + params[0] + ": " + params[1]);
        textArea.setCaretPosition(textArea.getDocument().getLength());
        if (!params[0].equals(playerName) && isPlayer)
          Misc.beep(700, 100, 20);
      }

      // game number

      gameNumberLabel.setText(rbs("Game") + " " + table.getGameNum());
      // sets title of main game tab with game number - Ryan
      tableTabbedPane.setTitleAt(1, rbs("Game") + " " + table.getGameNum());
      
      // fill result table

      for (int i=0; i < Table.getMaxPlayerNum(); i++) {
        
        if (i == 3 && table.getPlayerNum() == 3) {
          for (int j=0; j < resultTable.getColumnModel().getColumnCount(); j++) {
            resultTable.getModel().setValueAt("-", i, j);
          }
          continue;
        }

        // player name and position

        String p = table.getPlayersInResultTable()[i];
        ClientData cd = table.getClientData(p);
        if (cd == null)
          resultTable.getModel().setValueAt("", i, 1);
        else
          resultTable.getModel().setValueAt(cd.languages, i, 1);
          
        if (table.getPlayerAtTable(i) == null) {
          resultTable.getModel().setValueAt("["+p+"]", i, 0);
          resultTable.getModel().setValueAt("", i, 2);
          
        } else {
          resultTable.getModel().setValueAt(p, i, 0);

          p = "";
          int pi = table.playerInGameIndex(table.getPlayerAtTable(i));
          if (pi >= 0) p = posNames[pi];
          resultTable.getModel().setValueAt(p, i, 2);
        }
        
        // wins
        resultTable.getModel().setValueAt(table.getWins(i)+"", i, 3);
      
        // losses
        resultTable.getModel().setValueAt((table.getPlayed(i)-table.getWins(i))+"", i, 4);
      
        // last points
        resultTable.getModel().setValueAt(table.getLastPts(i)+"", i, 5);
      
        // total points
        resultTable.getModel().setValueAt(table.getTotalPts(i)+"", i, 6);

        // ready
        resultTable.getModel().setValueAt(table.getReady(i) ? "X" : "", i, 7);
      }

      SimpleGame sg = table.getGame();
      if (sg == null) return; // no game

      // display game state

      DisplayMode mode;
      int myIndex = sg.getPlayerIndex(playerName); // < 0 means observer (converted to 0 by max) when nece.

      if (action == ClientWindow.TablePanelUpdateAction.MOVE) {

        // reset panel index when move arrives        

        if (myIndex < 0)
          panelIndex = -1; // public view
        else 
          panelIndex = 0; // player view

        // show dialog if partner resigned just now
        SimpleState st = sg.getCurrentState();
        if (myIndex >= 0 && st.getDeclarer() >= 0 && st.getDeclarer() != myIndex &&
            st.getResigned(0) + st.getResigned(1) + st.getResigned(2) == 1 &&
            st.getResigned(myIndex) == 0 &&
            st.getResigned(st.getDeclarer()) == 0 &&
            sg.getStateNum() >= 2) {

          // a defender resigned

          // just now?

          st = sg.getState(sg.getStateNum()-2);
          if (st.getResigned(0) + st.getResigned(1) + st.getResigned(2) == 0) {

            Object[] options = { "<html><b>"+ rbs("Yes") + "</html>",
                                 "<html><b>"+ rbs("No") + "</html>" };
            int n = JOptionPane.
              showOptionDialog(getThis(),
                               "<html><b>" +
                               rbs("Partner_shortcut")+"</html>",
                               rbs("Game_shortcut"),
                               JOptionPane.YES_NO_OPTION,
                               JOptionPane.QUESTION_MESSAGE,
                               null,     //do not use a custom Icon
                               options,  //the titles of buttons
                               options[0]); //default button title

            if (n == 0) {
              send("play RE"); // also shortcut
            }
          }
        }
        

      } else if (action == ClientWindow.TablePanelUpdateAction.END) {

        historyIndex = 1; // was = 0; may be set > 0 from previous history (leading to exception)
        
        // switch to public view in history mode

        panelIndex = -1;

        // display game result in textarea

        SimpleGame g = table.getGame();

        if (g.isFinished()) {

          textArea.append("\n# " + rbs("Game") + " " + table.getGameNum() + " " + rbs("result") + ": ");

          GameDeclaration decl = sg.getCurrentState().getGameDeclaration();
          GameResult gr = new GameResult();
          sg.getCurrentState().gameResult(gr);
          
          if (gr.passed) {
            textArea.append(rbs("all_players_passed"));
          } else if (gr.left >= 0) {
            textArea.append(String.format(rbs("player_left_the_game"), table.playerInGame(gr.left)));
          } else if (gr.timeout >= 0) {
            textArea.append(String.format(rbs("player_timeout"), table.playerInGame(gr.timeout)));
          } else {
            
            textArea.append(String.format(rbs("player_points"),
                                          table.playerInGame(gr.declarer),
                                          gr.declValue+(gr.declValue>0 ? 50 : -50)));

            String type = rbs((useGermanSuitNames ? "GERMAN_" : "") + decl.typeToVerboseString());
            
            textArea.append("\n#   " + type + decl.modifiersToVerboseString());
            
            if (decl.type != GameDeclaration.NULL_GAME) {

              if (gr.matadors > 0)
                textArea.append(" " + rbs("with") + " " + gr.matadors);
              else
                textArea.append(" " + rbs("against") + " " + (-gr.matadors));
            
              if (gr.schwarz) textArea.append(", schwarz");
              else if (gr.schneider) textArea.append(", schneider");

              textArea.append(String.format(", " + rbs("card_points"), gr.declCardPoints));
              if (gr.overbid) textArea.append(", " + rbs("overbid"));
            }
          }

          textArea.setCaretPosition(textArea.getDocument().getLength());
          // Selects the Talk/Results tab so that players can see game results.
          tableTabbedPane.setSelectedIndex(0);
          tournamentResultTable.addGameData(gr, decl);
        }
      }
      
      noob = sg.isFinished() || sg.getOwner() == SimpleState.WORLD_VIEW;

      suitPanel0.setVisible(noob);
      suitPanel1.setVisible(noob);
      suitPanel2.setVisible(noob);

      // Misc.msg(sg.toSgf(false));
      
      // mode label
    
      if (sg.isFinished()) {
      
        playingLabel.setText(rbs("History"));
        playingLabel.setForeground(defaultForegroundColor);
        mode = DisplayMode.HISTORY;
      
      } else {
      
        if (myIndex < 0) {
          playingLabel.setText(rbs("Observing"));
          playingLabel.setForeground(color2);
          mode = DisplayMode.OBSERVING;
        
        } else {
          playingLabel.setText(rbs("Playing"));
          playingLabel.setForeground(color1);
          mode = DisplayMode.PLAYING;
        }
      }

      SimpleState st = sg.getCurrentState();
      boolean mePlaying = mode == DisplayMode.PLAYING && (myIndex >= 0 && st.getDeclarer() == myIndex);
      boolean meToMove = st.isViewerToMove();

      /* The code below switches to the game tab if all players are ready and the game
         has not yet started. - Ryan */

      boolean allReady = true;

      for (int index = 0; index < table.getPlayerNum(); index++) {
        if (!table.getReady(index)) {
          allReady = false;
        }
      }

      // Selects game tab if all players are ready and game has not begun yet. - Ryan
      if ((sg.getCurrentState().getPhase() == SimpleState.BID || sg.getCurrentState().getPhase() == SimpleState.ANSWER)
          && allReady) {
        tableTabbedPane.setSelectedIndex(1);
      }

      // Auto-selects game tab if it is the player's turn to move.
      if (meToMove) tableTabbedPane.setSelectedIndex(1);
      
      Color activeHandPanelColor, toMoveColor;
      toMoveColor = (meToMove && mode == DisplayMode.PLAYING) ? color1 : color2;
      activeHandPanelColor = toMoveColor;
    
      leaveButton.setEnabled(mode != DisplayMode.PLAYING);
      readyButton.setEnabled(mode == DisplayMode.HISTORY);
      inviteButton.setEnabled(mode == DisplayMode.HISTORY);
      resignButton.setEnabled(mode == DisplayMode.PLAYING && st.getPhase() == SimpleState.CARDPLAY);
      showButton.setEnabled(mePlaying && st.getPhase() == SimpleState.CARDPLAY);
      declareButton.setEnabled(mePlaying && (st.getPhase() == SimpleState.SKAT_OR_HAND_DECL ||
                                             st.getPhase() == SimpleState.DISCARD_AND_DECL));
      pickUpButton.setVisible(false);     
      // pickUpButton.invalidate();
      // pickUpButton.validate();
      
      trickCardPanel.remove(pickUpButton);
      trickCardPanel.add(pickUpButton,
                         new org.netbeans.lib.awtextra.AbsoluteConstraints(trickCardPanel.getUsableWidth()/2 -
                                                                           pickUpButton.getWidth()/2 + 10,
                                                                           trickCardPanel.getUsableHeight()/2 + 25 -
                                                                           pickUpButton.getHeight()/2));
      pickUpButton.setVisible(mePlaying && (st.getPhase() == SimpleState.SKAT_OR_HAND_DECL));
      
      biddingPanel.setBorder(BorderFactory.createTitledBorder(rbs("Bidding")));        
      bidButton1.setEnabled(false);
      bidButton2.setEnabled(false);
      bidIncrementButton.setEnabled(false);
      bidDecrementButton.setEnabled(false);

      int[] histStateIndexes = new int[11]; // historyIndex -> state index

      setDeclarationButtons();
    
      // trick panel

      ArrayList<ImagePos> imp = null;
    
      ArrayList<Card> cards = new ArrayList<Card>(); // skat,discard,played cards
      ArrayList<Integer> toMove = new ArrayList<Integer>();  // who played that card
      ArrayList<Integer> ptsDecl = new ArrayList<Integer>();
      ArrayList<Integer> ptsDefs = new ArrayList<Integer>();      

      for (int i=0; i < 10; i++) {
        labels[i].setVisible(false);
      }
      
      // original skat
      
      Card[] skat = sg.getOriginalSkat();
      
      if (skat != null) {
        cards.add(skat[0]);
        cards.add(skat[1]);
      } else {
        cards.add(new Card());
        cards.add(new Card());
      }
      
      int cardPlayStart = sg.getCardPlayBookmark();
      int decl = -1;
      int skatPts = 0;
    
      // 12 slots:  skat / discard / 10 tricks
    
      int w = trickCardPanel.getUsableWidth();
      int h = trickCardPanel.getUsableHeight();
      int cw = cardImagesTrickSmall.getWidth(); // card width
      int ch = cardImagesTrickSmall.getHeight(); // card height
      int x0 = (int)(cw/2*1.9);
      int dx = (w-x0)/10;
      int dy = Math.round(10 * scale);   // pts raised when card played in trick
      int yc = 0 + (int)(1.5*ch/2);
      
      if (cardPlayStart >= 0) {
	
        // game was declared or history
      
        /* "noob" is true only if the game is finished or if the client has "world view".
           This has nothing to do with the noob check box in TablePanel, which has been
           removed, for now, from tournament mode. - Ryan */
        if (!noob) {

          // current trick in the center, previous trick to the left

          ArrayList<Card> trickCards = new ArrayList<Card>();

          st.getCurrentTrick(trickCards);
          imp = Utils.cardsToImages(trickCards, cardImagesTrickBig);

          if (imp.size() > 0) {
            
            // fixme: consolidate constants so that they are size independent
            // triangle layout, first card in trick drawn first
          
            int xc2 = w/2; // panel center
            int yc2 = h/2;
            int cw2 = cardImagesTrickBig.getWidth(); // card width
            int ch2 = cardImagesTrickBig.getHeight(); // card height            
            double[] xd = new double[] { -1.35/3, +1.35/3, 0  };
            double[] yd = new double[] { -0.61/3, 0,  +0.61/3 };

            // find trick leader

            int k = sg.getMoveHist().size();
            
            for (int l = 0; l < imp.size(); l++) {
              // skip non-card moves
              while (Card.fromString(sg.getMoveHist().get(--k).action) == null) { }
            }

            int trickLeader = sg.getMoveHist().get(k).source;
            int topLeftIndex = 1; // observer
            if (myIndex >= 0) // player
              topLeftIndex = (table.playerInGameIndex(playerName) + 1) % 3;
            int startIndex = (trickLeader - topLeftIndex + 3) % 3;

            for (int i=0; i < imp.size(); i++) {
              imp.get(i).x = xc2 - cw2/2 + (int)(cw2*xd[(startIndex+i) % 3]);
              imp.get(i).y = yc2 - ch2/2 + (int)(ch2*yd[(startIndex+i) % 3]);
            }

            // previous trick to the left

            ArrayList<Card> prevTrickCards = new ArrayList<Card>();
            ArrayList<Card> currTrickCards = new ArrayList<Card>();
            
            ArrayList<Move> moves = sg.getMoveHist();
            int i;
            for (i=moves.size()-1; i >= 0; i--) {
              if (moves.get(i).action.length() == 2) {
                Card card = Card.fromString(moves.get(i).action);
                if (card != null) {
                  prevTrickCards.add(0, card);
                  st.getCurrentTrick(currTrickCards);
                  if (prevTrickCards.size() >= currTrickCards.size() + 3) {
                    trickLeader = moves.get(i).source;
                    break;
                  }
                }
              }
            }
            
            if (i >= 0) {
              
              // previous trick found

              ArrayList<Card> currTrick = new ArrayList<Card>();
              st.getCurrentTrick(currTrick);

              for (int j=0; j < currTrick.size(); j++) {
                prevTrickCards.remove(prevTrickCards.size()-1);
              }
              startIndex = (trickLeader - topLeftIndex + 3) % 3;
          
              ArrayList<ImagePos> imp2 = Utils.cardsToImages(prevTrickCards, cardImagesTrickSmall);
              cw2 = cardImagesTrickSmall.getWidth();  // card width
              ch2 = cardImagesTrickSmall.getHeight(); // card height            
              
              for (int j=0; j < 3; j++) {
                imp2.get(j).x = cw2 - cw2/2 + (int)(cw2*xd[(startIndex+j) % 3]);
                imp2.get(j).y = yc2 - ch2/2 + (int)(ch2*yd[(startIndex+j) % 3]);
                imp.add(imp2.get(j));              
              }
            }
          }

        } else {
          // complete history

          if (cardPlayStart >= 0) {

            {
              // void info

              int type = table.getGame().getCurrentState().getGameDeclaration().type;

              if (mode == DisplayMode.HISTORY) {
                if (historyIndex == 0 || sg.getCardPlayBookmark() < 0 ||
                    sg.getStatePriorToTrick(historyIndex-1) == null) 
                  st = sg.getState(0);
                else {
                  st = sg.getStatePriorToTrick(historyIndex-1);
                }
              } else
                st = sg.getCurrentState();

              {
                // hand panel 0
                int gi = (Math.max(myIndex,0)+0) % 3;
                int voids = st.getVoids(gi);
                clubsLabel0.setText   (((voids & (1 << Card.SUIT_CLUBS))    != 0) ? "0" : "?");
                spadesLabel0.setText  (((voids & (1 << Card.SUIT_SPADES))   != 0) ? "0" : "?");
                heartsLabel0.setText  (((voids & (1 << Card.SUIT_HEARTS))   != 0) ? "0" : "?");
                diamondsLabel0.setText(((voids & (1 << Card.SUIT_DIAMONDS)) != 0) ? "0" : "?");
                if (type == GameDeclaration.GRAND_GAME) {
                  jacksLabel0.setText(((voids & SimpleState.JACK_VOID_BIT) != 0) ?
                                      " " + jackStr + ": 0" :
                                      " " + jackStr + ": ?");
                } else
                  jacksLabel0.setText("");
              }

              {
                // hand panel 1
                int gi = (Math.max(myIndex,0)+1) % 3;
                int voids = st.getVoids(gi);
                clubsLabel1.setText   (((voids & (1 << Card.SUIT_CLUBS))    != 0) ? "0" : "?");
                spadesLabel1.setText  (((voids & (1 << Card.SUIT_SPADES))   != 0) ? "0" : "?");
                heartsLabel1.setText  (((voids & (1 << Card.SUIT_HEARTS))   != 0) ? "0" : "?");
                diamondsLabel1.setText(((voids & (1 << Card.SUIT_DIAMONDS)) != 0) ? "0" : "?");
                if (type == GameDeclaration.GRAND_GAME) {
                  jacksLabel1.setText(((voids & SimpleState.JACK_VOID_BIT) != 0) ?
                                      " " + jackStr + ": 0" :
                                      " " + jackStr + ": ?");
                } else
                  jacksLabel1.setText("");
              }

              {
                // hand panel 2
                int gi = (Math.max(myIndex,0)+2) % 3;
                int voids = st.getVoids(gi);
                clubsLabel2.setText   (((voids & (1 << Card.SUIT_CLUBS))    != 0) ? "0" : "?");
                spadesLabel2.setText  (((voids & (1 << Card.SUIT_SPADES))   != 0) ? "0" : "?");
                heartsLabel2.setText  (((voids & (1 << Card.SUIT_HEARTS))   != 0) ? "0" : "?");
                diamondsLabel2.setText(((voids & (1 << Card.SUIT_DIAMONDS)) != 0) ? "0" : "?");
                if (type == GameDeclaration.GRAND_GAME) {
                  jacksLabel2.setText(((voids & SimpleState.JACK_VOID_BIT) != 0) ?
                                      " " + jackStr + ": 0" :
                                      " " + jackStr + ": ?");
                } else
                  jacksLabel2.setText("");
              }
            }

            st = sg.getState(cardPlayStart); // before first card is played
            decl = st.getDeclarer();
            
            if (st.getSkat0() != null && st.getSkat0().isKnown()) {
              skatPts = st.getSkat0().value() + st.getSkat1().value();
            }
            
            // discarded cards
            cards.add(st.getSkat0());
            cards.add(st.getSkat1());
            
            // all played cards
            ArrayList<Move> moves = sg.getMoveHist();
            toMove.add(-1); toMove.add(-1); toMove.add(-1); toMove.add(-1);
            ptsDecl.add(0); ptsDecl.add(0); ptsDecl.add(0); ptsDecl.add(0);
            ptsDefs.add(0); ptsDefs.add(0); ptsDefs.add(0); ptsDefs.add(0);
            
            int cn = 0;
            int hi = 1;
            for (int i=cardPlayStart; i < moves.size(); i++) {
              Card c = Card.fromString(moves.get(i).action);
              if (c != null) {
                
                if (cn % 3 == 0) histStateIndexes[hi++] = i;  // new trick
                cn++;
                
                // played card: append move and values
                cards.add(c);
                toMove.add(moves.get(i).source);
                st = sg.getState(i+1); // pts after trick
                int declPts = st.getTrickPoints(decl);
                int sum = st.getTrickPoints(0)+st.getTrickPoints(1)+st.getTrickPoints(2);
                ptsDecl.add(declPts+skatPts);
                ptsDefs.add(sum-declPts);
              }
            }
            histStateIndexes[0] = histStateIndexes[1]; // forehand when clicking on skat
          }

          if (sg.isFinished() && sg.getOwner() == SimpleState.WORLD_VIEW) {

            // determine last trick point values
            // values could be different after resign, left, timeout

            GameResult gr = new GameResult();
            sg.getCurrentState().gameResult(gr);
            int declPts = gr.declCardPoints;
            int defPts = 120 - declPts;

            int i = ptsDefs.size()-1;  // 3: 0, 4: 1, 5: 2, 6: 3, 7: 1, 8: 2 ... 
            for (int j = 0; j < (i-1) % 3 + 1; j++) {
              ptsDecl.set(i-j, declPts);
              ptsDefs.set(i-j, defPts);
            }
          }

          // assign card locations

          imp = Utils.cardsToImages(cards, cardImagesTrickSmall);
    
          imp.get(0).x = x0 - 3 - cw;
          imp.get(0).y = dy/2-3;
          imp.get(1).x = x0/2 -3 - cw;
          imp.get(1).y = dy/2-3;

          if (imp.size() >= 4) {
            imp.get(2).x = x0 - 3 - cw;
            imp.get(2).y = dy/2+h/3-3;
            imp.get(3).x = x0/2 - 3 - cw;
            imp.get(3).y = dy/2+h/3-3;
          }
    
          // fixme: consolidate constants so that they are size independent
          // triangle layout, first card in trick drawn first
          
          double[] xd = new double[] { -0.18,  +0.18, 0  };
          double[] yd = new double[] { -0.8/3, 0,  +0.8/3 };

          // display tricks and set label texts

          int tcpX0 = trickCardPanel.getX0();
          int tcpY0 = trickCardPanel.getY0(); // top left of trick card panel (for labels)
          
          for (int i=4; i < cards.size(); i++) {

            int trickNum = (i-4) / 3;
            int withinTrick = (i-4) % 3;
            int xc = x0 + trickNum*dx + dx/2; // triangle center
            int cardIndex = (toMove.get(i) - Math.max(myIndex,0) + 3 + 2) % 3;
        
            imp.get(i).x = xc - cw/2 + (int)(cw*xd[cardIndex]);
            imp.get(i).y = (int)(0.57*ch/2 + (int)(ch*yd[cardIndex]));
            
            if (i % 3 == 1) {

              trickCardPanel.remove(labels[trickNum]);
              trickCardPanel.add(labels[trickNum],
                                 new org.netbeans.lib.awtextra.
                                 AbsoluteConstraints((int)(tcpX0 + xc - cw/2 + cw*xd[0]), // x(left card)
                                                     (int)(tcpY0 + 1.5*ch + 3)));
              String s = "";

              if (historyIndex >= 1 && trickNum == historyIndex - 1)
                s = "<html><u>";

              s += "" + (trickNum+1)+ posNamesShort[toMove.get(i)] + "|" +
                (i+2 < cards.size() ?
                 (ptsDecl.get(i+2) + "|" + ptsDefs.get(i+2)) :
                 (ptsDecl.get(i)   + "|" + ptsDefs.get(i)));

              if (historyIndex >= 1 && trickNum == historyIndex - 1)
                s += "</u></html>";

              labels[trickNum].setText(s);
              labels[trickNum].setVisible(true);
            }
          }

          // remaining cards: full deck - played cards (- known player cards)

          int rem = -1; // full deck
          
          for (int i=0; i < 3; i++) { // player index in game

            int hand, playedCards;
	    
            if (mode == DisplayMode.HISTORY) {

              // history
              if (historyIndex == 0 || sg.getCardPlayBookmark() < 0 ||
                  sg.getHandPriorToTrick(i, historyIndex-1) == 0) { 
                hand = sg.getState(1).getHand(i); // after deal
                playedCards = 0; // nothing
              } else {
                hand = sg.getHandPriorToTrick(i, historyIndex-1);
                playedCards = sg.getPlayedCardsPriorToTrick(i, historyIndex-1);
              }
	      
            } else {
	      
              hand = sg.getCurrentState().getHand(i);
              playedCards = sg.getCurrentState().getPlayedCards(i);
            }

            if (panelIndex >= 0 && panelIndex == (3-Math.max(myIndex,0)+i) % 3) {
              rem = Hand.clear(rem, hand);
              if (i == decl && sg.getCardPlayBookmark() >= 0) {
                if (st.getSkat0() != null && st.getSkat0().isKnown()) {
                  rem = Hand.clear(rem, st.getSkat0());
                  rem = Hand.clear(rem, st.getSkat1());
                }
              }
            }

            rem = Hand.clear(rem, playedCards);
          }

          Card[] remCardsArray = new Card[40];
          int mn = Hand.toCardArray(rem, remCardsArray);
          ArrayList<Card> remCards = Card.cardArrayToList(mn, remCardsArray);
          remCardsIndex = imp.size();
          int gt = sg.getCurrentState().getGameDeclaration().type;
          if (gt == GameDeclaration.NO_GAME)
            gt = GameDeclaration.GRAND_GAME;
	  
          Utils.sortCards(remCards, gt, redBlack);
          ArrayList<ImagePos> imp2 = Utils.cardsToImages(remCards, cardImagesTrickBig);
          int cw2 = cardImagesTrickBig.getWidth(); // card width
          int ch2 = cardImagesTrickBig.getHeight(); // (int)(cw2 * 3) / 2;              // card height            
          double xi = 0;
          int n = remCards.size();
          for (int j=0; j < n; j++) {
            imp2.get(j).x = 0 + (int)(xi * 2 * cw2/8 * 0.9); // was 1.0
            imp2.get(j).y = h - ch2/5 - 5;
            imp.add(imp2.get(j));
            if (j+1 < n) {
              if (gt == GameDeclaration.NULL_GAME) {
                // null
                if (remCards.get(j).getSuit() != remCards.get(j+1).getSuit())
                  xi += 0.75; // space between suits
              } else {
                // grand / suit
                if (SimpleState.trump(remCards.get(j), gt)) {
                  if (!SimpleState.trump(remCards.get(j+1), gt))
                    xi += 0.75; // space after trump
                } else {
                  if (remCards.get(j).getSuit() != remCards.get(j+1).getSuit())
                    xi += 0.75; // space between suits
                }
              }
            }
            xi += 1;
          }
        }
        // no longer in "noob" mode now
        
      } else {

        // no card play yet

        if (st.getPhase() != SimpleState.DISCARD_AND_DECL) {
        
          // passed game: show original skat in center
          
          imp = Utils.cardsToImages(cards, cardImagesTrickBig);
          
          cw = cardImagesTrickBig.getWidth(); // card width
          ch = cardImagesTrickBig.getHeight(); // card height
          
          imp.get(0).x = w/2 - cw;
          imp.get(0).y = imp.get(1).y = h/2 - ch/2;
          imp.get(1).x = w/2 + 2;
        }
      }

      trickCardPanel.setImages(imp);
      trickCardPanel.repaint();

      // hand panels

      for (int i=0; i < 3; i++) {

        int pi = (Math.max(myIndex,0) + i + 3) % 3;
        int hand, playedCards;
          
        if (mode == DisplayMode.HISTORY) {

          // history
          if (historyIndex == 0 || sg.getCardPlayBookmark() < 0 ||
              sg.getHandPriorToTrick(pi, historyIndex-1) == 0) { 
            hand = sg.getState(1).getHand(pi); // after deal
            playedCards = 0; // nothing
          } else {
            hand = sg.getHandPriorToTrick(pi, historyIndex-1);
            playedCards = sg.getPlayedCardsPriorToTrick(pi, historyIndex-1);
          }

        } else {

          hand = sg.getCurrentState().getHand(pi);
          playedCards = sg.getCurrentState().getPlayedCards(pi);
        }
      
        w = handPanels[i].getUsableWidth();
        h = handPanels[i].getUsableHeight() - yOffsets[i];
        ArrayList<Card> handCards;

        // collect own + played cards
      
        if (st.handKnown(pi)) {
          handCards = Hand.toCardList(hand);
        } else {
          handCards = new ArrayList<Card>();
          for (int j=0; j < st.numCards(pi); j++) {
            handCards.add(Card.newCard(-1, -1)); // unknown cards
          }
        }

        ArrayList<Card> played = Hand.toCardList(playedCards);

        if (noob) {
          for (Card c : played) {
            handCards.add(c);
          }
        }

        if (pi == st.getDeclarer() &&
	    (sg.getOwner() == SimpleState.WORLD_VIEW || mode == DisplayMode.PLAYING) &&
            st.getPhase() == SimpleState.DISCARD_AND_DECL) {

          // add cards to be discarded
          handCards.add(st.getSkat0());
          handCards.add(st.getSkat1());

          if (action == ClientWindow.TablePanelUpdateAction.MOVE) { 
            origSkatCards[0] = st.getSkat0();
            origSkatCards[1] = st.getSkat1();

            // don't raise skat          
            discardCards[0] = null;
            discardCards[1] = null;
          } 
        }
        
        Utils.sortCards(handCards, gameType, redBlack);	
        imp = Utils.cardsToImages(handCards, cardImages[i]);
        Utils.arrangeImagesStraight(imp, w, yOffsets[i], handCards.size() >= 10 ? handCards.size() : 10,
                                    mode == DisplayMode.PLAYING);

        // lower all cards + shift right in noob mode
        for (ImagePos ip : imp) {
          ip.y += dy;
        }
      
        handPanels[i].setImages(imp);

        int n = handCards.size();

        handPanels[i].m1x = handPanels[i].m2x = -1; // no marker
        
        for (int k=0; k < n; k++) {

          for (Card c : played) {
            if (handCards.get(k).equals(c)) {
              // lower played cards further when played
              imp.get(k).y = yOffsets[i] + 7*h/10;
              break;
            }
          }

          if (mode == DisplayMode.PLAYING &&
              st.getPhase() == SimpleState.DISCARD_AND_DECL &&
              Math.max(myIndex,0) == st.getDeclarer()) {

            if (handCards.get(k).equals(discardCards[0]) ||
                handCards.get(k).equals(discardCards[1])) {

              // raise cards to be discarded
              imp.get(k).y -= dy;

            }

            if ((handCards.get(k).equals(origSkatCards[0]) ||
                 handCards.get(k).equals(origSkatCards[1]))) {

              int mdx = (imp.get(1).x - imp.get(0).x)/2;

              // mark original skat cards
              if (handPanels[i].m1x < 0) {

                handPanels[i].m1x = imp.get(k).x + mdx; handPanels[i].m1y = imp.get(k).y - mdx/8;

              } else if (handPanels[i].m2x < 0) {

                handPanels[i].m2x = imp.get(k).x + mdx; handPanels[i].m2y = imp.get(k).y - mdx/8;
              }

              handPanels[i].mw = mdx/4;
              handPanels[i].mh = mdx/4;
              
              // lower original skat cards if not selected
              // imp.get(k).y -= dy;
            }
          }
        }

        if (mode == DisplayMode.HISTORY && historyIndex > 0) {

          // raise card played in this trick
          // show hands prior to trick

          Card[] trick = new Card[3];
        
          // collect trick cards
          for (int k=0; k < 3; k++) {
            int j = (historyIndex-1)*3 + k + 4;
            if (j >= cards.size()) break;
            trick[k] = cards.get(j);
          }

          // raise them
          int n2 = handCards.size();
          for (int k=0; k < n2; k++) {
            for (int j=0; j < 3; j++) {
              if (trick[j] == null) break;
              if (handCards.get(k).equals(trick[j])) {
                imp.get(k).y -= dy;
                break;
              }
            }
          }
        }
      }

      // set panel/button titles and backgrounds

      String trickTitle;
    
      if (mode == DisplayMode.HISTORY) {

        setHandTitles(sg.getState(histStateIndexes[historyIndex]), myIndex);

      } else { // playing/observing

        setHandTitles(sg.getCurrentState(), myIndex);
      }

      int phase = sg.getCurrentState().getPhase();
      
      {
        // trick panel

        String title = rbs("History");

        if (!noob && !sg.isFinished()) {
          if (sg.getCurrentState().getPhase() == SimpleState.CARDPLAY)
            title = rbs("Previous_and_current_trick");
          else
            title = "Skat";
        }
        
        TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleColor(!sg.isFinished() && phase == SimpleState.SKAT_OR_HAND_DECL 
                         ? toMoveColor : defaultForegroundColor);
        trickCardPanel.setBorder(tb);
      }

      {
        // declaration panel
        TitledBorder tb = BorderFactory.createTitledBorder(rbs("Contract"));
        tb.setTitleColor(!sg.isFinished() &&
                         (phase == SimpleState.SKAT_OR_HAND_DECL ||
                          phase == SimpleState.DISCARD_AND_DECL)
                         ? toMoveColor : defaultForegroundColor);
        declarationPanel.setBorder(tb);
      }

      {
        // bidding panel

        if (mode != DisplayMode.HISTORY) {

          String title = "   "; //rbs("Bidding");
          SimpleState s = sg.getCurrentState();
        
          if (phase == SimpleState.BID) {

            // BID
          
            if (s.getAsked() == SimpleState.WORLD_VIEW) { // MH,RH passed
            
              if (meToMove)
                title = rbs("Your_bid?");
              else
                title = String.format(rbs("someones_bid?"), posNames[s.getBidder()]);
            
            } else {

              if (meToMove) {
                title = String.format(rbs("bid_you_to_x?"), posNames[s.getAsked()]);
              } else if (s.getAsked() == myIndex) {
                title = String.format(rbs("bid_x_to_you?"), posNames[s.getBidder()]);
              } else {
                title = String.format(rbs("bid_x_to_y?"), posNames[s.getBidder()], posNames[s.getAsked()]);
              }
            }

          } else if (phase == SimpleState.ANSWER) {

            // ASKED

            if (meToMove) {
              title = String.format(rbs("asked_you_by_x"), posNames[s.getBidder()], s.getMaxBid());
            } else if (s.getBidder() == myIndex) {
              title = String.format(rbs("asked_x_by_you"), posNames[s.getAsked()], s.getMaxBid());
            } else {
              title = String.format(rbs("asked_x_by_y"), posNames[s.getAsked()], posNames[s.getBidder()], s.getMaxBid());
            }
          }

          boolean inAuction = phase == SimpleState.BID || phase == SimpleState.ANSWER;
          
          TitledBorder tb = BorderFactory.createTitledBorder(title);
          tb.setTitleColor(!sg.isFinished() && inAuction ? toMoveColor : defaultForegroundColor);

          // biddingPanel.setBorder(tb);
          biddingLabel.setText(title);
          biddingLabel.setForeground(!sg.isFinished() && inAuction ? toMoveColor : defaultForegroundColor);
          //biddingLabel.invalidate();
          //biddingPanel.validate();

          // bid buttons
        
          bidButton1.setEnabled(meToMove && inAuction);
          bidButton2.setEnabled(meToMove && inAuction);
          bidDecrementButton.setEnabled(false);
          bidIncrementButton.setEnabled(false);
          
          if (meToMove) {
          
            if (phase == SimpleState.BID) {
             
              bidButton1.setText(""+s.nextBid(numIncrements));
              bidDecrementButton.setEnabled(true);
              bidIncrementButton.setEnabled(true);
            
            } else if (phase == SimpleState.ANSWER) {
            
              bidButton1.setText(rbs("Yes"));
            
            }
          }
        }
      }
    }

    invalidate(); // !!! !!!
    validate();
    repaint();
  }

  private JPanel getGameTab() {

    trickCardPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

    declarationPanel.setLayout(new MigLayout("ins 0, nogrid, center"));
    // declarationPanel.setLayout(new MigLayout("ins 0, nogrid, center", "[shrink]", "[shrink][shrink][shrink]"));
    declarationPanel.add(grandButton, "wrap");
    declarationPanel.add(clubsButton, "split"); declarationPanel.add(clubsLabel, "gap -10");
    declarationPanel.add(spadesButton, "gap 10"); declarationPanel.add(spadesLabel, "gap -10, wrap");
    declarationPanel.add(heartsButton, "split"); declarationPanel.add(heartsLabel, "gap -10");
    declarationPanel.add(diamondsButton, "gap 10"); declarationPanel.add(diamondsLabel, "gap -10, wrap");
    declarationPanel.add(nullButton, "wrap");
    declarationPanel.add(ouvertCheckBox, "wrap -5");
    declarationPanel.add(handCheckBox, "wrap -5");
    declarationPanel.add(schneiderCheckBox, "wrap -5");
    declarationPanel.add(schwarzCheckBox, "wrap");
    declarationPanel.add(declareButton, "center");

    biddingPanel2.setLayout(new MigLayout("ins 0, flowy, align left, gap 2!", "[shrink]", "[shrink][shrink][shrink]"));
    biddingPanel2.add(bidIncrementButton, "width 15!, height 15!");
    biddingPanel2.add(bidDecrementButton, "width 15!, height 15!");
    
    biddingLabel = new JLabel(" ");
    biddingPanel.setLayout(new MigLayout("ins 0, nogrid, center", "[shrink]", "[shrink][shrink][shrink]"));
    biddingPanel.add(biddingLabel, "wrap");
    biddingPanel.add(biddingPanel2, "split 3, gapright 5!");
    biddingPanel.add(bidButton1, "flowx");
    biddingPanel.add(bidButton2);    

    JPanel topPanel = new JPanel(new MigLayout("ins 0, fill, gap 0!", "[grow][grow][grow]", "[grow][grow][grow]"));
    // JPanel topPanel = new JPanel(new MigLayout("debug, ins 0, fill", "[83%][17%]", "[grow]"));
    JPanel topLeftPanel = new JPanel(new MigLayout("ins 0, fill", "[grow][grow][grow]", "[grow][grow][grow]"));

    topLeftPanel.add(cardPanel1,      "w 100%, h 28%, growx");
    topLeftPanel.add(cardPanel2,      "w 100%, h 28%, growx, wrap");
    topLeftPanel.add(trickCardPanel,  "w 100%, h 42%, span 2, growx, wrap");
    topLeftPanel.add(cardPanel0,      "w 100%, h 38%, span 2, growx");

    JPanel topRightPanel = new JPanel(new MigLayout("nogrid, ins 0, fillx", "[center][shrink]", "[shrink][shrink][shrink]"));

    JPanel buttonPanel = new JPanel(new MigLayout("nogrid, ins 0, center"));
    buttonPanel.add(showButton,         "sgx 1, shrinkx, center, wrap");
    buttonPanel.add(resignButton,       "sgx 1, center");

    topRightPanel.add(buttonPanel,      "center, wrap 8");
    topRightPanel.add(declarationPanel, "sgx 1, shrinkx, center, wrap 8");
    topRightPanel.add(biddingPanel,     "sgx 1, shrinkx, center, wrap 8");
    topRightPanel.add(playingLabel,     "sgx 1, wrap 5");
    topRightPanel.add(gameNumberLabel,  "sgx 1, wrap 100");

    topPanel.add(topLeftPanel,         "growx, growy");
    topPanel.add(topRightPanel,        "growy, dock east");
    
    return topPanel;
  }

  private JPanel getTalkTab() {

    JPanel talkPanel = new JPanel(new MigLayout("ins 0, fill", "[grow]", "[grow][grow][grow]"));

    JPanel bottomRightPanel = new JPanel(new MigLayout("ins 0, fill", "[shrink]", "[shrink][shrink][shrink]"));

    resultScrollPane.setViewportView(resultTable);
    bottomRightPanel.add(resultScrollPane,   "center, growy, wrap");

    JPanel readyInvitePanel = new JPanel(new MigLayout("ins 0", "[shrink]", "[shrink][shrink][shrink]"));
    // may need to "fill" here

    readyInvitePanel.add(readyButton,      "sgx 1, center, split");
    readyInvitePanel.add(inviteButton,     "sgx 1");
    bottomRightPanel.add(readyInvitePanel, "center, wrap");

    JPanel optionsPanel = new JPanel(new MigLayout("ins 0, fill", "[grow]", "[grow][grow][grow]"));
    optionsBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options",
                                                     TitledBorder.CENTER, TitledBorder.TOP);
    optionsPanel.setBorder(optionsBorder); // ***requires i18n

    JPanel leaveHelpPanel = new JPanel(new MigLayout("ins 0, fill", "[shrink]", "[shrink][shrink][shrink]"));
    leaveHelpPanel.add(leaveButton,     "sgx 2, center, wrap");
    leaveHelpPanel.add(helpButton,      "sgx 2, center");

    JPanel resizePanel = new JPanel(new MigLayout("ins 0, fill", "[shrink]", "[shrink][shrink][shrink]"));
    resizePanel.add(resizeLabel,        "center, wrap");
    resizePanel.add(plusButton,         "center, split");
    resizePanel.add(minusButton);

    optionsPanel.add(leaveHelpPanel,    "center, split");
    optionsPanel.add(resizePanel);
    bottomRightPanel.add(optionsPanel,  "center");

    JPanel bottomLeftPanel = new JPanel(new MigLayout("ins 0, fill", "[grow]", "[grow][grow][grow]"));
    textArea.setRows(17);
    textScrollPane.setViewportView(textArea);
    bottomLeftPanel.add(textScrollPane,  "growx, growy, span, wrap");
    bottomLeftPanel.add(inputLine,       "growx, span");

    JPanel bottomPanel = new JPanel(new MigLayout("ins 0, fill, righttoleft"));

    bottomPanel.add(bottomRightPanel, "sgy 1, align right, shrinkx, gapbefore 10!, gapbottom 10!, gapafter 5!");
    bottomPanel.add(bottomLeftPanel,  "sgy 1, align left, gapbottom 10!, gapbefore 0!, gapafter 10!, growx");

    JPanel tournamentResultPanel = new JPanel(new MigLayout("ins 0, fill", "[grow][grow][grow]", "[grow][grow][grow]"));

    tournamentResultScrollPane.setViewportView(tournamentResultTable);
    tournamentResultPanel.add(tournamentResultScrollPane,  "w 100%, span, wrap, gapbottom 2!");
    tournamentResultPanel.add(infoLabel,                   "center, w 100%, span");
    
    talkPanel.add(tournamentResultPanel, "dock north, w 100%, span");
    talkPanel.add(bottomPanel,           "dock south, span, growy");

    return talkPanel;  
  }
  
  /* Method is called from within the constructor to initialize variables and set
     layout. */

  private void initComponents()
  {
    // allocate components
    
    tableTabbedPane = new JTabbedPane();

    gameTypeButtonGroup = new ButtonGroup();
    GamePanel = new JPanel();
    cardPanel = new JPanel();
    cardPanel2 = new common.CardPanel();

    suitPanel2 = new JPanel();
    clubsLabel2 = new JLabel();
    heartsLabel2 = new JLabel();
    spadesLabel2 = new JLabel();
    diamondsLabel2 = new JLabel();
    jacksLabel2 = new JLabel();
    
    textField2A = new JTextField();
    textField2B = new JTextField();
    cardPanel1 = new common.CardPanel();

    suitPanel1 = new JPanel();
    heartsLabel1 = new JLabel();
    diamondsLabel1 = new JLabel();
    spadesLabel1 = new JLabel();
    jacksLabel1 = new JLabel();
    clubsLabel1 = new JLabel();
    
    textField1A = new JTextField();
    textField1B = new JTextField();
    trickCardPanel = new common.CardPanel();
    cardPanel0 = new common.CardPanel();

    suitPanel0 = new JPanel();
    jacksLabel0 = new JLabel();
    diamondsLabel0 = new JLabel();
    heartsLabel0 = new JLabel();
    spadesLabel0 = new JLabel();
    clubsLabel0 = new JLabel();
    
    textField0A = new JTextField();
    textField0B = new JTextField();
    jPanel4 = new JPanel();
    resignButton = new JButton();
    biddingPanel = new JPanel();
    // Second bidding panel will be used to hold increment/decrement buttons. - Ryan
    biddingPanel2 = new JPanel();
    bidButton1 = new JButton();
    bidButton2 = new JButton();
    bidIncrementButton = new JButton("+");
    bidDecrementButton = new JButton("-");
    playingLabel = new JLabel();
    showButton = new JButton();
    gameNumberLabel = new JLabel();
    declarationPanel = new JPanel();
    nullButton = new JRadioButton();
    declareButton = new JButton();
    jPanel2 = new JPanel();
    clubsButton = new JRadioButton();
    heartsButton = new JRadioButton();
    spadesButton = new JRadioButton();
    diamondsButton = new JRadioButton();
    clubsLabel = new JLabel();
    heartsLabel = new JLabel();
    spadesLabel = new JLabel();
    diamondsLabel = new JLabel();
    grandButton = new JRadioButton();
    ouvertCheckBox = new JCheckBox();
    handCheckBox = new JCheckBox();
    schneiderCheckBox = new JCheckBox();
    schwarzCheckBox = new JCheckBox();
    leaveButton = new JButton();
    helpButton = new JButton();
    resultPanel = new JPanel();
    textScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    resultScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    tournamentResultScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    resultTable = new JTable();
    inputLine = new common.JHistTextField();
    inviteButton = new JButton();
    plusButton = new JButton();
    minusButton = new JButton();    
    readyButton = new JButton();
    textArea = new common.JLimitedTextArea();

    resizeLabel = new JLabel("Resize Window"); // ***requires i18n
    infoLabel = new JLabel("H = Hand; O = Ouvert; Ann. = Announced; Un. = Unannounced"); // ***i18n

    for (int i=0; i < 10; i++) {
      labels[i] = new JLabel();
    }
    
    textField0A.setEditable(false);
    textField0B.setEditable(false);
    textField1A.setEditable(false);
    textField1B.setEditable(false);
    textField2A.setEditable(false);
    textField2B.setEditable(false);

    cardPanel0.name = "c0";
    cardPanel1.name = "c1";
    cardPanel2.name = "c2";
    trickCardPanel.name = "ct";
    
    cardPanel0.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
    ((TitledBorder)cardPanel0.getBorder()).setTitle("");
    ((TitledBorder)cardPanel1.getBorder()).setTitle("");    
    ((TitledBorder)cardPanel2.getBorder()).setTitle("");

    suitPanel0.setLayout(new MigLayout("ins 0"));
    suitPanel0.add(clubsLabel0);
    suitPanel0.add(spadesLabel0);
    suitPanel0.add(heartsLabel0);
    suitPanel0.add(diamondsLabel0);
    suitPanel0.add(jacksLabel0);

    suitPanel1.setLayout(new MigLayout("ins 0", "[][]", "[]0px[]"));
    suitPanel1.add(clubsLabel1);
    suitPanel1.add(spadesLabel1);
    suitPanel1.add(jacksLabel1, "wrap");
    suitPanel1.add(heartsLabel1);
    suitPanel1.add(diamondsLabel1);
    
    suitPanel2.setLayout(new MigLayout("ins 0", "[][]", "[]0px[]"));
    suitPanel2.add(clubsLabel2);
    suitPanel2.add(spadesLabel2);
    suitPanel2.add(jacksLabel2, "wrap");
    suitPanel2.add(heartsLabel2);
    suitPanel2.add(diamondsLabel2);

    resignButton.setText(rbs("Game_shortcut")); // NOI18N
    resignButton.setToolTipText(rbs("Remaining_tricks_to_opponent(s)_or_resign_null_game")); // NOI18N
    resignButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
    
    biddingPanel.setBorder(BorderFactory.createTitledBorder(rbs("Bidding")));

    bidButton1.setText(rbs("Yes")); // NOI18N
    bidButton1.setEnabled(false);

    bidButton2.setText(rbs("Pass")); // NOI18N
    bidButton2.setEnabled(false);

    bidIncrementButton.setEnabled(false);
    bidDecrementButton.setEnabled(false);

    playingLabel.setHorizontalAlignment(SwingConstants.CENTER);
    playingLabel.setText(rbs("Playing")); // NOI18N
    playingLabel.setHorizontalTextPosition(SwingConstants.CENTER);
    playingLabel.setRequestFocusEnabled(false);

    showButton.setText(rbs("Show_cards")); // NOI18N
    showButton.setToolTipText(rbs("As_declarer_you_can_reveal_your_hand_")); // NOI18N
    showButton.setMargin(new java.awt.Insets(2, 5, 2, 5));

    gameNumberLabel.setHorizontalAlignment(SwingConstants.CENTER);
    gameNumberLabel.setText("Game 1");

    /* Reduces the insets between the declaration panel and its border, so
       that the declaration panel can be made thinner. - Ryan */
    Border declarationBorder = BorderFactory.createTitledBorder(rbs("Contract"));
    Border margin = new EmptyBorder(0, 0, 0, 0);
    declarationPanel.setBorder(new CompoundBorder(declarationBorder, margin));

    gameTypeButtonGroup.add(nullButton);
    nullButton.setText("Null");

    declareButton.setText(rbs("Declare")); // NOI18N

    gameTypeButtonGroup.add(clubsButton);
    gameTypeButtonGroup.add(spadesButton);
    gameTypeButtonGroup.add(heartsButton);
    gameTypeButtonGroup.add(diamondsButton);

    grandButton.setText("Grand");    
    gameTypeButtonGroup.add(grandButton);

    ouvertCheckBox.setText("Ouvert");
    handCheckBox.setText("Hand");
    schneiderCheckBox.setText("Schneider");
    schwarzCheckBox.setText("Schwarz");

    leaveButton.setText(rbs("Leave"));
    helpButton.setText(rbs("Help"));

    plusButton.setText("+"); // NOI18N
    plusButton.setToolTipText(rbs("Tooltip-Plus")); // NOI18N
    plusButton.setMargin(new java.awt.Insets(2, 5, 2, 5));

    minusButton.setText("-"); // NOI18N
    minusButton.setToolTipText(rbs("Tooltip-Minus")); // NOI18N
    minusButton.setMargin(new java.awt.Insets(2, 5, 2, 5));

    /* Made margins of "Invite" and "Ready" buttons larger to fill up space in
       the Miscellaneous tab. - Ryan
    */

    inviteButton.setText(rbs("Button-Invite")); // NOI18N
    inviteButton.setToolTipText(rbs("Tooltip-Invite")); // NOI18N
    inviteButton.setMargin(new java.awt.Insets(5, 20, 5, 20));

    /* Insets of "Ready" button slightly smaller so that the full word can always
       be displayed on the button when the window is resized. */
    readyButton.setText(rbs("Button-Ready")); // NOI18N
    readyButton.setToolTipText(rbs("Tooltip-Ready")); // NOI18N
    readyButton.setMargin(new java.awt.Insets(5, 15, 5, 15));

    textArea.setBorder(javax.swing.BorderFactory.createTitledBorder(rbs("Messages")));

    textArea.setEditable(false);
    textArea.setLineWrap(true);

    // wire components

    setLayout(new MigLayout("ins 0, fill", "[grow][fill]", "[grow][fill]"));
    add(tableTabbedPane, "growx, growy");
    tableTabbedPane.setLayout(new MigLayout("ins 0, flowy, fill", "[grow][fill]", "[grow][fill]"));

    tableTabbedPane.addTab("Miscellaneous", getTalkTab());
    tableTabbedPane.addTab("Game 1", getGameTab());

    /* So that the user can type Alt+(left arrow) to switch to the game panel and
       Alt+(right arrow) to switch to the auxiliary (talk/results) panel. - Ryan */
    // (For some reason, this function has stopped working?)
    tableTabbedPane.setMnemonicAt(0, KeyEvent.VK_LEFT);
    tableTabbedPane.setMnemonicAt(1, KeyEvent.VK_RIGHT);    
    
    invalidate();
    validate();
  }

  JTabbedPane tableTabbedPane;

  JButton bidButton1;
  JButton bidButton2;
  JButton bidIncrementButton;
  JButton bidDecrementButton;
  JButton declareButton;
  JButton helpButton;
  JButton plusButton, minusButton;
  JButton inviteButton;
  JButton leaveButton;
  JButton readyButton;
  JButton resignButton;
  JButton showButton;
  
  JRadioButton diamondsButton;
  JRadioButton heartsButton;
  JRadioButton spadesButton;
  JRadioButton clubsButton;
  JRadioButton grandButton;
  JRadioButton nullButton;
  ButtonGroup gameTypeButtonGroup;

  JLabel diamondsLabel, heartsLabel, spadesLabel, clubsLabel;
  JLabel diamondsLabel0, diamondsLabel1, diamondsLabel2;  
  JLabel heartsLabel0, heartsLabel1, heartsLabel2;
  JLabel spadesLabel0, spadesLabel1, spadesLabel2;  
  JLabel clubsLabel0, clubsLabel1, clubsLabel2;
  JLabel jacksLabel0, jacksLabel1, jacksLabel2;
  JLabel gameNumberLabel;
  JLabel playingLabel;
  JLabel biddingLabel;
  JLabel resizeLabel; // just a label for the window-resizing buttons - Ryan
  JLabel infoLabel; // provides small legend for tourney result table - Ryan
  
  JCheckBox ouvertCheckBox, handCheckBox, schneiderCheckBox, schwarzCheckBox;
  JTable resultTable;

  JTextField textField0A, textField0B;
  JTextField textField1A, textField1B;
  JTextField textField2A, textField2B;
  common.JLimitedTextArea textArea;
  common.JHistTextField inputLine;

  JScrollPane textScrollPane, resultScrollPane, tournamentResultScrollPane;

  JPanel GamePanel;
  JPanel biddingPanel;
  JPanel biddingPanel2;
  JPanel cardPanel;
  JPanel declarationPanel;
  JPanel resultPanel;
  JPanel jPanel2;
  JPanel jPanel4;
  JPanel suitPanel0;
  JPanel suitPanel1;
  JPanel suitPanel2;
  common.CardPanel cardPanel0;
  common.CardPanel cardPanel1;
  common.CardPanel cardPanel2;
  common.CardPanel trickCardPanel;
  Border optionsBorder;
  TournamentResultTable tournamentResultTable;

  TournamentTablePanel getThis() { return this; }

  public Table getTable() { return table; }
  
  public void resize(float f)
  {
    Misc.msg("TABLE PANEL RESIZE " + f);
    
    scale = f;

    // pick fonts

    Font f1 = new Font("Dialog", 1, Math.round(10*f));
    Font f2 = new Font("Dialog", 1, Math.round(12*f));
    Font f2m = new Font("Monospaced", 1, Math.round(12*f));   
    Font f3 = new Font("Dialog", 1, Math.round(14*f));
    Font f4 = new Font("Dialog", 1, Math.round(18*f));
    // Font for bidding increment/decrement buttons. - Ryan
    // Font f5 = new Font("Monospaced", 1, Math.round(8*f));
    
    // create labels for trickCardPanel
      
    for (int i=0; i < 10; i++) {
      labels[i].setFont(f1);
    }

    clubsLabel0.setFont(f1);
    spadesLabel0.setFont(f1);
    heartsLabel0.setFont(f1);
    diamondsLabel0.setFont(f1);
    jacksLabel0.setFont(f1);

    clubsLabel1.setFont(f1);
    spadesLabel1.setFont(f1);
    heartsLabel1.setFont(f1);
    diamondsLabel1.setFont(f1);
    jacksLabel1.setFont(f1);

    clubsLabel2.setFont(f1);
    spadesLabel2.setFont(f1);
    heartsLabel2.setFont(f1);
    diamondsLabel2.setFont(f1);
    jacksLabel2.setFont(f1);

    textField0A.setFont(f2);
    textField0B.setFont(f2);
    textField1A.setFont(f2);
    textField1B.setFont(f2);
    textField2A.setFont(f2);
    textField2B.setFont(f2);

    playingLabel.setFont(f4);
    gameNumberLabel.setFont(f3);

    ouvertCheckBox.setFont(f3);
    handCheckBox.setFont(f3);
    schneiderCheckBox.setFont(f3);
    schwarzCheckBox.setFont(f3);

    inputLine.setFont(f2);
    resultTable.setFont(f2);
    textArea.setFont(f2m);

    bidIncrementButton.setFont(f1);
    bidDecrementButton.setFont(f1);
    bidButton1.setFont(f2);
    bidButton2.setFont(f2);
    resizeLabel.setFont(f3);
    infoLabel.setFont(f1);

    // set icons

    // Add icons to declaration buttons

    int iw = Math.round(24*f);

    clubsLabel.setIcon(getSuitIcon("clubs", iw));
    spadesLabel.setIcon(getSuitIcon("spades", iw));    
    heartsLabel.setIcon(getSuitIcon("hearts", iw));
    diamondsLabel.setIcon(getSuitIcon("diamonds", iw));    

    // adjust card panels

    iw = Math.round(14*f);

    // hand 0
    clubsLabel0.setIcon(getSuitIcon("clubs", iw));
    spadesLabel0.setIcon(getSuitIcon("spades", iw));    
    heartsLabel0.setIcon(getSuitIcon("hearts", iw));
    diamondsLabel0.setIcon(getSuitIcon("diamonds", iw));

    // hand 1
    clubsLabel1.setIcon(getSuitIcon("clubs", iw));
    spadesLabel1.setIcon(getSuitIcon("spades", iw));    
    heartsLabel1.setIcon(getSuitIcon("hearts", iw));
    diamondsLabel1.setIcon(getSuitIcon("diamonds", iw));

    // hand 2
    clubsLabel2.setIcon(getSuitIcon("clubs", iw));
    spadesLabel2.setIcon(getSuitIcon("spades", iw));    
    heartsLabel2.setIcon(getSuitIcon("hearts", iw));
    diamondsLabel2.setIcon(getSuitIcon("diamonds", iw));

    int WIDTH=600;
    int w = Math.round(WIDTH*f);
    int th = Math.round(((textField2A.getFont().getSize() + 1)* 1.2f));
    int tw = Math.round(w*0.5f*0.73f);

    yOffsets[0] = th;
    yOffsets[1] = yOffsets[2] = 2*th;
    
    Dimension tdim = new Dimension(tw, th);
    
    textField0A.setPreferredSize(tdim); textField0A.setMaximumSize(tdim); 
    textField0B.setPreferredSize(tdim); textField0B.setMaximumSize(tdim);
    textField1A.setPreferredSize(tdim); textField1A.setMaximumSize(tdim);
    textField1B.setPreferredSize(tdim); textField1B.setMaximumSize(tdim);   
    textField2A.setPreferredSize(tdim); textField2A.setMaximumSize(tdim);
    textField2B.setPreferredSize(tdim); textField2B.setMaximumSize(tdim);

    cardPanel0.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
    cardPanel0.add(textField0A, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2));
    cardPanel0.add(textField0B, new org.netbeans.lib.awtextra.AbsoluteConstraints(tw+1+2, 2));
    cardPanel0.add(suitPanel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(2*(tw+1)+2, 2, -1, th));

    cardPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
    cardPanel1.add(textField1A, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2));
    cardPanel1.add(textField1B, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, th+2));
    cardPanel1.add(suitPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(tw+1+2, 2, -1, 2*th));

    cardPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
    cardPanel2.add(textField2A, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2));
    cardPanel2.add(textField2B, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, th+2));
    cardPanel2.add(suitPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(tw+1+2, 2, -1, 2*th));

    // adjust table sizes

    revalidate();
    
    Font tableFont = resultTable.getFont();
    FontMetrics fm = this.getFontMetrics(tableFont);
    String[] resultCont = new String[] {
      "MMMMxxxx", "EDS", "M", "9999", "999", "-9999", "99999", "XXXXX" };

    for (int i=0; i < resultTable.getColumnModel().getColumnCount(); i++) {
      int w2 = Math.max(fm.stringWidth("I"+resultCont[i]), fm.stringWidth("I"+resultTableHeaders[i]));
      resultTable.getColumnModel().getColumn(i).setPreferredWidth(w2);
    }

    int h = (int)Math.round((tableFont.getSize() + 1)* 1.1);
    resultTable.setRowHeight(h);
    resultTable.invalidate();
    resultTable.revalidate();
    Dimension d1 = resultTable.getPreferredSize();
    Dimension d2 = resultTable.getTableHeader().getPreferredSize();
    resultScrollPane.setPreferredSize(new Dimension(d1.width, d1.height + d2.height + 5));
    resultScrollPane.invalidate();
    resultScrollPane.revalidate();

    tournamentResultTable.resize(f, fm);

    revalidate();
    game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
  }

  public void setTableFrame(JFrame frame)
  {
    tableFrame = frame;
  }
  
}
