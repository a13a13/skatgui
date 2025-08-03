/*
 * view/edit game
 *
 * (c) Michael Buro, licensed under GPLv3
 */

package gui;

import common.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.net.*;

/*
  functionality:

  play mode:
  
  play game at table
   - input all cards and moves

  play game with connected programs/players
   - input moves
   - rotate players
   - keep stats

  ---------------------------
  
  view mode:
  
  view game
   - replay
   - game end -> edit mode?

  view/edit game
   - replay
   - edit current move (deletes rest of game)

 */

public class EditWindow
  extends javax.swing.JFrame
  implements EventHandler,WindowListener,ComponentListener
{
  public enum TablePanelUpdateAction {
    UNKNOWN, MOVE, START, END, INFO, GUI, ERROR, TELL, TIMER
      };
  
  final SimpleGame game; // game to view/edit

  boolean externalWorldMoves; // true: world moves from outside
  int gameView; // game play perspective (from game)
  
  String me;
  int gameType;

  boolean redBlack = false;
  BetterRNG rng;
  
  // view state
  boolean playMode;  // true:play, false:view
  // int historyIndex;  // at what move are we? (skat,discard,tricks)
  int remCardsIndex; // for differentiating clicks on tricks / remaining cards
  int panelIndex;    // mouse clicks on hand/trick panel change this value, <0: trick

  CardImages cardImagesTrickSmall, cardImagesTrickBig;
  CardImages[] cardImages = new CardImages[3];
  Color defaultBackgroundColor, defaultForegroundColor;
  CardPanel[] handPanels = new CardPanel[3];
  JLabel[] labels = new JLabel[10]; // trick annotations
  Color color1 = new Color(255,83,83);  // me to move
  Color color2 = new Color(81,113,255); // other to move

  int[] yoffset = new int[] { 19, 38, 38 }; // for cards in panels, fixme: adjustable

  JFormattedTextField textFieldsA[] = new JFormattedTextField[3];
  JFormattedTextField textFieldsB[] = new JFormattedTextField[3];  
  
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

  static Map<Integer, CardImages> cardMap = new TreeMap<Integer, CardImages>();
  
  static String FROM_0 = "0";
  static String FROM_1 = "1";
  static String FROM_2 = "2";
  static String FROM_TRICK = "trick";
  
  static String FROM_FH = "dealFH";
  static String FROM_MH = "dealMH";
  static String FROM_RH = "dealRH";
  static String FROM_SK = "dealSK";
  static String FROM_DISCARD = "discard";
  static String FROM_PLAY = "play";     

  static int DEAL_TAB     = 0;
  static int BID_TAB      = 1;
  static int PICKUP_TAB   = 2;
  static int SKAT_TAB     = 3;
  static int CONTRACT_TAB = 4;
  static int CARDPLAY_TAB = 5;
  static int FINISHED_TAB = 6;    
  
  Card[] discardCards = new Card[2];  // cards selected for discard
  java.util.ResourceBundle bundle;

  int moveIndex; // for stepping through game
    
  Color selectColor = new Color(210,210,210);
  
  String rbs(String s) { return bundle.getString(s); }
  
  /** Creates new form EditWindow */
  public EditWindow(SimpleGame g, boolean externalWorldMoves, int width, int height)
  {
    this.game = g;
    this.gameView = g.getOwner();

    long t = System.currentTimeMillis();
    rng = new BetterRNG(t, t);
    rng.setOverlay(0);
    
    bundle = java.util.ResourceBundle.getBundle(resourceFile);

    initComponents();

    setSize(width, height);
    
    getRootPane().setDoubleBuffered(true);

    actionTabbedPane.setEnabledAt(0, false);
    actionTabbedPane.setEnabledAt(1, false);
    actionTabbedPane.setEnabledAt(2, false);
    actionTabbedPane.setEnabledAt(3, false);
    actionTabbedPane.setEnabledAt(4, false);
    actionTabbedPane.setEnabledAt(5, false);
    actionTabbedPane.setEnabledAt(6, false);    
    
    textFieldsA[0] = textField0A;
    textFieldsB[0] = textField0B;   
    textFieldsA[1] = textField1A;
    textFieldsB[1] = textField1B;   
    textFieldsA[2] = textField2A;
    textFieldsB[2] = textField2B;

    game2MoveTable();

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(this);
    addComponentListener(this);

    setTitle("Edit Game");

    // hook up components

    dealSKCardPanel.setCardNum(2);
    discardCardPanel.setCardNum(2);
    playCardPanel.setCardNum(1);
    
    dealFHCardPanel.setHandler(FROM_FH, this);
    dealMHCardPanel.setHandler(FROM_MH, this);
    dealRHCardPanel.setHandler(FROM_RH, this);
    dealSKCardPanel.setHandler(FROM_SK, this);    
    discardCardPanel.setHandler(FROM_DISCARD, this);
    playCardPanel.setHandler(FROM_PLAY, this);

    setDealCardCounts();
    
    printButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("");
          Misc.msg(game.toSgf(false, SimpleState.WORLD_VIEW));
        }});

    doneDealButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          // append deal move

          assert(moveIndex == 0);

          int fh = dealFHCardPanel.getSelected();
          int mh = dealMHCardPanel.getSelected();
          int rh = dealRHCardPanel.getSelected();
          int sk = dealSKCardPanel.getSelected();          
          
          if (Hand.numCards(fh) != 10 || Hand.numCards(mh) != 10 ||
              Hand.numCards(rh) != 10 || Hand.numCards(sk) != 2 ||
              (fh | mh | rh | sk) != -1)
            return;
          
          synchronized (game) {
            
            game.prune(moveIndex);

            String move =
              Card.cardListToString(Hand.toCardList(fh)) + "|" +
              Card.cardListToString(Hand.toCardList(mh)) + "|" +
              Card.cardListToString(Hand.toCardList(rh)) + "|" +
              Card.cardListToString(Hand.toCardList(sk));
              
            String r = game.makeMove(SimpleState.WORLD_MOVE, move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal deal move " + r);
            }
          }
          
          gameChanged();
        }});

    showButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          // append show card move

          SimpleState st = game.getState(moveIndex);
          if (st.getDeclarer() < 0) return;
            
          synchronized (game) {
            
            game.prune(moveIndex);

            String move = "SC";
            String r = game.makeMove(st.getDeclarer(), move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal show card move " + r);
            }
          }
          
          gameChanged();
        }});

    declResignButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          // append declarer resign move

          SimpleState st = game.getState(moveIndex);

          if (st.getDeclarer() < 0) return;
          
          if (st.getResigned(0) != 0 ||
              st.getResigned(1) != 0 ||
              st.getResigned(2) != 0)
            return; // already resigned
          
          synchronized (game) {
            
            game.prune(moveIndex);

            String move = "RE";
            String r = game.makeMove(st.getDeclarer(), move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal resign move " + r);
            }
          }
          
          gameChanged();
        }});

    defResignButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          // append defender resign move

          SimpleState st = game.getState(moveIndex);

          int decl = st.getDeclarer();
          if (decl < 0) return;
          
          if (st.getResigned(decl) != 0)
            return; // declarer already resigned

          int player = -1;
          if (st.getResigned((decl+1) % 3) != 0) {
            player = (decl + 2) % 3;
          } else {
            player = (decl + 1) % 3;
          }
            
          synchronized (game) {
            
            game.prune(moveIndex);

            String move = "RE";
            String r = game.makeMove(player, move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal resign move " + r);
            }
          }
          
          gameChanged();
        }});
    
    bidButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {

          String move = bidButton1.getText();

          if (move.equals("yes")) {
            move = "y";
          }
          
          synchronized (game) {
            game.prune(moveIndex);
            String r = game.makeMove(game.getState(moveIndex).getToMove(), move, null);
            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal bid1 move " + r);
            }
          }
          
          gameChanged();
        }});

    bidButton2.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          String move = bidButton2.getText();

          if (move.equals("pass")) {
            move = "p";
          }
          
          synchronized (game) {
            game.prune(moveIndex);
            String r = game.makeMove(game.getState(moveIndex).getToMove(), move, null);
            Misc.msg("MOVE " + move);

            if (r != null) {
              Misc.err("illegal bid2 move " + r);
            }
          }
          
          gameChanged();
        }});

    pickupButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          
          synchronized (game) {
            game.prune(moveIndex);
            String r = game.makeMove(game.getState(moveIndex).getToMove(), "s", null);
            Misc.msg("MOVE s");

            if (r != null) {
              Misc.err("illegal pickup move " + r);
            }
          }

          if (gameView == SimpleState.WORLD_VIEW) {
            // skat known in worldview -> generate move
            String move = Card.cardListToString(Hand.toCardList(game.getState(1).getSkat()));
            String r = game.makeMove(SimpleState.WORLD_MOVE, move, null);

            Misc.msg("MOVE w " + move);

            if (r != null) {
              Misc.err("illegal skat move " + r);
            }

            pickupCardPanel.setAvailable(game.getState(1).getSkat());
            pickupCardPanel.setSelected(game.getState(1).getSkat());
            pickupCardPanel.setEnabled(false);
          } else {
            pickupCardPanel.setEnabled(true);
          }
          
          gameChanged();
        }});


    handButton.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("declare_clicked");
	  
	  synchronized (game) { //!!!revisit
	  
            int gt = -1;

            if      (grandButton1.isSelected()) gt = GameDeclaration.GRAND_GAME;
            else if (clubsButton1.isSelected()) gt = GameDeclaration.CLUBS_GAME;
            else if (spadesButton1.isSelected()) gt = GameDeclaration.SPADES_GAME;
            else if (heartsButton1.isSelected()) gt = GameDeclaration.HEARTS_GAME;
            else if (diamondsButton1.isSelected()) gt = GameDeclaration.DIAMONDS_GAME;
            else if (nullButton1.isSelected()) gt = GameDeclaration.NULL_GAME;
            
            GameDeclaration gd = new GameDeclaration(gt, true,
                                                     ouvertCheckBox1.isSelected(),
                                                     schneiderCheckBox1.isSelected(),
                                                     schwarzCheckBox1.isSelected());

            game.prune(moveIndex);
            
            String move = gd.toString();

            String r = game.makeMove(game.getState(moveIndex).getToMove(), move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal hand declaration move " + r);
            }
          }
          gameChanged();
        }});

    nButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          if (moveIndex < moveEntries.size()-1) moveIndex++;
          moveIndexUpdate();
        }});

    pButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          if (moveIndex > 0) moveIndex--;
          moveIndexUpdate();
        }});

    nnButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          moveIndex += 3;
          if (moveIndex >= moveEntries.size()-1)
            moveIndex = moveEntries.size()-1;
          moveIndexUpdate();
        }});

    ppButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          moveIndex -= 3;
          if (moveIndex < 0)
            moveIndex = 0;
          moveIndexUpdate();
        }});

    nnnButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          moveIndex = moveEntries.size()-1;
          moveIndexUpdate();
        }});

    pppButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          moveIndex = 0;
          moveIndexUpdate();
        }});

    gameClearButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          synchronized (game) {
            game.prune(0);
            moveIndex = 0;
          }

          gameChanged();
        }});

    clearAllButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) { clearAllDealCards(); }});
    
    shuffleButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) { shuffleDealCards(); }});

    clearFHButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          int selected = dealFHCardPanel.getSelected();
          dealFHCardPanel.setSelected(0);
          dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() | selected);
          dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() | selected);
          dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() | selected);

          setDealCardCounts();
        }});
    
    clearMHButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          int selected = dealMHCardPanel.getSelected();
          dealMHCardPanel.setSelected(0);
          dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() | selected);
          dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() | selected);
          dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() | selected);

          setDealCardCounts();
        }});
    
    clearRHButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          int selected = dealRHCardPanel.getSelected();
          dealRHCardPanel.setSelected(0);
          dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() | selected);
          dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() | selected);
          dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() | selected);

          setDealCardCounts();
        }});
    
    clearSKButton.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          int selected = dealSKCardPanel.getSelected();
          dealSKCardPanel.setSelected(0);
          dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() | selected);
          dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() | selected);
          dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() | selected);

          setDealCardCounts();
        }});

    moveTable.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e)
        {
          Point p = e.getPoint();
          int row = moveTable.rowAtPoint(p);
          int column = moveTable.columnAtPoint(p); // This is the view column!

          for (int i=0; i < moveEntries.size(); i++) {
            if (row == moveEntries.get(i).y &&
                column == moveEntries.get(i).x) {
              moveIndex = i;
              moveIndexUpdate();
              break;
            }
          }
        }
      });
    
    // create labels for trickCardPanel
      
    for (int i=0; i < 10; i++) {
      labels[i] = new JLabel();
      labels[i].setFont(new Font(labels[i].getFont().getFontName(),
                                 labels[i].getFont().getStyle(), 12));
    }

    //leaveMenuItem.setEnabled(true);
    //readyButton.setEnabled(true);    
    //showButton.setEnabled(false);
    declareButton.setEnabled(false);

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


    //     noobCheckbox.addActionListener(new ActionListener()
    //       {
    //         public void actionPerformed(ActionEvent e) {

    //           synchronized (clientWindow.sc) {

    //             // switch noob view to self

    //             SimpleGame sg = table.getGame();            
    //             if (sg != null) {
              
    //               int myIndex = sg.getPlayerIndex(me);
    //               if (myIndex < 0)
    //                 panelIndex = -1;
    //               else
    //                 panelIndex = 0;
    //             }
    //           }
          
    //           // refresh game window
          
    //           game2Window(ClientWindow.TablePanelUpdateAction.GUI, null);
    //         }}
    //       );
    

    // input: send tell message to server
    //    inputLine.addActionListener(new ActionListener()
    //      {
    //        public void actionPerformed(ActionEvent e) {
    //
    //          String input = inputLine.getText();
    //          
    //          send("tell " + input);
    //          inputLine.setText("");
    //          inputLine.setCaretPosition(inputLine.getDocument().getLength());
    //          inputLine.requestFocus();
    //
    //          if (input.startsWith("@invite ")) {
    //	    send(input.substring(1)); // remove @ and send invitation
    //	    textArea.append("\n### " + input);
    //          }
    //        }}
    //      );
    
    // buttons
    
    // Add icons to declaration buttons

    int iw = 20;

    URL url = Thread.class.getResource("/data/images/clubs.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    clubsLabel3.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    clubsLabel3.setText("");
    clubsLabel4.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    clubsLabel4.setText("");

    url = Thread.class.getResource("/data/images/spades.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    spadesLabel3.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    spadesLabel3.setText("");
    spadesLabel4.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    spadesLabel4.setText("");

    url = Thread.class.getResource("/data/images/hearts.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    heartsLabel3.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    heartsLabel3.setText("");
    heartsLabel4.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    heartsLabel4.setText("");

    url = Thread.class.getResource("/data/images/diamonds.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    diamondsLabel3.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                        getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    diamondsLabel3.setText("");
    diamondsLabel4.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                        getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    diamondsLabel4.setText("");

    // hand card numbers
    
    iw = 16;

    // hand 0
    url = Thread.class.getResource("/data/images/clubs.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    clubsLabel0.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    clubsLabel0.setText("");

    url = Thread.class.getResource("/data/images/spades.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    spadesLabel0.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                       getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    spadesLabel0.setText("");

    url = Thread.class.getResource("/data/images/hearts.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    heartsLabel0.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                       getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    heartsLabel0.setText("");

    url = Thread.class.getResource("/data/images/diamonds.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    diamondsLabel0.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                         getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    diamondsLabel0.setText("");

    jacksLabel0.setText("");

    // hand 1
    url = Thread.class.getResource("/data/images/clubs.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    clubsLabel1.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    clubsLabel1.setText("");

    url = Thread.class.getResource("/data/images/spades.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    spadesLabel1.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                       getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    spadesLabel1.setText("");

    url = Thread.class.getResource("/data/images/hearts.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    heartsLabel1.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                       getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    heartsLabel1.setText("");

    url = Thread.class.getResource("/data/images/diamonds.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    diamondsLabel1.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                         getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    diamondsLabel1.setText("");

    jacksLabel1.setText("");

    // hand 2
    url = Thread.class.getResource("/data/images/clubs.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    clubsLabel2.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                      getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    clubsLabel2.setText("");

    url = Thread.class.getResource("/data/images/spades.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    spadesLabel2.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                       getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    spadesLabel2.setText("");

    url = Thread.class.getResource("/data/images/hearts.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    heartsLabel2.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                       getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    heartsLabel2.setText("");

    url = Thread.class.getResource("/data/images/diamonds.gif");
    if (url == null) Misc.err(rbs("can't_access_suit_image"));
    diamondsLabel2.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(url).
                                         getScaledInstance(iw, -1, Image.SCALE_SMOOTH)));
    diamondsLabel2.setText("");

    jacksLabel2.setText("");

    grandButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("grand_clicked");
          gameType = GameDeclaration.GRAND_GAME;
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
        }});

    clubsButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("clubs_clicked");
          gameType = GameDeclaration.CLUBS_GAME;
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
        }});
    clubsLabel3.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("clubs_label_clicked");
          gameType = GameDeclaration.CLUBS_GAME;          
          clubsButton1.doClick();
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);          
        }});

    spadesButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("spades_clicked");
          gameType = GameDeclaration.SPADES_GAME;
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
        }});
    spadesLabel3.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("spades_label_clicked");
          gameType = GameDeclaration.SPADES_GAME;          
          spadesButton1.doClick();
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);          
        }});

    heartsButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("hearts_clicked");
          gameType = GameDeclaration.HEARTS_GAME;
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
        }});
    heartsLabel3.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("hearts_label_clicked");
          gameType = GameDeclaration.HEARTS_GAME;          
          heartsButton1.doClick();
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);          
        }});

    diamondsButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("diamonds_clicked");
          gameType = GameDeclaration.DIAMONDS_GAME;
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
        }});
    diamondsLabel3.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("diamonds_label_clicked");
          gameType = GameDeclaration.DIAMONDS_GAME;          
          diamondsButton1.doClick();
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);          
        }});

    nullButton1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("null_clicked");
          gameType = GameDeclaration.NULL_GAME;
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
        }});

    schneiderCheckBox1.addMouseListener(new MouseAdapter() {
        @Override
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("schneider_clicked");
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);	  
        }});

    schwarzCheckBox1.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("schwarz_clicked");
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);	  
        }});

    ouvertCheckBox1.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("ouvert_clicked");
          game2Window(TablePanelUpdateAction.GUI, moveIndex, null);	  
        }});

    declareButton.addMouseListener(new MouseAdapter() {
        @Override        
          public void mouseClicked(MouseEvent evt) {
          Misc.msg("declare_clicked");
	  GameDeclaration gd;
	  boolean hand;
	  int maxBid;
	  
	  synchronized (game) { //!!!revisit
	  
            int gt = -1;

            if      (grandButton2.isSelected()) gt = GameDeclaration.GRAND_GAME;
            else if (clubsButton2.isSelected()) gt = GameDeclaration.CLUBS_GAME;
            else if (spadesButton2.isSelected()) gt = GameDeclaration.SPADES_GAME;
            else if (heartsButton2.isSelected()) gt = GameDeclaration.HEARTS_GAME;
            else if (diamondsButton2.isSelected()) gt = GameDeclaration.DIAMONDS_GAME;
            else if (nullButton2.isSelected()) gt = GameDeclaration.NULL_GAME;
            else if (nullOuvertButton2.isSelected()) gt = GameDeclaration.NULL_GAME;
            
	    gd = new GameDeclaration(gt, false,
				     nullOuvertButton2.isSelected(),
				     false,
                                     false);

            game.prune(moveIndex);
            
            String move = gd.toString() + "." + Card.cardListToString(Hand.toCardList(discardCardPanel.getSelected()));

            String r = game.makeMove(game.getState(moveIndex).getToMove(), move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal declare move " + r);
            }
          }
          gameChanged();
        }});

    clubsButton1.doClick();
    gameType = GameDeclaration.CLUBS_GAME;
    
    // cardpanel callback

    cardPanel0.setHandler(FROM_0, this);
    cardPanel1.setHandler(FROM_1, this);
    cardPanel2.setHandler(FROM_2, this);
    trickCardPanel.setHandler(FROM_TRICK, this);    
    
    // load card images
    // fixme: should reuse card images across tables (CardImage factory?)

    setVisible(true);

    game2Window(TablePanelUpdateAction.GUI, moveIndex, null);    
  }

  CardImages getCardImages(int width)
  {
    String deck = "E4";
    CardImages ci = cardMap.get(new Integer(width));

    if (ci == null) {
      // create card images and cache them
      ci = new CardImages();
      ci.loadCards("/data/cards/"+deck, width);
      cardMap.put(new Integer(width), ci);
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
  
  void send(String msg)
  {
    Misc.msg("send " + msg);
  }

  // handle messages from components
  
  public boolean handleEvent(EventMsg msg)
  {
    synchronized (getThis()) { //!!! revisit when external events get handled
    
      SimpleGame sg = game;

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

          if (!game.isFinished()) {

            // move
        
            SimpleState st = sg.getCurrentState();
            if (!st.isViewerToMove())
              break; // not me to move
        
            if (st.getPhase() == SimpleState.SKAT_OR_HAND_DECL) {
              send("play s");
            }
        
          } else {

            // select history point

            int ind = Integer.parseInt(msg.getArgs().get(0));
            if (ind < remCardsIndex) {
              if      (ind < 2) moveIndex = 0; // deal
              else if (ind < 7) moveIndex = game.getTrickBookmark(0); // before cardplay
              else moveIndex = game.getTrickBookmark((ind - 7) / 3 + 1)+3;
            }

            moveIndexUpdate();
          }
          break;
        }

      } else if (msg.getSender().equals(FROM_FH)) {

        int bit = Integer.parseInt(msg.getArgs().get(0));
        dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() ^ bit);
        dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() ^ bit);
        dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() ^ bit);
        inferRemainingCards();
        return true;

      } else if (msg.getSender().equals(FROM_MH)) {

        int bit = Integer.parseInt(msg.getArgs().get(0));
        dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() ^ bit);
        dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() ^ bit);
        dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() ^ bit);
        inferRemainingCards();
        return true;
        
      } else if (msg.getSender().equals(FROM_RH)) {

        int bit = Integer.parseInt(msg.getArgs().get(0));
        dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() ^ bit);
        dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() ^ bit);
        dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() ^ bit);
        inferRemainingCards();
        return true;
        
      } else if (msg.getSender().equals(FROM_SK)) {        

        int bit = Integer.parseInt(msg.getArgs().get(0));
        dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() ^ bit);
        dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() ^ bit);
        dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() ^ bit);
        inferRemainingCards();
        return true;

      } else if (msg.getSender().equals(FROM_DISCARD)) {

        declareButton.setEnabled(Hand.numCards(discardCardPanel.getSelected()) == 2);
        return true;

      } else if (msg.getSender().equals(FROM_PLAY)) {

        int sel = playCardPanel.getSelected();

        if (sel != 0) {

          // new move played

          synchronized (game) {
            
            game.prune(moveIndex);

            String move = Card.cardListToString(Hand.toCardList(sel));
            Misc.msg("CARDPLAY MOVE: " + move);
            String r = game.makeMove(game.getState(moveIndex).getToMove(), move, null);

            Misc.msg("MOVE " + move);
            
            if (r != null) {
              Misc.err("illegal card move " + r);
            }
          }
          
          gameChanged();
        }
        return true;
      }
    }      
    game2Window(TablePanelUpdateAction.UNKNOWN, moveIndex, null);        
    return true;
  }

  private void gameChanged() {
    game2MoveTable();
    moveIndex = moveEntries.size()-1;
    moveIndexUpdate();
  }
  
  private void shuffleDealCards()
  {
    ArrayList<Card> cards = SimpleState.fullDeck();
    SimpleState.shuffle(cards, rng);

    int fh=0, mh=0, rh=0;
    
    for (int i=0; i < 10; i++) {
      fh |= cards.get(i).toBit();
      mh |= cards.get(i+10).toBit();
      rh |= cards.get(i+20).toBit();      
    }

    setAllDealCards(fh, mh, rh);
  }

  private void setAllDealCards(int fh, int mh, int rh)
  {
    int sk = ~(fh|mh|rh);
    assert(Hand.numCards(sk) == 2);
    
    dealFHCardPanel.setAvailable(fh);
    dealMHCardPanel.setAvailable(mh);
    dealRHCardPanel.setAvailable(rh);
    dealSKCardPanel.setAvailable(sk);
    dealFHCardPanel.setSelected(fh);
    dealMHCardPanel.setSelected(mh);
    dealRHCardPanel.setSelected(rh);
    dealSKCardPanel.setSelected(sk);

    setDealCardCounts();
  }

  private void clearAllDealCards()
  {
    dealFHCardPanel.setAvailable(~0);
    dealMHCardPanel.setAvailable(~0);
    dealRHCardPanel.setAvailable(~0);
    dealSKCardPanel.setAvailable(~0);
    dealFHCardPanel.setSelected(0);
    dealMHCardPanel.setSelected(0);
    dealRHCardPanel.setSelected(0);
    dealSKCardPanel.setSelected(0);
    
    setDealCardCounts();
  }

  private void setDealCardCounts()
  {
    countFHLabel.setText(""+Hand.numCards(dealFHCardPanel.getSelected()));
    countMHLabel.setText(""+Hand.numCards(dealMHCardPanel.getSelected()));
    countRHLabel.setText(""+Hand.numCards(dealRHCardPanel.getSelected()));
    countSKLabel.setText(""+Hand.numCards(dealSKCardPanel.getSelected()));    
  }
  
  private void inferRemainingCards()
  {
    int fh = dealFHCardPanel.getSelected();
    int mh = dealMHCardPanel.getSelected();
    int rh = dealRHCardPanel.getSelected();
    int sk = dealSKCardPanel.getSelected();
      
    if (Hand.numCards(fh) == 10 &&
        Hand.numCards(mh) == 10 &&
        Hand.numCards(rh) == 10 &&
        Hand.numCards(sk) <   2) {

      int r = ~(fh | mh | rh);
      
      dealSKCardPanel.setSelected(r);
      dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() & ~r);
      dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() & ~r);
      dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() & ~r);
    }

    if (Hand.numCards(fh) <  10 &&
        Hand.numCards(mh) == 10 &&
        Hand.numCards(rh) == 10 &&
        Hand.numCards(sk) ==  2) {

      int r = ~(mh | rh | sk);
      
      dealFHCardPanel.setSelected(r);
      dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() & ~r);
      dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() & ~r);
      dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() & ~r);
    }

    if (Hand.numCards(fh) == 10 &&
        Hand.numCards(mh) <  10 &&
        Hand.numCards(rh) == 10 &&
        Hand.numCards(sk) ==  2) {

      int r = ~(fh | rh | sk);
      
      dealMHCardPanel.setSelected(r);
      dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() & ~r);
      dealRHCardPanel.setAvailable(dealRHCardPanel.getAvailable() & ~r);
      dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() & ~r);
    }

    if (Hand.numCards(fh) == 10 &&
        Hand.numCards(mh) == 10 &&
        Hand.numCards(rh) <  10 &&
        Hand.numCards(sk) ==  2) {

      int r = ~(fh | mh | sk);
      
      dealRHCardPanel.setSelected(r);
      dealFHCardPanel.setAvailable(dealFHCardPanel.getAvailable() & ~r);
      dealMHCardPanel.setAvailable(dealMHCardPanel.getAvailable() & ~r);
      dealSKCardPanel.setAvailable(dealSKCardPanel.getAvailable() & ~r);
    }

    setDealCardCounts();
  }
  
  public void windowClosing(WindowEvent e) {}

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
    Component c = (Component)evt.getSource();
    // Get new size
    Dimension newSize = c.getSize();

    Misc.msg("NEW SIZE = " + c.getWidth() + " " + c.getHeight());
    invalidate();
    validate();
    
    game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
  }
  
  // assumes table is locked
  private void setHandTitles(SimpleState st, int myIndex)
  {
    for (int p=0; p < 3; p++) { // player index in game
      
      String titleA = posNames[p] + " " + game.getPlayerName(p);
      String titleB = "";
      
      if (st.getDeclarer() >= 0) {
        
        if (st.getDeclarer() == p) {
          titleB += rbs("Decl.");
          if (st.getGameDeclaration().type != GameDeclaration.NO_GAME)
            titleB += " | " + rbs(st.getGameDeclaration().typeToVerboseString()) +
              st.getGameDeclaration().modifiersToVerboseString();
        } else {
          titleB += rbs("Def.");
        }
      }

      int mb = st.getMaxBid(p);
      //     Misc.msg("MB " + p + " " + mb);
      if (mb == 0)
        titleA += " | ?";
      else if (mb == 1)
        titleA += " | " + rbs("Pass");
      else
        titleA += " | " + mb;

      if (st.getResigned(p) != 0) { titleA += " | " + rbs("Resigned"); }
      if (st.getLeft() == p)      { titleA += " | " + rbs("Left"); }
      if (st.getTimeOut() == p)   { titleA += " | " + rbs("Timeout"); }      
      
      StringBuilder sb = new StringBuilder();
      Formatter formatter = new Formatter(sb, Locale.US);
      int secs = (int)Math.round(10.0); //!!! fixme
      
      formatter.format("%s%d:%02d", (secs < 0 ? "-" : ""), Math.abs(secs)/60, Math.abs(secs) % 60);
      titleA += " | " + sb.toString();

      int toMove = st.getToMove(); // trick leader

      // tomove indication
      
      if (toMove == p) {
        int phase = st.getPhase();

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
        !game.isFinished() && SimpleState.isPlayer(game.getOwner()) &&
        myIndex == p ? color1 : color2;

      if (!game.isFinished() && !SimpleState.isPlayer(game.getOwner()))
        toMove = st.getToMove(); // observing: consider current player to move
      
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
    if (diamondsButton1.isSelected())
      return GameDeclaration.DIAMONDS_GAME;

    if (heartsButton1.isSelected())
      return GameDeclaration.HEARTS_GAME;

    if (spadesButton1.isSelected())
      return GameDeclaration.SPADES_GAME;

    if (clubsButton1.isSelected())
      return GameDeclaration.CLUBS_GAME;

    if (grandButton1.isSelected())
      return GameDeclaration.GRAND_GAME;

    if (nullButton1.isSelected())
      return GameDeclaration.NULL_GAME;

    Misc.err("illegal game type button selection");
    return GameDeclaration.NO_GAME;
  }
  
  // set radio buttons to game type when playing cards
  // assumes table is locked
  void setDeclarationButtons()
  {
    SimpleGame sg = game;
    if (sg.getGameDeclaration().type != GameDeclaration.NO_GAME) {

      gameType = sg.getGameDeclaration().type;
      
      switch (gameType) {
      case GameDeclaration.DIAMONDS_GAME: diamondsButton1.doClick(); break;
      case GameDeclaration.HEARTS_GAME:   heartsButton1.doClick(); break;
      case GameDeclaration.SPADES_GAME:   spadesButton1.doClick(); break;
      case GameDeclaration.CLUBS_GAME:	  clubsButton1.doClick(); break;
      case GameDeclaration.NULL_GAME:	  nullButton1.doClick(); break;
      case GameDeclaration.GRAND_GAME:	  grandButton1.doClick(); break;
      }
      
      ouvertCheckBox1.setSelected(sg.getGameDeclaration().ouvert);
      schneiderCheckBox1.setSelected(sg.getGameDeclaration().schneiderAnnounced);
      schwarzCheckBox1.setSelected(sg.getGameDeclaration().schwarzAnnounced);
    }

    if (!sg.isFinished()) {
      int phase = sg.getCurrentState().getPhase();
      switch (phase) {
      case SimpleState.BID:
      case SimpleState.ANSWER:

        ouvertCheckBox1.setSelected(false);
        schneiderCheckBox1.setSelected(false);
        schwarzCheckBox1.setSelected(false);
        break;
        
      case SimpleState.DISCARD_AND_DECL:

        schneiderCheckBox1.setSelected(false);
        schwarzCheckBox1.setSelected(false);

        if (gameType != GameDeclaration.NULL_GAME) {
          ouvertCheckBox1.setSelected(false);
        }
        break;

      case SimpleState.SKAT_OR_HAND_DECL: 

        if (gameType == GameDeclaration.NULL_GAME) {

          schneiderCheckBox1.setSelected(false);
          schwarzCheckBox1.setSelected(false);

        } else {
          
          if (ouvertCheckBox1.isSelected()) {
            schwarzCheckBox1.setSelected(true);
          }
          if (schwarzCheckBox1.isSelected()) {
            schneiderCheckBox1.setSelected(true);
          }
        }
        
        break;
      }
    }
  }


  class MoveEntry
  {
    String s;
    int x, y;  // move table coordinates
    int ind;   // index in move list
  }
  
  ArrayList<MoveEntry> moveEntries = new ArrayList<MoveEntry>();

  String card2html(char c1, char c2)
  {
    String s = "<b><font color=";
    switch (c1) {
    case 'D': s += "#A4850A>"; break;
    case 'H': s += "#E00000>"; break;
    case 'S': s += "#00A000>"; break;
    case 'C': s += "#000000>"; break;
    default: Misc.err("suit " + c1 + " ?");
    }

    switch (c2) {
    case '7':
    case '8':
    case '9': s += c2; break;
    case 'T': s += "10"; break;
    case 'J': s += 'J'; break;
    case 'Q': s += 'Q'; break;
    case 'K': s += 'K'; break;            
    case 'A': s += 'A'; break;
    default: Misc.err("rank " + c2 + " ?");
    }

    s += "</font></b>";
    return s;
  }
  
  void game2MoveTable()
  {
    int x = 0;
    int y = 0;
    ArrayList<Move> mh = game.getMoveHist();
    moveEntries = new ArrayList<MoveEntry>();
    
    for (int i=0; i < game.getMoveHist().size(); i++) {

      Move mv = mh.get(i);

      Misc.msg(">> " + mv.source + " " + mv.action);
      
      MoveEntry m = new MoveEntry();
      if (x >= 3) { x = 0; y++; }
      m.x = x; m.y = y;
      m.ind = i;
      
      for (;;) {
        
        if (i == 0) {
          
          // deal
          m.s = "deal";
          x = 0; y++;
          break;
        }

        if (mv.source == SimpleState.WORLD_MOVE) {
          
          if (mv.action.length() == 5) {

            // skat
            m.s = mv.action;
            
            m.s = "<html>" + card2html(mv.action.charAt(0), mv.action.charAt(1)) + 
              card2html(mv.action.charAt(3), mv.action.charAt(4)) + "</html>";

            x++;
            
          } else {

            // left or timeout
            m.s = mv.action;
            x++;
          }

          break;
        }

        if (mv.source == SimpleState.FORE_HAND) {
          m.s = "F ";
        } else if (mv.source == SimpleState.MIDDLE_HAND) {
          m.s = "M ";
        } else {
          m.s = "R ";
        }

        if (mv.action.equals("RE")) {
          m.s += "RE";
          m.x = 0; m.y++;
          x = 1;
          y++;
        } else if (mv.action.equals("SC")) {
          m.s += "SC";
          y++;
        } else if (mv.action.equals("p")) {
          m.s += "pass";
          x = 0; y++;
        } else if (mv.action.equals("y")) {
          m.s += "yes";
          x = 0; y++;
        } else if (mv.action.equals("s")) {
          m.s += "pickup";
          x++;
        } else {

          if (mv.action.charAt(0) >= '0' &&
              mv.action.charAt(0) <= '9') {
            
            // bid
            m.s += mv.action;
            x++;
            if (mv.source == SimpleState.FORE_HAND) {
              // pass pass 18
              x = 0; y++;
            }
            break;
          }

          String[] parts = mv.action.split("\\.");      

          if (parts.length == 3) {

            // contract + discarded cards

            assert parts.length == 3;

            m.s = "<html>" + m.s + parts[0] + " " +
              card2html(parts[1].charAt(0), parts[1].charAt(1)) +
              card2html(parts[2].charAt(0), parts[2].charAt(1)) + "</html>";

            x = 0;
            y++;
            break;
            
          }

          if (Card.fromStringAccurate(mv.action) != null) {

            // card
            
            String s = "<html>" + m.s + " " + card2html(mv.action.charAt(0), mv.action.charAt(1));
            s += "</html>";
            Misc.msg(s);
            m.s = s;
            x++;
            break;

          }

          // hand contract
          m.s += mv.action;
          x = 0; y++;
          break;
        }

        break;
      }

      moveEntries.add(m);
    }

    {
      // add next move

      if (x >= 3) { x = 0; y++; }
      
      SimpleState s = game.getCurrentState();
      MoveEntry m = new MoveEntry();
      m.x = x; m.y = y;
    
      switch (s.getPhase()) {
      
      case SimpleState.DEAL:
        m.s = "?deal";
        break;
      
      case SimpleState.BID:
        m.s = "?bid";
        break;
      
      case SimpleState.ANSWER:
        m.s = "?answer";
        break;
      
      case SimpleState.SKAT_OR_HAND_DECL:
        m.s = "?pickup";
        break;
      
      case SimpleState.GET_SKAT:
        m.s = "?skat";
        break;
      
      case SimpleState.DISCARD_AND_DECL:
        m.s = "?declare";
        break;
      
      case SimpleState.CARDPLAY:
        m.s = "?card";
        break;
      
      case SimpleState.FINISHED:
        m.s = "finished";
        if (m.x != 0) {
          m.x = 0;
          m.y++;
          x = 1;
        }
        y++;
        break;
      }
    
      moveEntries.add(m);
    }
  
    Object[][] objs = new Object[y+1][3];

    for (int i=0; i < moveEntries.size(); i++) {
      MoveEntry m = moveEntries.get(i);
      Misc.msg("" + m.y + " " + m.x + " " + m.s);
      objs[m.y][m.x] = m.s;
    }
    
    moveTable.
      setModel(new javax.
               swing.table.
               DefaultTableModel(objs,
                                 new String [] {
                                   "Move", "Move", "Move"
                                 }
                                 ) {
          Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class, java.lang.String.class
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

    DefaultTableCellRenderer moveLeft = new DefaultTableCellRenderer() {
        @Override
          public Component getTableCellRendererComponent(JTable table, Object value,
                                                         boolean isSelected, boolean hasFocus,
                                                         int row, int column) {
	  Component renderer =
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

          setBackground(Color.WHITE);            
          
	  if (value != null) {
            // Misc.msg("" + moveEntries.size() + " " + row + " " + column);
            if (moveEntries.size() > 0 &&
                row == moveEntries.get(moveIndex).y && column == moveEntries.get(moveIndex).x) {
              setBackground(selectColor);
            } 
            setText(value.toString());
	  } else {
	    setText("");
          }
	  setHorizontalAlignment(JLabel.LEFT);

	  return renderer;
	}
      };

    for (int i=0; i < 3; i++) {
      moveTable.getColumnModel().getColumn(i).setCellRenderer(moveLeft);
    }

  }

  
  private void moveIndexUpdate() {
    int tab = DEAL_TAB;
    
    boolean newMove = moveIndex >= game.getMoveHist().size();

    Misc.msg("INDEX UPDATE " + moveIndex + " " + newMove);
    SimpleState st = game.getState(moveIndex);
    
    switch (st.getPhase()) {

    case SimpleState.DEAL:
      tab = DEAL_TAB;

      if (newMove) {
        clearAllDealCards();
      } else {
        // copy deal cards from game
        setAllDealCards(game.getHandInitial(0), game.getHandInitial(1), game.getHandInitial(2));
      }
      break;
      
    case SimpleState.BID:
      tab = BID_TAB;
      if (st.getAsked() >= 3) {
        bidLabel.setText(playerPos[st.getBidder()] + " bid");        
      } else {
        bidLabel.setText(playerPos[st.getBidder()] + " bids to " + playerPos[st.getAsked()]);
      }
      bidButton1.setText(""+st.nextBid());
      bidButton2.setText("pass");
      break;
      
    case SimpleState.ANSWER:
      tab = BID_TAB;
      bidLabel.setText(playerPos[st.getAsked()] + "'s answer to " + playerPos[st.getBidder()] + "'s " + st.getMaxBid());
      bidButton1.setText("yes");
      bidButton2.setText("pass");
      break;
      
    case SimpleState.SKAT_OR_HAND_DECL:
      tab = PICKUP_TAB;
      break;
      
    case SimpleState.GET_SKAT:
      tab = SKAT_TAB;
      if (newMove) {
        pickupCardPanel.setAvailable(~0);
        pickupCardPanel.setSelected(0);        
      } else {
        pickupCardPanel.setAvailable(game.getState(1).getSkat());
        pickupCardPanel.setSelected(game.getState(1).getSkat());        
      }
      break;
      
    case SimpleState.DISCARD_AND_DECL:
      tab = CONTRACT_TAB;

      discardCardPanel.setAvailable(st.getHand(st.getToMove()) | st.getSkat());

      if (newMove) {

        discardCardPanel.setSelected(0);
        grandButton2.doClick();
        declareButton.setEnabled(false);
        nullButton2.setEnabled(st.getMaxBid() <= 23);
        nullOuvertButton2.setEnabled(st.getMaxBid() <= 46);
        
      } else {

        SimpleState st2 = game.getState(moveIndex+1);
        discardCardPanel.setSelected(st2.getSkat());

        GameDeclaration decl = st2.getGameDeclaration();
        
        if (decl.type == GameDeclaration.GRAND_GAME) grandButton2.doClick();
        if (decl.type == GameDeclaration.CLUBS_GAME) clubsButton2.doClick();
        if (decl.type == GameDeclaration.SPADES_GAME) spadesButton2.doClick();
        if (decl.type == GameDeclaration.HEARTS_GAME) heartsButton2.doClick();
        if (decl.type == GameDeclaration.DIAMONDS_GAME) diamondsButton2.doClick();        
        if (decl.type == GameDeclaration.NULL_GAME) {
          if (!decl.ouvert) nullButton2.doClick(); else nullOuvertButton2.doClick();
        }
        declareButton.setEnabled(true);
      }
      
      break;
      
    case SimpleState.CARDPLAY:
      tab = CARDPLAY_TAB;

      playCardPanel.setAvailable(legalCardMoves(moveIndex));
      
      if (newMove) {
        playCardPanel.setSelected(0);
      } else {
        Card card = Card.fromString(game.getMoveHist().get(moveIndex).action);
        if (card != null) 
          playCardPanel.setSelected(card.toBit());
      }

      break;

    case SimpleState.FINISHED:
      tab = FINISHED_TAB;
      break;

    default:
      Misc.err("shouldn't get here " + st.getPhase());
    }
    
    actionTabbedPane.setSelectedIndex(tab);
    game2Window(TablePanelUpdateAction.GUI, moveIndex, null);
    moveTable.repaint();
  }

  int legalCardMoves(int index)
  {
    String[] moves = new String[10];
    
    int n = game.getState(index).genMoves(moves);

    int m = 0;

    for (int i=0; i < n; i++) {
      m |= Card.fromString(moves[i]).toBit();
    }

    return m;
  }

  // sIndex : current state index
  // < 0: end of game
  void game2Window(TablePanelUpdateAction action, int sIndex, String[] params) {
    Misc.msg("game2Window");
    
    synchronized (game) { //!!! revisit

      if (game == null) return; // no game

      if (sIndex < 0) {
        sIndex = game.getStateNum()-1;
      }

      if (sIndex > game.getStateNum()-1) {
        sIndex = game.getStateNum()-1;
      }

      SimpleState state = game.getState(sIndex);

      setCardImages();
      
      // display game state

      DisplayMode mode;
      int myIndex = game.getPlayerIndex(me);  // < 0 means observer (converted to 0 by max) when nece.

      // show/hide suit information panels

      boolean noob = true;
      
      suitPanel0.setVisible(noob);
      suitPanel1.setVisible(noob);
      suitPanel2.setVisible(noob);

      boolean mePlaying = true; //mode == DisplayMode.PLAYING && (myIndex >= 0 && st.getDeclarer() == myIndex);
      boolean meToMove = state.isViewerToMove();
      Color activeHandPanelColor, toMoveColor;
      toMoveColor = (meToMove && true /*mode == DisplayMode.PLAYING*/) ? color1 : color2;
      activeHandPanelColor = toMoveColor;
    
      int[] histStateIndexes = new int[11]; // historyIndex -> state index

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
      
      Card[] skat = game.getOriginalSkat();
      
      if (moveIndex > 0 && skat != null) {
        cards.add(skat[0]);
        cards.add(skat[1]);
      } else {
        cards.add(new Card());
        cards.add(new Card());
      }
      
      int cardPlayStart = game.getCardPlayBookmark();
      int decl = -1;
      int skatPts = 0;
    
      // 12 slots:  skat / discard / 10 tricks
    
      int w = trickCardPanel.getUsableWidth();
      int h = trickCardPanel.getUsableHeight();
      int cw = cardImagesTrickSmall.getWidth(); // card width
      int ch = cardImagesTrickSmall.getHeight(); // card height
      int x0 = (int)(cw/2*1.9);
      int dx = (w-x0)/10;
      int dy = 10;   // pts raised when card played in trick
      int yc = dy + (int)(1.5*ch/2);
      
      if (cardPlayStart >= 0) {
	
        // game was declared or history
      
        if (!noob) {

          // current trick in the center, previous trick to the left

          ArrayList<Card> trickCards = new ArrayList<Card>();

          state.getCurrentTrick(trickCards);
          imp = Utils.cardsToImages(trickCards, cardImagesTrickBig);

          if (imp.size() > 0) {

            Misc.msg("\n\nCURRENT TRICK : "); // !!!
            for (Card c : trickCards) {
              Misc.msg(c.toString());
            }
            
            // fixme: consolidate constants so that they are size independent
            // triangle layout, first card in trick drawn first
           
            int xc2 = w/2; // panel center
            int yc2 = h/2;
            int cw2 = cardImagesTrickBig.getWidth(); // card width
            int ch2 = cardImagesTrickBig.getHeight(); // card height            
            double[] xd = new double[] { -1.35/3, +1.35/3, 0  };
            double[] yd = new double[] { -0.61/3, 0,  +0.61/3 };

            // find trick leader

            int k = game.getMoveHist().size();
            
            for (int l = 0; l < imp.size(); l++) {
              // skip non-card moves
              while (Card.fromString(game.getMoveHist().get(--k).action) == null) { }
            }

            int trickLeader = game.getMoveHist().get(k).source;
            int topLeftIndex = 1; // observer
            if (myIndex >= 0) // player
              topLeftIndex = (0 /*!!! fix table.playerInGameIndex(me)*/ + 1) % 3;
            int startIndex = (trickLeader - topLeftIndex + 3) % 3;

            for (int i=0; i < imp.size(); i++) {
              imp.get(i).x = xc2 - cw2/2 + (int)(cw2*xd[(startIndex+i) % 3]);
              imp.get(i).y = yc2 - ch2/2 + (int)(ch2*yd[(startIndex+i) % 3]);

              Misc.msg("pos= " + imp.get(i).x + " " + imp.get(i).y); // !!!
            }

            // previous trick to the left

            ArrayList<Card> prevTrickCards = new ArrayList<Card>();
            ArrayList<Card> currTrickCards = new ArrayList<Card>();
            
            ArrayList<Move> moves = game.getMoveHist();
            int i;
            for (i=sIndex-1; i >= 0; i--) {
              if (moves.get(i).action.length() == 2) {
                Card card = Card.fromString(moves.get(i).action);
                if (card != null) {
                  prevTrickCards.add(0, card);
                  state.getCurrentTrick(currTrickCards);
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
              state.getCurrentTrick(currTrick);

              for (int j=0; j < currTrick.size(); j++) {
                prevTrickCards.remove( prevTrickCards.size()-1);
              }
              startIndex = (trickLeader - topLeftIndex + 3) % 3;
 
              Misc.msg("LAST TRICK : "); // !!!
              for (Card c : prevTrickCards) {
                Misc.msg(c.toString());
              }
          
              ArrayList<ImagePos> imp2 = Utils.cardsToImages(prevTrickCards, cardImagesTrickSmall);
              cw2 = cardImagesTrickSmall.getWidth();  // card width
              ch2 = cardImagesTrickSmall.getHeight(); // card height            
              
              for (int j=0; j < 3; j++) {
                imp2.get(j).x = cw2 - cw2/2 + (int)(cw2*xd[(startIndex+j) % 3]);
                imp2.get(j).y = yc2 - ch2/2 + (int)(ch2*yd[(startIndex+j) % 3]);
                imp.add(imp2.get(j));
                Misc.msg("pos= " + imp2.get(j).x + " " + imp2.get(j).y); // !!!                
              }
            }
          }

        } else {

          // noob mode
          
          // complete history

          if (cardPlayStart >= 0) {

            {
              // void info

              int type = state.getGameDeclaration().type;

              {
                // hand panel 0
                int gi = (Math.max(myIndex,0)+0) % 3;
                int voids = state.getVoids(gi);
                clubsLabel0.setText   (((voids & (1 << Card.SUIT_CLUBS))    != 0) ? "0" : "?");
                spadesLabel0.setText  (((voids & (1 << Card.SUIT_SPADES))   != 0) ? "0" : "?");
                heartsLabel0.setText  (((voids & (1 << Card.SUIT_HEARTS))   != 0) ? "0" : "?");
                diamondsLabel0.setText(((voids & (1 << Card.SUIT_DIAMONDS)) != 0) ? "0" : "?");
                if (type == GameDeclaration.GRAND_GAME) {
                  jacksLabel0.setText(((voids & SimpleState.JACK_VOID_BIT) != 0) ? " J: 0" : " J: ?");
                } else
                  jacksLabel0.setText("");
              }

              {
                // hand panel 1
                int gi = (Math.max(myIndex,0)+1) % 3;
                int voids = state.getVoids(gi);
                clubsLabel1.setText   (((voids & (1 << Card.SUIT_CLUBS))    != 0) ? "0" : "?");
                spadesLabel1.setText  (((voids & (1 << Card.SUIT_SPADES))   != 0) ? "0" : "?");
                heartsLabel1.setText  (((voids & (1 << Card.SUIT_HEARTS))   != 0) ? "0" : "?");
                diamondsLabel1.setText(((voids & (1 << Card.SUIT_DIAMONDS)) != 0) ? "0" : "?");
                if (type == GameDeclaration.GRAND_GAME) {
                  jacksLabel1.setText(((voids & SimpleState.JACK_VOID_BIT) != 0) ? " J: 0" : " J: ?");
                } else
                  jacksLabel1.setText("");
              }

              {
                // hand panel 2
                int gi = (Math.max(myIndex,0)+2) % 3;
                int voids = state.getVoids(gi);
                clubsLabel2.setText   (((voids & (1 << Card.SUIT_CLUBS))    != 0) ? "0" : "?");
                spadesLabel2.setText  (((voids & (1 << Card.SUIT_SPADES))   != 0) ? "0" : "?");
                heartsLabel2.setText  (((voids & (1 << Card.SUIT_HEARTS))   != 0) ? "0" : "?");
                diamondsLabel2.setText(((voids & (1 << Card.SUIT_DIAMONDS)) != 0) ? "0" : "?");
                if (type == GameDeclaration.GRAND_GAME) {
                  jacksLabel2.setText(((voids & SimpleState.JACK_VOID_BIT) != 0) ? " J: 0" : " J: ?");
                } else
                  jacksLabel2.setText("");
              }
            }

            SimpleState sc = game.getState(cardPlayStart); // before first card is played
            decl = sc.getDeclarer();
            
            if (sc.getSkat0() != null && sc.getSkat0().isKnown()) {
              skatPts = sc.getSkat0().value() + sc.getSkat1().value();
            }
            
            // discarded cards
            cards.add(sc.getSkat0());
            cards.add(sc.getSkat1());
            
            // all played cards
            ArrayList<Move> moves = game.getMoveHist();
            toMove.add(-1); toMove.add(-1); toMove.add(-1); toMove.add(-1);
            ptsDecl.add(0); ptsDecl.add(0); ptsDecl.add(0); ptsDecl.add(0);
            ptsDefs.add(0); ptsDefs.add(0); ptsDefs.add(0); ptsDefs.add(0);
            
            int cn = 0;
            int hi = 1;
            for (int i=cardPlayStart; i <= sIndex-1; i++) {
              Card c = Card.fromString(moves.get(i).action);
              if (c != null) {
                
                if (cn % 3 == 0) histStateIndexes[hi++] = i;  // new trick
                cn++;
                
                // played card: append move and values
                cards.add(c);
                toMove.add(moves.get(i).source);
                sc = game.getState(i+1); // pts after trick
                int declPts = sc.getTrickPoints(decl);
                int sum = sc.getTrickPoints(0)+sc.getTrickPoints(1)+sc.getTrickPoints(2);
                ptsDecl.add(declPts+skatPts);
                ptsDefs.add(sum-declPts);
              }
            }
            histStateIndexes[0] = histStateIndexes[1]; // forehand when clicking on skat
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
      
          for (int i=4; i < cards.size(); i++) {

            int trickNum = (i-4) / 3;
            int withinTrick = (i-4) % 3;
            int xc = x0 + trickNum*dx + dx/2; // triangle center
            int cardIndex = (toMove.get(i) - Math.max(myIndex,0) + 3 + 2) % 3;
        
            imp.get(i).x = xc - cw/2 + (int)(cw*xd[cardIndex]);
            imp.get(i).y = yc - 3 - dy/2 - ch/2 + (int)(ch*yd[cardIndex]);

            if (i % 3 == 1) {

              trickCardPanel.remove(labels[trickNum]);
              trickCardPanel.add(labels[trickNum],
                                 new org.netbeans.lib.awtextra.AbsoluteConstraints(x0+dx/6+trickNum*dx, // !!!
                                                                                   yc + (int)((1.0-yd[2])*ch) + 25 - ((i/3)%2)*4, -1, -1));
              labels[trickNum].setVisible(true);
              String s = "";

              // !!!if (historyIndex >= 1 && trickNum == historyIndex - 1) s = "<html><u>";

              s += "" + (trickNum+1)+ posNamesShort[toMove.get(i)] + "|" +
                (i+2 < cards.size() ?
                 (ptsDecl.get(i+2) + "|" + ptsDefs.get(i+2)) :
                 (ptsDecl.get(i)   + "|" + ptsDefs.get(i)));

              // if (historyIndex >= 1 && trickNum == historyIndex - 1) s += "</u></html>";

              labels[trickNum].setText(s);
            }
          }

          // remaining cards: full deck - played cards (- known player cards)

          int rem = -1; // full deck
          
          for (int i=0; i < 3; i++) { // player index in game

            int hand, playedCards;
	    
            hand = state.getHand(i);
            playedCards = state.getPlayedCards(i);

            if (panelIndex >= 0 && panelIndex == (3-Math.max(myIndex,0)+i) % 3) {
              rem = Hand.clear(rem, hand);
              if (i == decl && game.getCardPlayBookmark() >= 0) {
                if (state.getSkat0() != null && state.getSkat0().isKnown()) {
                  rem = Hand.clear(rem, state.getSkat0());
                  rem = Hand.clear(rem, state.getSkat1());
                }
              }
            }

            rem = Hand.clear(rem, playedCards);
          }

          Card[] remCardsArray = new Card[40];
          int mn = Hand.toCardArray(rem, remCardsArray);
          ArrayList<Card> remCards = Card.cardArrayToList(mn, remCardsArray);
          remCardsIndex = imp.size();
          int gt = game.getCurrentState().getGameDeclaration().type;
          if (gt == GameDeclaration.NO_GAME)
            gt = GameDeclaration.GRAND_GAME;
	  
          Utils.sortCards(remCards, gt, redBlack);
          ArrayList<ImagePos> imp2 = Utils.cardsToImages(remCards, cardImagesTrickBig);
          int cw2 = cardImagesTrickBig.getWidth(); // card width
          int ch2 = cardImagesTrickBig.getHeight(); // (int)(cw2 * 3) / 2;              // card height            

          int xi = 0;
          int n = remCards.size();
          for (int j=0; j < n; j++) {
            imp2.get(j).x = 0 + xi * 2 * cw2/8;
            imp2.get(j).y = h - ch2/5 - 5;
            imp.add(imp2.get(j));
            if (j+1 < n) {
              if (gt == GameDeclaration.NULL_GAME) {
                // null
                if (remCards.get(j).getSuit() != remCards.get(j+1).getSuit())
                  xi+=1; // space between suits
              } else {
                // grand / suit
                if (SimpleState.trump(remCards.get(j), gt)) {
                  if (!SimpleState.trump(remCards.get(j+1), gt))
                    xi+=1; // space after trump
                } else {
                  if (remCards.get(j).getSuit() != remCards.get(j+1).getSuit())
                    xi+=1; // space between suits
                }
              }
            }
            xi++;
          }
        }
      
      } else {

        // no card play yet

        if (state.getPhase() != SimpleState.DISCARD_AND_DECL) {
        
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

        hand = state.getHand(pi);
        playedCards = state.getPlayedCards(pi);
      
        w = handPanels[i].getUsableWidth();
        h = handPanels[i].getUsableHeight();
        ArrayList<Card> handCards;

        // collect own + played cards
      
        if (state.handKnown(pi)) {
          handCards = Hand.toCardList(hand);
        } else {
          handCards = new ArrayList<Card>();
          for (int j=0; j < state.numCards(pi); j++) {
            handCards.add(Card.newCard(-1, -1)); // unknown cards
          }
        }

        ArrayList<Card> played = Hand.toCardList(playedCards);

        if (noob) {
          for (Card c : played) {
            handCards.add(c);
          }
        }

        if (pi == state.getDeclarer() &&
            /*mode == DisplayMode.PLAYING && */
            state.getPhase() == SimpleState.DISCARD_AND_DECL) {

          // add cards to be discarded
          handCards.add(state.getSkat0());
          handCards.add(state.getSkat1());

          if (action == TablePanelUpdateAction.MOVE) {
            discardCards[0] = state.getSkat0();
            discardCards[1] = state.getSkat1();
          }
        }
        
        Utils.sortCards(handCards, gameType, redBlack);	
        imp = Utils.cardsToImages(handCards, cardImages[i]);
        Utils.arrangeImagesStraight(imp, w, yoffset[i], handCards.size() >= 10 ? handCards.size() : 10, false);

        // lower all cards + shift right in noob mode
        for (ImagePos ip : imp) {
          ip.y += dy;
        }
      
        handPanels[i].setImages(imp);

        int n = handCards.size();

        for (int k=0; k < n; k++) {
          for (Card c : played) {
            if (handCards.get(k).equals(c)) {
              // lower played cards further
              imp.get(k).y = 7*h/10;
              break;
            }
          }

          if (/*mode == DisplayMode.PLAYING &&*/
              state.getPhase() == SimpleState.DISCARD_AND_DECL &&
              Math.max(myIndex,0) == state.getDeclarer() &&
              (handCards.get(k).equals(discardCards[0]) ||
               handCards.get(k).equals(discardCards[1]))) {

            // raise cards to be discarded
            imp.get(k).y -= dy;
          }
        }

        int trick_i = state.getTrickNum();
        if (trick_i >= 0) {

          // raise card played in this trick
          // show hands prior to trick

          Card[] trick = new Card[3];
        
          // collect trick cards
          for (int k=0; k < 3; k++) {
            int j = (trick_i)*3 + k + 4;
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
    
      setHandTitles(state, myIndex);

      int phase = state.getPhase();
      
      {
        // trick panel

        String title = rbs("History");

        if (!noob && !game.isFinished()) {
          if (state.getPhase() == SimpleState.CARDPLAY)
            title = rbs("Previous_and_current_trick");
          else
            title = "Skat";
        }
        
        TitledBorder tb = BorderFactory.createTitledBorder(null, title,
                                                           javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                                                           javax.swing.border.TitledBorder.DEFAULT_POSITION,
                                                           new java.awt.Font("Dialog", 1, 14)); // NOI18N
        tb.setTitleColor(!game.isFinished() && phase == SimpleState.SKAT_OR_HAND_DECL 
                         ? toMoveColor : defaultForegroundColor);
        trickCardPanel.setBorder(tb);
        trickCardPanel.setBorder(tb);
      }

      {
        // declaration panel
        TitledBorder tb = BorderFactory.createTitledBorder(rbs("Contract"));
        tb.setTitleColor(!game.isFinished() &&
                         (phase == SimpleState.SKAT_OR_HAND_DECL ||
                          phase == SimpleState.DISCARD_AND_DECL)
                         ? toMoveColor : defaultForegroundColor);
        //declarationPanel.setBorder(tb);
      }

      {
        // bidding panel

        if (true /*mode != DisplayMode.HISTORY*/) {

          String title = rbs("Bidding");
        
          if (phase == SimpleState.BID) {

            // BID
          
            if (state.getAsked() == SimpleState.WORLD_VIEW) { // MH,RH passed
            
              if (meToMove)
                title = rbs("Your_bid?");
              else
                title = String.format(rbs("someones_bid?"), posNames[state.getBidder()]);
            
            } else {

              if (meToMove) {
                title = String.format(rbs("bid_you_to_x?"), posNames[state.getAsked()]);
              } else if (state.getAsked() == myIndex) {
                title = String.format(rbs("bid_x_to_you?"), posNames[state.getBidder()]);
              } else {
                title = String.format(rbs("bid_x_to_y?"), posNames[state.getBidder()], posNames[state.getAsked()]);
              }
            }

          } else if (phase == SimpleState.ANSWER) {

            // ASKED

            if (meToMove) {
              title = String.format(rbs("asked_you_by_x"), posNames[state.getBidder()], state.getMaxBid());
            } else if (state.getBidder() == myIndex) {
              title = String.format(rbs("asked_x_by_you"), posNames[state.getAsked()], state.getMaxBid());
            } else {
              title = String.format(rbs("asked_x_by_y"), posNames[state.getAsked()], posNames[state.getBidder()], state.getMaxBid());
            }
          }
        
          TitledBorder tb = BorderFactory.createTitledBorder(title);
          tb.setTitleColor(!game.isFinished() && (phase == SimpleState.BID ||
                                                  phase == SimpleState.ANSWER) ?
                           toMoveColor : defaultForegroundColor);
          //biddingPanel.setBorder(tb);

        }
      }
    }

    repaint();
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        gameTypeButtonGroup1 = new javax.swing.ButtonGroup();
        dealButtonGroup = new javax.swing.ButtonGroup();
        gameTypeButtonGroup2 = new javax.swing.ButtonGroup();
        GamePanel = new javax.swing.JPanel();
        cardPanel = new javax.swing.JPanel();
        cardPanel2 = new common.CardPanel();
        suitPanel2 = new javax.swing.JPanel();
        clubsLabel2 = new javax.swing.JLabel();
        heartsLabel2 = new javax.swing.JLabel();
        spadesLabel2 = new javax.swing.JLabel();
        diamondsLabel2 = new javax.swing.JLabel();
        jacksLabel2 = new javax.swing.JLabel();
        textField2A = new javax.swing.JFormattedTextField();
        textField2B = new javax.swing.JFormattedTextField();
        cardPanel1 = new common.CardPanel();
        suitPanel1 = new javax.swing.JPanel();
        heartsLabel1 = new javax.swing.JLabel();
        diamondsLabel1 = new javax.swing.JLabel();
        spadesLabel1 = new javax.swing.JLabel();
        jacksLabel1 = new javax.swing.JLabel();
        clubsLabel1 = new javax.swing.JLabel();
        textField1A = new javax.swing.JFormattedTextField();
        textField1B = new javax.swing.JFormattedTextField();
        trickCardPanel = new common.CardPanel();
        cardPanel0 = new common.CardPanel();
        suitPanel0 = new javax.swing.JPanel();
        jacksLabel0 = new javax.swing.JLabel();
        diamondsLabel0 = new javax.swing.JLabel();
        heartsLabel0 = new javax.swing.JLabel();
        spadesLabel0 = new javax.swing.JLabel();
        clubsLabel0 = new javax.swing.JLabel();
        textField0A = new javax.swing.JFormattedTextField();
        textField0B = new javax.swing.JFormattedTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        propTable = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        actionTabbedPane = new javax.swing.JTabbedPane();
        dealFH = new javax.swing.JPanel();
        doneDealButton = new javax.swing.JButton();
        clearAllButton = new javax.swing.JButton();
        shuffleButton = new javax.swing.JButton();
        dealTabbedPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        countFHLabel = new javax.swing.JLabel();
        clearFHButton = new javax.swing.JButton();
        dealFHCardPanel = new gui.ChooseCardPanel();
        jPanel3 = new javax.swing.JPanel();
        dealMHCardPanel = new gui.ChooseCardPanel();
        countMHLabel = new javax.swing.JLabel();
        clearMHButton = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        dealRHCardPanel = new gui.ChooseCardPanel();
        countRHLabel = new javax.swing.JLabel();
        clearRHButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        dealSKCardPanel = new gui.ChooseCardPanel();
        countSKLabel = new javax.swing.JLabel();
        clearSKButton = new javax.swing.JButton();
        bidPanel = new javax.swing.JPanel();
        bidButton1 = new javax.swing.JButton();
        bidButton2 = new javax.swing.JButton();
        bidLabel = new javax.swing.JLabel();
        fhSpinner = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        mhSpinner = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        rhSpinner = new javax.swing.JSpinner();
        bidSetButton = new javax.swing.JButton();
        pickupHandPanel = new javax.swing.JPanel();
        pickupButton = new javax.swing.JButton();
        handButton = new javax.swing.JButton();
        grandButton1 = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        clubsButton1 = new javax.swing.JRadioButton();
        heartsButton1 = new javax.swing.JRadioButton();
        spadesButton1 = new javax.swing.JRadioButton();
        diamondsButton1 = new javax.swing.JRadioButton();
        clubsLabel3 = new javax.swing.JLabel();
        heartsLabel3 = new javax.swing.JLabel();
        spadesLabel3 = new javax.swing.JLabel();
        diamondsLabel3 = new javax.swing.JLabel();
        nullButton1 = new javax.swing.JRadioButton();
        ouvertCheckBox1 = new javax.swing.JCheckBox();
        schneiderCheckBox1 = new javax.swing.JCheckBox();
        schwarzCheckBox1 = new javax.swing.JCheckBox();
        pickupPanel = new javax.swing.JPanel();
        pickupCardPanel = new gui.ChooseCardPanel();
        jButton1 = new javax.swing.JButton();
        contractPanel = new javax.swing.JPanel();
        declareButton = new javax.swing.JButton();
        discardCardPanel = new gui.ChooseCardPanel();
        grandButton2 = new javax.swing.JRadioButton();
        jPanel9 = new javax.swing.JPanel();
        clubsButton2 = new javax.swing.JRadioButton();
        heartsButton2 = new javax.swing.JRadioButton();
        spadesButton2 = new javax.swing.JRadioButton();
        diamondsButton2 = new javax.swing.JRadioButton();
        clubsLabel4 = new javax.swing.JLabel();
        heartsLabel4 = new javax.swing.JLabel();
        spadesLabel4 = new javax.swing.JLabel();
        diamondsLabel4 = new javax.swing.JLabel();
        nullButton2 = new javax.swing.JRadioButton();
        nullOuvertButton2 = new javax.swing.JRadioButton();
        cardPlayPanel = new javax.swing.JPanel();
        showButton = new javax.swing.JButton();
        defResignButton = new javax.swing.JButton();
        playCardPanel = new gui.ChooseCardPanel();
        gameInfoLabel = new javax.swing.JLabel();
        declResignButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        moveTable = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        pppButton = new javax.swing.JButton();
        ppButton = new javax.swing.JButton();
        pButton = new javax.swing.JButton();
        nButton = new javax.swing.JButton();
        nnButton = new javax.swing.JButton();
        nnnButton = new javax.swing.JButton();
        gameClearButton = new javax.swing.JButton();
        viewCheckBox = new javax.swing.JCheckBox();
        replayComboBox = new javax.swing.JComboBox();
        noobCheckBox = new javax.swing.JCheckBox();
        printButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        GamePanel.setMaximumSize(new java.awt.Dimension(32000, 32000));
        GamePanel.setPreferredSize(new java.awt.Dimension(100, 100));

        cardPanel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        cardPanel2.setMinimumSize(new java.awt.Dimension(100, 20));
        cardPanel2.setPreferredSize(new java.awt.Dimension(360, 0));
        cardPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        suitPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        clubsLabel2.setFont(new java.awt.Font("Dialog", 1, 10));
        clubsLabel2.setText("XXX");
        suitPanel2.add(clubsLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, -1, 20));

        heartsLabel2.setFont(new java.awt.Font("Dialog", 1, 10));
        heartsLabel2.setText("XXX");
        suitPanel2.add(heartsLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 15, -1, 20));

        spadesLabel2.setFont(new java.awt.Font("Dialog", 1, 10));
        spadesLabel2.setText("XXX");
        suitPanel2.add(spadesLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 0, -1, 20));

        diamondsLabel2.setFont(new java.awt.Font("Dialog", 1, 10));
        diamondsLabel2.setText("XXX");
        suitPanel2.add(diamondsLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 15, -1, 20));

        jacksLabel2.setFont(new java.awt.Font("Dialog", 1, 10));
        jacksLabel2.setText("XXX");
        suitPanel2.add(jacksLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 15, -1, 20));

        cardPanel2.add(suitPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 5, 100, -1));

        textField2A.setEditable(false);
        textField2A.setFont(new java.awt.Font("Dialog", 1, 14));
        cardPanel2.add(textField2A, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2, 268, -1));

        textField2B.setEditable(false);
        textField2B.setFont(new java.awt.Font("Dialog", 1, 14));
        cardPanel2.add(textField2B, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 22, 268, -1));

        cardPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        cardPanel1.setMinimumSize(new java.awt.Dimension(100, 20));
        cardPanel1.setPreferredSize(new java.awt.Dimension(360, 0));
        cardPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        suitPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        heartsLabel1.setFont(new java.awt.Font("Dialog", 1, 10));
        heartsLabel1.setText("XXX");
        suitPanel1.add(heartsLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

        diamondsLabel1.setFont(new java.awt.Font("Dialog", 1, 10));
        diamondsLabel1.setText("XXX");
        suitPanel1.add(diamondsLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 20, -1, -1));

        spadesLabel1.setFont(new java.awt.Font("Dialog", 1, 10));
        spadesLabel1.setText("XXX");
        suitPanel1.add(spadesLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 4, -1, -1));

        jacksLabel1.setFont(new java.awt.Font("Dialog", 1, 10));
        jacksLabel1.setText("XXX");
        suitPanel1.add(jacksLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, -1, -1));

        clubsLabel1.setFont(new java.awt.Font("Dialog", 1, 10));
        clubsLabel1.setText("XXX");
        suitPanel1.add(clubsLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 4, -1, -1));

        cardPanel1.add(suitPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(270, 5, 100, -1));

        textField1A.setEditable(false);
        textField1A.setFont(new java.awt.Font("Dialog", 1, 14));
        cardPanel1.add(textField1A, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2, 268, -1));

        textField1B.setEditable(false);
        textField1B.setFont(new java.awt.Font("Dialog", 1, 14));
        cardPanel1.add(textField1B, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 22, 268, -1));

        trickCardPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "skat/trick/history", javax.swing.border.TitledBorder.LEADING, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Dialog", 1, 14))); // NOI18N
        trickCardPanel.setMinimumSize(new java.awt.Dimension(100, 20));
        trickCardPanel.setPreferredSize(new java.awt.Dimension(100, 20));
        trickCardPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        cardPanel0.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, true));
        cardPanel0.setMinimumSize(new java.awt.Dimension(100, 20));
        cardPanel0.setPreferredSize(new java.awt.Dimension(100, 20));
        cardPanel0.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        suitPanel0.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jacksLabel0.setFont(new java.awt.Font("Dialog", 1, 10));
        jacksLabel0.setText("XXX");
        suitPanel0.add(jacksLabel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 3, -1, -1));

        diamondsLabel0.setFont(new java.awt.Font("Dialog", 1, 10));
        diamondsLabel0.setText("XXX");
        suitPanel0.add(diamondsLabel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 3, -1, -1));

        heartsLabel0.setFont(new java.awt.Font("Dialog", 1, 10));
        heartsLabel0.setText("XXX");
        suitPanel0.add(heartsLabel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 3, -1, -1));

        spadesLabel0.setFont(new java.awt.Font("Dialog", 1, 10));
        spadesLabel0.setText("XXX");
        suitPanel0.add(spadesLabel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 3, -1, -1));

        clubsLabel0.setFont(new java.awt.Font("Dialog", 1, 10));
        clubsLabel0.setText("XXX");
        suitPanel0.add(clubsLabel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 3, -1, -1));

        cardPanel0.add(suitPanel0, new org.netbeans.lib.awtextra.AbsoluteConstraints(552, 2, 160, 20));

        textField0A.setEditable(false);
        textField0A.setFont(new java.awt.Font("Dialog", 1, 14));
        cardPanel0.add(textField0A, new org.netbeans.lib.awtextra.AbsoluteConstraints(2, 2, 300, -1));

        textField0B.setEditable(false);
        textField0B.setFont(new java.awt.Font("Dialog", 1, 14));
        textField0B.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                textField0BActionPerformed(evt);
            }
        });
        cardPanel0.add(textField0B, new org.netbeans.lib.awtextra.AbsoluteConstraints(302, 2, 250, -1));

        org.jdesktop.layout.GroupLayout cardPanelLayout = new org.jdesktop.layout.GroupLayout(cardPanel);
        cardPanel.setLayout(cardPanelLayout);
        cardPanelLayout.setHorizontalGroup(
            cardPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(cardPanelLayout.createSequentialGroup()
                .add(cardPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cardPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE))
            .add(trickCardPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
            .add(cardPanel0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
        );
        cardPanelLayout.setVerticalGroup(
            cardPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(cardPanelLayout.createSequentialGroup()
                .add(cardPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(cardPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE)
                    .add(cardPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(trickCardPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(cardPanel0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 198, Short.MAX_VALUE))
        );

        propTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Key", "Value", "Key", "Value", "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, false, true, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        propTable.setRowSelectionAllowed(false);
        jScrollPane2.setViewportView(propTable);

        org.jdesktop.layout.GroupLayout GamePanelLayout = new org.jdesktop.layout.GroupLayout(GamePanel);
        GamePanel.setLayout(GamePanelLayout);
        GamePanelLayout.setHorizontalGroup(
            GamePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(cardPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
        );
        GamePanelLayout.setVerticalGroup(
            GamePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(GamePanelLayout.createSequentialGroup()
                .add(cardPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        dealFH.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        doneDealButton.setText("Done");
        doneDealButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        dealFH.add(doneDealButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 170, -1, -1));

        clearAllButton.setText("Clear All");
        clearAllButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        dealFH.add(clearAllButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 170, -1, -1));

        shuffleButton.setText("Shuffle");
        shuffleButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        dealFH.add(shuffleButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 170, -1, -1));

        dealTabbedPane.setTabPlacement(javax.swing.JTabbedPane.RIGHT);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        countFHLabel.setText("10");
        jPanel1.add(countFHLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 40, -1, -1));

        clearFHButton.setText("Clear");
        clearFHButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel1.add(clearFHButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 70, -1, -1));
        jPanel1.add(dealFHCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 120, -1));

        dealTabbedPane.addTab("FH", jPanel1);

        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel3.add(dealMHCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 120, -1));

        countMHLabel.setText("10");
        jPanel3.add(countMHLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 50, -1, -1));

        clearMHButton.setText("Clear");
        clearMHButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel3.add(clearMHButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 80, -1, -1));

        dealTabbedPane.addTab("MH", jPanel3);

        jPanel4.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel4.add(dealRHCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 120, -1));

        countRHLabel.setText("10");
        jPanel4.add(countRHLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 40, -1, -1));

        clearRHButton.setText("Clear");
        clearRHButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel4.add(clearRHButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 70, -1, -1));

        dealTabbedPane.addTab("RH", jPanel4);

        jPanel6.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel6.add(dealSKCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 120, -1));

        countSKLabel.setText("10");
        jPanel6.add(countSKLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 40, -1, -1));

        clearSKButton.setText("Clear");
        clearSKButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel6.add(clearSKButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 70, -1, -1));

        dealTabbedPane.addTab("Skat", jPanel6);

        dealFH.add(dealTabbedPane, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 240, 160));

        actionTabbedPane.addTab("Deal", dealFH);

        bidPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("data/i18n/gui/TableWindow"); // NOI18N
        bidButton1.setText(bundle.getString("Yes")); // NOI18N
        bidButton1.setMargin(new java.awt.Insets(2, 5, 2, 5));
        bidPanel.add(bidButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 160, -1, -1));

        bidButton2.setText(bundle.getString("Pass")); // NOI18N
        bidButton2.setMargin(new java.awt.Insets(2, 5, 2, 5));
        bidPanel.add(bidButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 160, -1, -1));

        bidLabel.setText("Bid");
        bidPanel.add(bidLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 140, -1, -1));
        bidPanel.add(fhSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 30, -1, -1));

        jLabel1.setText("FH");
        bidPanel.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 30, -1, -1));

        jLabel2.setText("MH");
        bidPanel.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 60, -1, -1));
        bidPanel.add(mhSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 60, -1, -1));

        jLabel3.setText("RH");
        bidPanel.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 90, -1, -1));
        bidPanel.add(rhSpinner, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 90, -1, -1));

        bidSetButton.setText("Set");
        bidPanel.add(bidSetButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 60, -1, -1));

        actionTabbedPane.addTab("Bid", bidPanel);

        pickupHandPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        pickupButton.setText("Pickup Skat");
        pickupHandPanel.add(pickupButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 170, -1, -1));

        handButton.setText("Hand");
        pickupHandPanel.add(handButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 170, -1, -1));

        gameTypeButtonGroup1.add(grandButton1);
        grandButton1.setFont(new java.awt.Font("Dialog", 1, 14));
        grandButton1.setText(bundle.getString("Grand")); // NOI18N
        grandButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        grandButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        pickupHandPanel.add(grandButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        gameTypeButtonGroup1.add(clubsButton1);
        clubsButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        clubsButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel2.add(clubsButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        gameTypeButtonGroup1.add(heartsButton1);
        heartsButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        heartsButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel2.add(heartsButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 20, -1, -1));

        gameTypeButtonGroup1.add(spadesButton1);
        spadesButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        spadesButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel2.add(spadesButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, -1, -1));

        gameTypeButtonGroup1.add(diamondsButton1);
        diamondsButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        diamondsButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel2.add(diamondsButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, -1, -1));

        clubsLabel3.setText("X");
        jPanel2.add(clubsLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, -1, -1));

        heartsLabel3.setText("X");
        jPanel2.add(heartsLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        spadesLabel3.setText("X");
        jPanel2.add(spadesLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 0, -1, -1));

        diamondsLabel3.setText("X");
        jPanel2.add(diamondsLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, 20, -1));

        pickupHandPanel.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, -1, -1));

        gameTypeButtonGroup1.add(nullButton1);
        nullButton1.setFont(new java.awt.Font("Dialog", 1, 14));
        nullButton1.setText(bundle.getString("Null")); // NOI18N
        nullButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        nullButton1.setMargin(new java.awt.Insets(0, 0, 0, 0));
        pickupHandPanel.add(nullButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, -1, -1));

        ouvertCheckBox1.setFont(new java.awt.Font("Dialog", 1, 14));
        ouvertCheckBox1.setText("Ouvert");
        pickupHandPanel.add(ouvertCheckBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 90, -1, -1));

        schneiderCheckBox1.setFont(new java.awt.Font("Dialog", 1, 14));
        schneiderCheckBox1.setText("Schneider");
        pickupHandPanel.add(schneiderCheckBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 110, -1, -1));

        schwarzCheckBox1.setFont(new java.awt.Font("Dialog", 1, 14));
        schwarzCheckBox1.setText("Schwarz");
        pickupHandPanel.add(schwarzCheckBox1, new org.netbeans.lib.awtextra.AbsoluteConstraints(5, 130, -1, -1));

        actionTabbedPane.addTab("Hand/Pickup", pickupHandPanel);

        pickupPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        pickupPanel.add(pickupCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 120, -1));

        jButton1.setText("Done");
        pickupPanel.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 80, -1, -1));

        actionTabbedPane.addTab("Skat", pickupPanel);

        contractPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        declareButton.setFont(new java.awt.Font("Dialog", 1, 14));
        declareButton.setText("Discard & Declare"); // NOI18N
        declareButton.setMargin(new java.awt.Insets(2, 5, 2, 5));
        contractPanel.add(declareButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 170, -1, -1));
        contractPanel.add(discardCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 10, 120, -1));

        gameTypeButtonGroup2.add(grandButton2);
        grandButton2.setFont(new java.awt.Font("Dialog", 1, 14));
        grandButton2.setText(bundle.getString("Grand")); // NOI18N
        grandButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        grandButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        contractPanel.add(grandButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        jPanel9.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        gameTypeButtonGroup2.add(clubsButton2);
        clubsButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        clubsButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel9.add(clubsButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        gameTypeButtonGroup2.add(heartsButton2);
        heartsButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        heartsButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel9.add(heartsButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 20, -1, -1));

        gameTypeButtonGroup2.add(spadesButton2);
        spadesButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        spadesButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel9.add(spadesButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 0, -1, -1));

        gameTypeButtonGroup2.add(diamondsButton2);
        diamondsButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        diamondsButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jPanel9.add(diamondsButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, -1, -1));

        clubsLabel4.setText("X");
        jPanel9.add(clubsLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 0, -1, -1));

        heartsLabel4.setText("X");
        jPanel9.add(heartsLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 20, -1, -1));

        spadesLabel4.setText("X");
        jPanel9.add(spadesLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 0, -1, -1));

        diamondsLabel4.setText("X");
        jPanel9.add(diamondsLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 20, 20, -1));

        contractPanel.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, -1, -1));

        gameTypeButtonGroup2.add(nullButton2);
        nullButton2.setFont(new java.awt.Font("Dialog", 1, 14));
        nullButton2.setText(bundle.getString("Null")); // NOI18N
        nullButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        nullButton2.setMargin(new java.awt.Insets(0, 0, 0, 0));
        contractPanel.add(nullButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, -1, -1));

        gameTypeButtonGroup2.add(nullOuvertButton2);
        nullOuvertButton2.setText("Null Ouvert");
        contractPanel.add(nullOuvertButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(6, 90, -1, -1));

        actionTabbedPane.addTab("Contract", contractPanel);

        cardPlayPanel.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        showButton.setFont(new java.awt.Font("Dialog", 1, 14));
        showButton.setText("Show cards"); // NOI18N
        showButton.setToolTipText(bundle.getString("As_declarer_you_can_reveal_your_hand_")); // NOI18N
        showButton.setMargin(new java.awt.Insets(2, 0, 2, 0));
        cardPlayPanel.add(showButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 50, -1, -1));

        defResignButton.setFont(new java.awt.Font("Dialog", 1, 14));
        defResignButton.setText("Def.Res."); // NOI18N
        defResignButton.setToolTipText(bundle.getString("Remaining_tricks_to_opponent(s)_or_resign_null_game")); // NOI18N
        defResignButton.setMargin(new java.awt.Insets(2, 0, 2, 0));
        cardPlayPanel.add(defResignButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 130, -1, -1));
        cardPlayPanel.add(playCardPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 120, -1));

        gameInfoLabel.setText("TEXT");
        cardPlayPanel.add(gameInfoLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, -1, -1));

        declResignButton.setFont(new java.awt.Font("Dialog", 1, 14));
        declResignButton.setText("Decl.Res."); // NOI18N
        declResignButton.setToolTipText(bundle.getString("Remaining_tricks_to_opponent(s)_or_resign_null_game")); // NOI18N
        declResignButton.setMargin(new java.awt.Insets(2, 0, 2, 0));
        cardPlayPanel.add(declResignButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 90, -1, -1));

        actionTabbedPane.addTab("Cardplay", cardPlayPanel);

        jLabel4.setFont(new java.awt.Font("Dialog", 1, 14));
        jLabel4.setText("Game Finished");

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(67, 67, 67)
                .add(jLabel4)
                .addContainerGap(67, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(85, 85, 85)
                .add(jLabel4)
                .addContainerGap(104, Short.MAX_VALUE))
        );

        actionTabbedPane.addTab("X", jPanel5);

        moveTable.setAutoCreateRowSorter(true);
        moveTable.setFont(new java.awt.Font("Liberation Sans", 0, 14));
        moveTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3"
            }
        ));
        moveTable.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(moveTable);

        jPanel8.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        pppButton.setText("|<");
        pppButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(pppButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        ppButton.setText("<<");
        ppButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(ppButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 0, 30, -1));

        pButton.setText("<");
        pButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(pButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 0, -1, -1));

        nButton.setText(">");
        nButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(nButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 0, -1, -1));

        nnButton.setText(">>");
        nnButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(nnButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(130, 0, -1, -1));

        nnnButton.setText(">|");
        nnnButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(nnnButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 0, -1, -1));

        gameClearButton.setText("Clear");
        gameClearButton.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jPanel8.add(gameClearButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 0, -1, -1));

        viewCheckBox.setText("View as");
        jPanel8.add(viewCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 40, -1, -1));

        replayComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Soloist", "R. Def.", "L. Def.", "God", "Kibitz", "FH", "MH", "RH" }));
        jPanel8.add(replayComboBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 40, -1, -1));

        noobCheckBox.setText("Noob");
        jPanel8.add(noobCheckBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 40, -1, -1));

        printButton.setText("Print");
        jPanel8.add(printButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 70, -1, -1));

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, actionTabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel7Layout.createSequentialGroup()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 97, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(actionTabbedPane, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 251, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(GamePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(GamePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 692, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void textField0BActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_textField0BActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_textField0BActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel GamePanel;
    private javax.swing.JTabbedPane actionTabbedPane;
    private javax.swing.JButton bidButton1;
    private javax.swing.JButton bidButton2;
    private javax.swing.JLabel bidLabel;
    private javax.swing.JPanel bidPanel;
    private javax.swing.JButton bidSetButton;
    private javax.swing.JPanel cardPanel;
    private common.CardPanel cardPanel0;
    private common.CardPanel cardPanel1;
    private common.CardPanel cardPanel2;
    private javax.swing.JPanel cardPlayPanel;
    private javax.swing.JButton clearAllButton;
    private javax.swing.JButton clearFHButton;
    private javax.swing.JButton clearMHButton;
    private javax.swing.JButton clearRHButton;
    private javax.swing.JButton clearSKButton;
    private javax.swing.JRadioButton clubsButton1;
    private javax.swing.JRadioButton clubsButton2;
    private javax.swing.JLabel clubsLabel0;
    private javax.swing.JLabel clubsLabel1;
    private javax.swing.JLabel clubsLabel2;
    private javax.swing.JLabel clubsLabel3;
    private javax.swing.JLabel clubsLabel4;
    private javax.swing.JPanel contractPanel;
    private javax.swing.JLabel countFHLabel;
    private javax.swing.JLabel countMHLabel;
    private javax.swing.JLabel countRHLabel;
    private javax.swing.JLabel countSKLabel;
    private javax.swing.ButtonGroup dealButtonGroup;
    private javax.swing.JPanel dealFH;
    private gui.ChooseCardPanel dealFHCardPanel;
    private gui.ChooseCardPanel dealMHCardPanel;
    private gui.ChooseCardPanel dealRHCardPanel;
    private gui.ChooseCardPanel dealSKCardPanel;
    private javax.swing.JTabbedPane dealTabbedPane;
    private javax.swing.JButton declResignButton;
    private javax.swing.JButton declareButton;
    private javax.swing.JButton defResignButton;
    private javax.swing.JRadioButton diamondsButton1;
    private javax.swing.JRadioButton diamondsButton2;
    private javax.swing.JLabel diamondsLabel0;
    private javax.swing.JLabel diamondsLabel1;
    private javax.swing.JLabel diamondsLabel2;
    private javax.swing.JLabel diamondsLabel3;
    private javax.swing.JLabel diamondsLabel4;
    private gui.ChooseCardPanel discardCardPanel;
    private javax.swing.JButton doneDealButton;
    private javax.swing.JSpinner fhSpinner;
    private javax.swing.JButton gameClearButton;
    private javax.swing.JLabel gameInfoLabel;
    private javax.swing.ButtonGroup gameTypeButtonGroup1;
    private javax.swing.ButtonGroup gameTypeButtonGroup2;
    private javax.swing.JRadioButton grandButton1;
    private javax.swing.JRadioButton grandButton2;
    private javax.swing.JButton handButton;
    private javax.swing.JRadioButton heartsButton1;
    private javax.swing.JRadioButton heartsButton2;
    private javax.swing.JLabel heartsLabel0;
    private javax.swing.JLabel heartsLabel1;
    private javax.swing.JLabel heartsLabel2;
    private javax.swing.JLabel heartsLabel3;
    private javax.swing.JLabel heartsLabel4;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel jacksLabel0;
    private javax.swing.JLabel jacksLabel1;
    private javax.swing.JLabel jacksLabel2;
    private javax.swing.JSpinner mhSpinner;
    private javax.swing.JTable moveTable;
    private javax.swing.JButton nButton;
    private javax.swing.JButton nnButton;
    private javax.swing.JButton nnnButton;
    private javax.swing.JCheckBox noobCheckBox;
    private javax.swing.JRadioButton nullButton1;
    private javax.swing.JRadioButton nullButton2;
    private javax.swing.JRadioButton nullOuvertButton2;
    private javax.swing.JCheckBox ouvertCheckBox1;
    private javax.swing.JButton pButton;
    private javax.swing.JButton pickupButton;
    private gui.ChooseCardPanel pickupCardPanel;
    private javax.swing.JPanel pickupHandPanel;
    private javax.swing.JPanel pickupPanel;
    private gui.ChooseCardPanel playCardPanel;
    private javax.swing.JButton ppButton;
    private javax.swing.JButton pppButton;
    private javax.swing.JButton printButton;
    private javax.swing.JTable propTable;
    private javax.swing.JComboBox replayComboBox;
    private javax.swing.JSpinner rhSpinner;
    private javax.swing.JCheckBox schneiderCheckBox1;
    private javax.swing.JCheckBox schwarzCheckBox1;
    private javax.swing.JButton showButton;
    private javax.swing.JButton shuffleButton;
    private javax.swing.JRadioButton spadesButton1;
    private javax.swing.JRadioButton spadesButton2;
    private javax.swing.JLabel spadesLabel0;
    private javax.swing.JLabel spadesLabel1;
    private javax.swing.JLabel spadesLabel2;
    private javax.swing.JLabel spadesLabel3;
    private javax.swing.JLabel spadesLabel4;
    private javax.swing.JPanel suitPanel0;
    private javax.swing.JPanel suitPanel1;
    private javax.swing.JPanel suitPanel2;
    private javax.swing.JFormattedTextField textField0A;
    private javax.swing.JFormattedTextField textField0B;
    private javax.swing.JFormattedTextField textField1A;
    private javax.swing.JFormattedTextField textField1B;
    private javax.swing.JFormattedTextField textField2A;
    private javax.swing.JFormattedTextField textField2B;
    private common.CardPanel trickCardPanel;
    private javax.swing.JCheckBox viewCheckBox;
    // End of variables declaration//GEN-END:variables

  EditWindow getThis() { return this; }

  public void run()
  {
    while (true) {
      Misc.sleep(1000);
    }
  }

  final String[] playerPos = { "FH", "MH", "RH" };
}
