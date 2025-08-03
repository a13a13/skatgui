/*
 * (c) Michael Buro, licensed under GPLv3
 *
 */

package gui;

import java.util.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import java.util.Properties;
import java.io.*;
import java.net.*;
import java.beans.*;
import javax.swing.table.*;

import net.miginfocom.layout.PlatformDefaults;
import net.miginfocom.layout.UnitValue;
import net.miginfocom.swing.MigLayout;

import common.*;
import client.*;

class LogoCanvas extends Canvas
{
  Image logo;
  MediaTracker tracker = new MediaTracker(new Canvas());
  final int width = 500;
  
  public LogoCanvas()
  {
    String file = "/data/images/logo.gif";
    URL url = Thread.class.getResource(file);
    if (url == null) { Misc.err("can't access logo"); }
	
    logo = Toolkit.getDefaultToolkit().getImage(url).
      getScaledInstance(width, -1, Image.SCALE_SMOOTH);
    tracker.addImage(logo, 2);

    try { tracker.waitForID(2); }
    catch (InterruptedException e) { }

    setSize(width+2, (int)(width*0.3403426)+2);
  }

  @Override
  public void paint(Graphics g) //Graphics actualG)
  {
    // Image image = createImage(getWidth() + 1, getHeight() + 1);
    // Graphics g = image.getGraphics();

    super.paint(g);

    if (logo != null) {
      g.drawImage(logo, 0, 0, this);
    }

    //paintChildren(g);
  }
}


public class ClientWindow
  extends ResizeFrame
  implements ServiceClient.ServiceMsgHandler, WindowListener
{
  static int VERSION = 16; // increase with major steps (change forces users to update)

  static final int SETUP_INDEX = 0;  // main tabbed panes
  static final int TOURNAMENT_INDEX = 1;
  static final boolean ENABLE_TOUR_TAB = false;  // false in testing period for public release
  static final int MAIN_INDEX = 2;
  int WINDOW_WIDTH;   // aspect ratio is static
  int WINDOW_HEIGHT;

  final int[] ports = { 7000, 8000, 80 };
  final String[] portStrings = { "Port 7000", "Port 8000", "Port 80" };
  final String[] hosts = { "skatgame.net", "skat.dnsalias.net", "localhost", "192.168.2.1" };
  
  String host;
  int port;
  String id;
  String password;
  String cmds;
  boolean additionalData;
  boolean tournamentMode;
  final ServerClient sc;
  javax.swing.Timer timer;
  boolean gfxLogin;
  boolean exitOnDisconnect;
  java.util.ResourceBundle bundle;

  Vector<TablePanelBase> tablePanels = new Vector<TablePanelBase>();
  Vector<TablePanelWindow> tableWindows = new Vector<TablePanelWindow>();
  Vector<TalkContext> talkContexts = new Vector<TalkContext>();

  String langStr; // language used en,pl,de right now
  HTMLResultTable htmlResults;

  final String cardDeckPath = "/data/cards";
  final String[] cardDecks = new String[] { "E2", "E4", "G2", "G4", "GG" };
  
  class TablePanelWindow extends ResizeFrame implements WindowListener

  {
    public Table table;

    TablePanelWindow(TablePanelBase p, Table table)
    {
      this.table = table;
      setContentPane(p);
      setResizable(false);
      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      addWindowListener(this);
    }

    public void windowClosing(WindowEvent e) {
      synchronized (getThis().sc) {
	  for (TablePanelWindow tw : tableWindows) {
	      htmlResults = new HTMLResultTable(tw.table);
	      // if (!tw.table.getTourny() || tw.table.isFinished()) // can't save table archive file for non-tourney table
	      /*
	      if (tw.table.isFinished())
		  tw.table.saveTableHistory();
	      */
	  }

        if (!table.getInProgress() || table.playerInGameIndex(table.getViewerName()) < 0) {
          ((TablePanelBase)getContentPane()).send("leave");
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

    public void resize(float f)
    {
      // Misc.msg("RESIZE " + (int)(WINDOW_WIDTH*f) + " " + (int)(WINDOW_HEIGHT*f));
      setSize((int)(WINDOW_WIDTH*f), (int)(WINDOW_HEIGHT*f));

      invalidate();
      validate();
      ((TablePanelBase)getContentPane()).resize(f);
      repaint();
    }
  }
  
  // table models
  TableTableModel tableTableModel;
  TourTableModel  tourTableModel;
  UserTableModel  userTableModel;
  
  TreeMap<String, Integer> tableIdToPos = new TreeMap<String, Integer>();
  TreeMap<String, Integer> userIdToPos = new TreeMap<String, Integer>();  
  TreeMap<String, Integer> tourIdToPos = new TreeMap<String, Integer>();
  int tableTableNextRow;
  int userTableNextRow;
  int tourTableNextRow;
  final int TABLE_TABLE_COLUMNS = 6;
  final int USER_TABLE_COLUMNS  = 4;
  final int TOUR_TABLE_COLUMNS  = 5;    
  
  int msgTabIndex;
  Color defaultTabBackground;

  GlobalUI glob = new GlobalUI();
  
  public CardImages cardImagesSmall, cardImagesMedium, cardImagesBig;
  
  public enum TablePanelUpdateAction {
    UNKNOWN, MOVE, START, END, INFO, GUI, ERROR, TELL, TIMER
      };

  public enum ClientWindowUpdateAction {
    UNKNOWN, ADD_TO_LOBBY_TEXT, ADD_TO_TALK_TEXT, SET_ERROR, ADD_TO_ISS_TEXT,
      CREATE_TABLE_WINDOW, DESTROY_TABLE_WINDOW, INVITE,
      ADD_UPDATE_TABLE, REMOVE_TABLE,
      ADD_UPDATE_TOUR, REMOVE_TOUR,      
      ADD_UPDATE_USER, REMOVE_USER      
      };

  Settings settings;
  String settingsFile;

  public Settings getSettings() { return settings; }
  
  // return resource bundle string binding
  public String rbs(String s) { return bundle.getString(s); }

  // for testing
  public String rbs0(String s) { return s; }

  /** When a message received from the server has to be displayed, any phrase that needs
      to be internationalized begins with an underscore and all words in the phrase are
      separated by underscores -- e.g., _Already_at_5_tables warns user that he has
      already joined the maximum allowable number of tables and cannot join anymore. */
  public String translate(String str) {
    StringBuffer translated = new StringBuffer();
    /* Any phrase that needs to be internationalized will begin with an underscore and will
       be preceded by a space, unless it begins the string.  Then it will not be preceded by
       a space. */
    String symbol = " _";

    /* As mentioned above, the string may begin with a phrase that needs to be internationalized.
       In this case, it will begin with an underscore not preceded by a space.  Have to check for
       this case first. */
    if (str.startsWith("_")) {
      /* If the condition below is true, there is a space somewhere in the string, meaning that the
         string continues beyond the phrase that needs to be internationalized.  If false, the string
         consists only of the phrase that needs to be internationalized, so translate this phrase and
         then return right away. */
      if (str.indexOf(" ") > 0) {
        String str2 = str.substring(1, str.indexOf(" "));

        try {
          translated.append(rbs(str2));
          // Misc.msg(rbs(str2) + " appended -- Case 1.");
        }
        catch (Exception e) {
          translated.append(str2);
          // Misc.msg(str2 + " appended -- Case 2.");
        }
        
        str = str.substring(str.indexOf(" "));
      } else {
        String str2 = str.substring(1); // remove the underscore from the front

        try {
          translated.append(rbs(str2));
          // Misc.msg(rbs(str2) + " appended -- Case 3.");
        }
        catch (Exception e) {
          translated.append(str2);
          // Misc.msg(str2 + " appended -- Case 4.");
        }

        return translated.toString();
      }
    }

    while (str.indexOf(symbol) >= 0) {
      if (str.indexOf(symbol) != 0) {
        /* If the remaining string begins with content that does not need to be internationalized,
           append this directly. */

        translated.append(str.substring(0, str.indexOf(symbol)));
        // Misc.msg(str.substring(0, str.indexOf(symbol)) + " appended -- Case 5.");
        str = str.substring(str.indexOf(symbol));
      }

      translated.append(" ");
      str = str.substring(str.indexOf(symbol) + 2);

      /* If this condition is true, there is a space somewhere in the string, meaning that it
         continues beyond the current phrase being internationalized.  If false, the current
         phrase being internationalized is at the end of the string, so translate the phrase
         and return right away. */
      if (str.indexOf(" ") > 0) {
        String str2 = str.substring(0, str.indexOf(" "));

        try {
          translated.append(rbs(str2));
          // Misc.msg(rbs(str2) + " appended -- Case 6.");
        }
        catch (Exception e) {
          translated.append(str2);
          // Misc.msg(str2 + " appended -- Case 7.");
        }

        str = str.substring(str.indexOf(" "));
      } else {
        try {
          translated.append(rbs(str));
          // Misc.msg(rbs(str) + " appended -- Case 8.");
        }
        catch (Exception e) {
          translated.append(str);
          // Misc.msg(str + " appended -- Case 9.");
        }

        return translated.toString();
      }
    }

    // If the string ends with content that does not need to be internationalized, append directly.
    if (str.length() > 0)
      translated.append(str);

    return translated.toString();
  }

  static public String version()
  {
    return Misc.version() + "." + VERSION;
  }

  // check the skatgui version numbers and open download page if
  // mismatched
  void checkVersion()
  {
    HttpURLConnection huc = null;
    
    try  {
	URL u = new URL(Misc.issHome + rbs("/download.html"));
      huc = (HttpURLConnection) u.openConnection();

      huc.setRequestMethod("GET");
      huc.connect() ;
      InputStream is = huc.getInputStream() ;
      int code = huc.getResponseCode();
      String s = "";
      
      if (code == HttpURLConnection.HTTP_OK) {

	// read web page into string
	byte[] buffer = new byte[4096];
	int bytes = 0;
	//totBytes = huc.getContentLength();

	while (true) {
	  bytes = is.read(buffer);
	  if  (bytes <= 0)  break;
	  s += new String(buffer, 0, bytes, "ISO8859_1");
	}

	//System.out.println("s= " + s);
	Scanner scan = new Scanner(s);

	// scan for version
	while (scan.hasNext()) {
	  String t = scan.next();
	  if (t.equals("Version:")) {
	    String v = scan.next();
	    if (v != null) {
	      System.out.println("version=" + v);
	      // extract last component
	      String[] parts = v.split("\\.");
	      if (parts.length == 3) {
		if (Integer.parseInt(parts[2]) > VERSION) {
		    Misc.openURL(Misc.issHome + rbs("/download.html"));
		    JOptionPane.showMessageDialog(getThis(),
						  rbs("skatgui_available"),
						  "Information",
						  JOptionPane.INFORMATION_MESSAGE);
		    System.exit(0);
		}
	      }
	    }
	    break;	    
	  }
	}
      }
    }
    catch (IOException e) {
      Misc.msg("exception encountered in http connection: " + e.toString());
    }
    finally {
      if (huc != null) huc.disconnect();
    }
  }
  
  public void serious(Exception e)
  {
    // Has been changed so that bugs pursuant to my modifications are not sent. - Ryan
    if (!host.equals("localhost") && !id.equals("lagerya")) { // don't send report when testing
    
      sc.send("report " + Misc.encodeCRLF("skatgui version " + version() + "\n" +
					  Misc.stack2string(e)));
    }
    Misc.exception(e);
  }

  class Settings
  {
    public boolean saveLogin; // save login data (userId/password)
    public String  userId;
    public String  password;
    public int     hostInd;
    public int     portInd;
    public String  language; // empty=default, en, de, pl,fr for now
    public int     clientWinWidth;
    public int     clientWinHeight;
    public int     tableWinWidth;
    public int     tableWinHeight;
    public int     scalePerc; // 100 = default size
    public int     beep;      // 0: no, 1: timeout warning
    public int     asWindow;
    public String  cardDeck;
    
    void init()
    {
      saveLogin = true;
      userId = "";
      password = "";
      hostInd = 0;
      portInd = 0;
      language = "";
      
      clientWinWidth  = 700;
      clientWinHeight = 500;
      tableWinWidth  = 1000;
      tableWinHeight = 750;
      scalePerc = 100;
      beep = 1;
      asWindow = 1;

      cardDeck = "E4";
    }

    public Settings()
    {
      init();
    }
    
    public String read(String filename) {

      init();
      
      try {
          
	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filename));
	Properties p = new Properties();
	p.load(bis);
	bis.close();

	saveLogin = p.getProperty("saveLogin").equals("true");
	userId = p.getProperty("userId");
	password = p.getProperty("password");
        hostInd = 0;
        String s = p.getProperty("hostInd");
        if (s != null) 
          hostInd = Integer.parseInt(s);
        if (hostInd < 0 || hostInd >= hosts.length) hostInd = 0;
        portInd = 0;
        s = p.getProperty("portInd");
        if (s != null)
          portInd = Integer.parseInt(s);
        if (portInd < 0 || portInd >= ports.length) portInd = 0;        
	language = p.getProperty("language");
	clientWinWidth = Integer.parseInt(p.getProperty("clientWinWidth"));
	clientWinHeight = Integer.parseInt(p.getProperty("clientWinHeight"));
	tableWinWidth = Integer.parseInt(p.getProperty("tableWinWidth"));
	tableWinHeight = Integer.parseInt(p.getProperty("tableWinHeight"));
	scalePerc = Integer.parseInt(p.getProperty("scalePerc"));
        asWindow = Integer.parseInt(p.getProperty("asWindow"));
        beep = 1;
        s = p.getProperty("beep");
        if (s != null)
          beep = Integer.parseInt(s);
        cardDeck = p.getProperty("cardDeck");
        if (cardDeck == null || cardDeck.equals("")) cardDeck = "E4";
      }
      catch (Exception e) {
	return "can't read settings from file " + filename + " : " + e;
      }
      
      return null;
    }

    public String write(String filename) {

      try {
	BufferedOutputStream bis = new BufferedOutputStream(new FileOutputStream(filename));
	Properties p = new Properties();
	p.setProperty("saveLogin", saveLogin+"");
	p.setProperty("userId", userId);
	p.setProperty("password", password);
	p.setProperty("hostInd", hostInd+"");        
	p.setProperty("portInd", portInd+"");
	p.setProperty("language", language);
	p.setProperty("clientWinWidth",  clientWinWidth+"");
	p.setProperty("clientWinHeight", clientWinHeight+"");
	p.setProperty("tableWinWidth",   tableWinWidth+"");
	p.setProperty("tableWinHeight",  tableWinHeight+"");
	p.setProperty("scalePerc",  scalePerc+"");
	p.setProperty("asWindow",  asWindow+"");
	p.setProperty("beep",  beep+"");        
        p.setProperty("cardDeck",  cardDeck+"");        

	p.store(bis, "skatgui user settings");
	bis.close();
      }
      catch (Exception e) {
	return String.format(rbs("Cant_write_settings_to_file") +
			     " : " + e, filename); // "can't write settings to file "
      }

      return null;
    }
  }

  class TourTableModel extends javax.swing.table.DefaultTableModel
  {
    public TourTableModel()
    {
      super(tourTableHeaders, 0);
    }

    public Class getColumnClass(int columnIndex) {
      return String.class;
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return false;
    }
  }


  class TableTableModel extends javax.swing.table.DefaultTableModel
  {
    public TableTableModel()
    {
      super(tableTableHeaders, 0);
    }

    public Class getColumnClass(int columnIndex) {
      return String.class;
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return false;
    }
  }

  class UserTableModel extends javax.swing.table.DefaultTableModel
  {
    public UserTableModel()
    {
      super(userTableHeaders, 0);
    }

    public Class getColumnClass(int columnIndex) {
      return String.class;
    }
    
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return false;
    }
  }


  class TalkContext implements MsgHandler
  {
    public String clientId;
    public JLimitedTextArea textArea;
    public JScrollPane scrollPane;
    public ButtonTabComponent bcomp;
    public JTabbedPane pane;
    
    public TalkContext(String clientId)
    {
      this.clientId = clientId;
      textArea = new JLimitedTextArea();
      scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      textArea.setBorder(null);
      textArea.setColumns(20);
      textArea.setEditable(false);
      textArea.setRows(5);
      textArea.setLineWrap(true);

      scrollPane.setViewportView(textArea);
      messageTabbedPane.add(clientId, scrollPane);
      messageTabbedPane.setTabComponentAt(messageTabbedPane.getTabCount()-1,
					  new ButtonTabComponent(messageTabbedPane, this));
      talkContexts.add(this);
    }

    public void handleMsg(String who, String what, long sessionId)
    {
      dispose();
    }
    
    void dispose() {
      messageTabbedPane.remove(scrollPane);
      talkContexts.remove(this);
      
      JScrollPane sp = (JScrollPane)messageTabbedPane.getSelectedComponent();
      setTalkContext(sp, null, true);
    }
  }

  TalkContext setTalkContext(JScrollPane sp, String name, boolean focus)
  {
    TalkContext tc;
    
    if (sp == null) {
      tc = findTalkContext(name);
    } else
      tc = findTalkContext(sp);
    
    if (sp != lobbyPane && sp != logPane && tc == null) {
      // create new
      tc = new TalkContext(name);
    }

    if (tc != null)
      sp = tc.scrollPane;
    
    if (focus) {
    
      // set input title
      String title = "";
      if (sp == lobbyPane) {
	title = String.format(rbs("Message_to_x"), rbs("Lobby"));
      } else if (sp == logPane) {
	title = String.format(rbs("Message_to_x"), rbs("Server"));
      } else {
	title = String.format(rbs("Message_to_x"), tc.clientId);
      }

      mainInput.setBorder(javax.swing.BorderFactory.
			  createTitledBorder(javax.swing.BorderFactory.createTitledBorder(title)));
      mainInput.requestFocus();

      java.awt.Component co = messageTabbedPane.getSelectedComponent();

      messageTabbedPane.setBackgroundAt(getTabIndex((JScrollPane)co), defaultTabBackground);

      if (co != sp) {
	messageTabbedPane.setSelectedComponent(sp);
      }
    }
    
    return tc;
  }

  int getTabIndex(JScrollPane sp)
  {
    for (int i=0; i < messageTabbedPane.getTabCount(); i++) {
      if (sp == messageTabbedPane.getComponentAt(i))
	return i;
    }
    return -1;
  }
  
  ClientWindow getThis() { return this; }

  String cardDeckStringFromSelection()
  {
    if (additionalData) return "A2"; // hack
    return cardDecks[cardSelection.getSelectedIndex()];
  }
  
  void cardDeckStringToSelection(String s)
  {
    for (int i=0; i < cardDecks.length; i++) {
      if (s.equals(cardDecks[i])) { cardSelection.setSelectedIndex(i); return; }
    }

    cardSelection.setSelectedIndex(1);
    // Misc.err("illegal card string " + s);
  }
  
  void quitAction()
  {
    // remember settings
    settings.clientWinWidth  = getWidth();
    settings.clientWinHeight = getHeight();
    // settings.tableWinWidth  = 1024;
    // settings.tableWinHeight = 800;

    settings.asWindow = asWindowCheckbox.isSelected() ? 1: 0;
    settings.cardDeck = cardDeckStringFromSelection();

    settings.write(settingsFile);
    System.exit(0);    
  }      
  
  public void windowClosing(WindowEvent e) {
      for (TablePanelBase tp : tablePanels) {
	  htmlResults = new HTMLResultTable(tp.getTable());
	  // if (!tp.getTable().getTourny() || tp.getTable().isFinished()) // can't save table archive file for non-tourney table
	  /*
	  if (tp.getTable().isFinished())
	      tp.getTable().saveTableHistory();
	  */
      }
      for (TablePanelWindow tw : tableWindows) {
	  htmlResults = new HTMLResultTable(tw.table);
	  // if (!tw.table.getTourny() || tw.table.isFinished())
	  /*
	  if (tw.table.isFinished())
	      tw.table.saveTableHistory();
	  */
      }

    quitAction();
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

  // collect selected names
  public String getSelectedNames()
  {
    StringBuffer sb = new StringBuffer();
    
    int[] rowIndices = userTable.getSelectedRows();
    if (rowIndices != null) {
      for (int i=0; i < rowIndices.length; i++) {
	sb.append(" " + (String)userTable.getModel().getValueAt(rowIndices[i], 0));
      }
    }
    return sb.toString();
  }

  // collect all player names
  public String[][] getUserData()
  {
    int rn = userTable.getRowCount();

    if (rn == 0) return null;
    String[][] sa = new String[rn][4];

    for (int i=0; i < rn; i++) {
      sa[i] = new String[4];
      sa[i][0] = (String)userTable.getModel().getValueAt(i, 0); // name
      sa[i][1] = (String)userTable.getModel().getValueAt(i, 1); // lang
      sa[i][2] = (String)userTable.getModel().getValueAt(i, 3); // rating
      sa[i][3] = "0";
    }
    return sa;
  }

  public void clearNameSelection()
  {
    userTable.clearSelection();
  }

  class BeepChangeListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e) {
      boolean v = ((javax.swing.JCheckBox)e.getSource()).getSelectedObjects() != null;
      Misc.msg("beeper value changed to " + v);
      settings.beep = v ? 1 : 0;
      saveSettings();
    }
  }

  class SpinnerChangeListener implements ChangeListener
  {
    public void stateChanged(ChangeEvent e) {
      int v = (Integer)((JSpinner)e.getSource()).getValue();
      Misc.msg("spinner value changed to " + v);
      newSize(v);
    }
  }

  private void newSize(int v)
  {
    if (v < 50) {
      v = 50;
    } else if (v > 200) {
      v = 200;
    }

    sizeSpinner.setValue(new Integer(v));            
    
    if (v != settings.scalePerc) {
      settings.scalePerc = v;
      glob.resize(settings.scalePerc * 0.01f);
    }
  }

  public void sizeDelta(int delta)
  {
    newSize(settings.scalePerc + delta);
  }


  /** Creates new form ClientWindow */
  // Constructor is called from Main.java with additional parameter tournamentMode_. - Ryan
  public ClientWindow(String host_, int port_, String id_, String password_,
                      String cmds_, String settingsFile_,
                      String locale_language,
                      boolean additionalData_,
                      boolean tournamentMode_,
                      int WINDOW_WIDTH, int WINDOW_HEIGHT)
  {
    glob.add(this);
    
    host = host_;
    port = port_;
    id = id_;
    password = password_;
    cmds = cmds_;
    settingsFile = settingsFile_;
    additionalData = additionalData_;
    tournamentMode = tournamentMode_;
    this.WINDOW_WIDTH = WINDOW_WIDTH;
    this.WINDOW_HEIGHT = WINDOW_HEIGHT;    
    settings = new Settings();
    settings.read(settingsFile);

    if (!settings.language.equals("")) {
      // change default locale to whatever is stored in settings file
      Locale locale = new Locale(settings.language);
      Locale.setDefault(locale);
    }
    
    if (!locale_language.equals("")) {
      // change default locale to command line option value (overrides file setting)
      Locale locale = new Locale(locale_language);
      Locale.setDefault(locale);
    }

    langStr = Locale.getDefault().getLanguage();
    if (!langStr.equals("de") && !langStr.equals("pl") && !langStr.equals("fr"))
      langStr = "en";
    Misc.msg("langStr=" + langStr);
    
    bundle = java.util.ResourceBundle.getBundle("data/i18n/gui/ClientWindow");

    initComponents();

    defaultTabBackground = messageTabbedPane.getBackgroundAt(0);

    // Register a change listener
    mainTabbedPane.addChangeListener(new ChangeListener() {
        // This method is called whenever the selected tab changes
        public void stateChanged(ChangeEvent evt) {
          JTabbedPane pane = (JTabbedPane)evt.getSource();
          
          // Get current tab

          // no "SMW" button in tournament mode - Ryan
          if(!isTournamentMode()) {
            int sel = mainTabbedPane.getSelectedIndex();
            if (sel == MAIN_INDEX) {
              highlightTableButtons(null);
            }
          }
        }
      });
    
    userTableModel = new UserTableModel();
    userTable.setModel(userTableModel);

    // width of table columns
    //!!!    userTable.getColumnModel().getColumn(0).setPreferredWidth(130);
    //userTable.getColumnModel().getColumn(1).setPreferredWidth(50);
    //userTable.getColumnModel().getColumn(2).setPreferredWidth(50);
    //userTable.getColumnModel().getColumn(3).setPreferredWidth(50);

    DefaultTableCellRenderer userCenter = new DefaultTableCellRenderer() {
	public Component getTableCellRendererComponent(JTable table, Object value,
						       boolean isSelected, boolean hasFocus,
						       int row, int column) {
	  Component renderer =
	    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	  setHorizontalAlignment(JLabel.CENTER);
	  return renderer;
	}
      };
    
    DefaultTableCellRenderer userRight = new DefaultTableCellRenderer() {
	public Component getTableCellRendererComponent(JTable table, Object value,
						       boolean isSelected, boolean hasFocus,
						       int row, int column) {
	  Component renderer =
	    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	  setHorizontalAlignment(JLabel.RIGHT);
	  return renderer;
	}
      };
    
    userTable.getColumnModel().getColumn(1).setCellRenderer(userCenter);
    userTable.getColumnModel().getColumn(2).setCellRenderer(userCenter);
    userTable.getColumnModel().getColumn(3).setCellRenderer(userRight);

    setUserTableListener();

    // -----
    
    tourTableModel = new TourTableModel();
    tourTable.setModel(tourTableModel);

    DefaultTableCellRenderer tourCenter = new DefaultTableCellRenderer() {
	public Component getTableCellRendererComponent(JTable table, Object value,
						       boolean isSelected, boolean hasFocus,
						       int row, int column) {
	  Component renderer =
	    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	  setBackground(Color.WHITE);
          
	  if (value != null) {
	    if (((String)value).startsWith("<"))             
	      setBackground(Color.GREEN);
	    else if (((String)value).equals("#"))
	      setBackground(Color.BLACK);
	  }
          if (column == 0)
            setBackground(Color.GREEN);
          
	  setHorizontalAlignment(JLabel.CENTER);
	  return renderer;
	}
      };

    tourTable.getColumnModel().getColumn(0).setCellRenderer(tourCenter);
    tourTable.getColumnModel().getColumn(1).setCellRenderer(tourCenter);
    tourTable.getColumnModel().getColumn(2).setCellRenderer(tourCenter);
    tourTable.getColumnModel().getColumn(3).setCellRenderer(tourCenter);
    tourTable.getColumnModel().getColumn(4).setCellRenderer(tourCenter);

    // ----

    tableTableModel = new TableTableModel();
    tableTable.setModel(tableTableModel);

    // width of table columns
    //tableTable.getColumnModel().getColumn(0).setPreferredWidth(100);
    //tableTable.getColumnModel().getColumn(1).setPreferredWidth(50);
    //tableTable.getColumnModel().getColumn(2).setPreferredWidth(140);
    //tableTable.getColumnModel().getColumn(3).setPreferredWidth(140);
    //tableTable.getColumnModel().getColumn(4).setPreferredWidth(140);
    //tableTable.getColumnModel().getColumn(5).setPreferredWidth(140);


    // set cell policies
      
    DefaultTableCellRenderer tableCenter = new DefaultTableCellRenderer() {
	public Component getTableCellRendererComponent(JTable table, Object value,
						       boolean isSelected, boolean hasFocus,
						       int row, int column) {
	  Component renderer =
	    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

	  setBackground(Color.WHITE);
          
	  if (value != null) {
	    if (((String)value).startsWith("<"))             
	      setBackground(Color.GREEN);
	    else if (((String)value).equals("#"))
	      setBackground(Color.BLACK);
	  }
          if (column == 0)
            setBackground(Color.GREEN);
          
	  setHorizontalAlignment(JLabel.CENTER);
	  return renderer;
	}
      };

    tableTable.getColumnModel().getColumn(0).setCellRenderer(tableCenter);
    tableTable.getColumnModel().getColumn(1).setCellRenderer(tableCenter);
    tableTable.getColumnModel().getColumn(2).setCellRenderer(tableCenter);
    tableTable.getColumnModel().getColumn(3).setCellRenderer(tableCenter);
    tableTable.getColumnModel().getColumn(4).setCellRenderer(tableCenter);
    tableTable.getColumnModel().getColumn(5).setCellRenderer(tableCenter);

    tableTable.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent e)
	{
	  Point p = e.getPoint();
	  int row = tableTable.rowAtPoint(p);
	  int column = tableTable.columnAtPoint(p); // This is the view column!
          
	  Misc.msg("clicked " + row + " " + column);

	  if (column == 0) {
	    // observe
	    send("observe " + tableTable.getValueAt(row, 0));
	    return;
	  }

	  String s = (String)tableTable.getValueAt(row, column);
          
	  if (s.startsWith("<")) {
	    // join

	    if (((String)tableTable.getValueAt(row, 0)).startsWith(".")) {
	      // public table
	      send("join " + (String)tableTable.getValueAt(row, 0));
	      return;
	    }

	    String response = JOptionPane.
	      showInputDialog(getThis(),
			      rbs("Enter_password_for_table_space") + (String)tableTable.getValueAt(row, 0),
			      rbs("Password_Required"),
			      JOptionPane.QUESTION_MESSAGE);
	    send("join " + tableTable.getValueAt(row, 0) + " " + response);
	    return;
	  }
	}
      });
      
    skatguiLangComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] {
	  rbs("default"),
	  "English",
	  "Deutsch",
          "Polski",
          "Français"
	}));    
    
    // select language

    if (settings.language.equals(""))
      skatguiLangComboBox.setSelectedIndex(0);
    else if (settings.language.equals("en"))
      skatguiLangComboBox.setSelectedIndex(1);      
    else if (settings.language.equals("de"))
      skatguiLangComboBox.setSelectedIndex(2);      
    else if (settings.language.equals("pl"))
      skatguiLangComboBox.setSelectedIndex(3);
    else if (settings.language.equals("fr"))
      skatguiLangComboBox.setSelectedIndex(4);

    asWindowCheckbox.setSelected(settings.asWindow != 0);

    cardDeckStringToSelection(settings.cardDeck);
    
    mainTabbedPane.setSelectedComponent(mainPanel);
    mainTabbedPane.setEnabledAt(0, false);
    mainTabbedPane.setEnabledAt(1, false);
    mainTabbedPane.setEnabledAt(2, false);
    mainTabbedPane.setEnabledAt(3, false);    

    messageTabbedPane.setSelectedIndex(0);
    msgTabIndex = 0;

    // wire components

    aboutButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("about clicked");
	  Misc.openURL(Misc.issHome + "/" + rbs("skatgui-about.html"));
	}});
    
    messageTabbedPane.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent e) {
	  JScrollPane sp = (JScrollPane)messageTabbedPane.getSelectedComponent();          
	  setTalkContext(sp, null, true);
	}});

    talkButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("talk clicked");
	  // create talk context for selected users
	  int[] rowIndices = userTable.getSelectedRows();
	  if (rowIndices != null && rowIndices.length > 0) {
	    for (int i=0; i < rowIndices.length; i++) {
	      setTalkContext(null, (String)userTable.getModel().getValueAt(rowIndices[i], 0), true);
	      break;
	    }
	    userTable.clearSelection();
            
	  } else {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("empty_talk_selection"),
					  rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
	    return;
	  }
	}});

    userInfoButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("userInfo clicked");
	  // send finger command for selected users
	  int[] rowIndices = userTable.getSelectedRows();
	  if (rowIndices != null && rowIndices.length > 0) {
	    for (int i=0; i < rowIndices.length; i++) {
	      send("finger " + userTable.getModel().getValueAt(rowIndices[i], 0));
	    }
	  } else {
	    send("finger");
	  }
	  setTalkContext(logPane, "", true); // ISS
	  userTable.clearSelection();
	}});

    helpButton.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent evt) {
          Misc.msg("help clicked");
          Misc.openURL(Misc.issHome + "/" + rbs("skatgui-main.html"));
	}});

    showTableButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("show table window clicked");

          // show table window
          
          // pick play window
          for (TablePanelWindow tw : tableWindows) {
            if (!tw.table.getViewerName().equals(Table.NO_NAME)) {
              tw.toFront();
              return;
            }
          }

          // pick play panel
          for (TablePanelBase tp : tablePanels) {

            if (tp.getTable().getViewerName().equals(Table.NO_NAME)) {
              mainTabbedPane.setSelectedComponent(tp);
              return;
            }
          }

          // otherwise, pick observe window
          for (TablePanelWindow tw : tableWindows) {
            tw.toFront(); // otherwise, pick first
            return;
          }

          // pick play panel
          for (TablePanelBase tp : tablePanels) {
            mainTabbedPane.setSelectedComponent(tp);
            return;
          }
        }});

    ratingsButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("ratings clicked");
	  // show ratings in browser window
	  String s = Misc.openURL(Misc.issHome + "/ratings.html");
          if (s != null) Misc.msg("!!! " + s);
	}});

    quitButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("quit clicked");
	  quitAction();
	}});

    passwordInput.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent e) {
	  doLogin();
	}
      });
    
    loginButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("login clicked");
	  doLogin();
	}});

    // main input: send to server & copy to text area
    mainInput.addActionListener(new ActionListener()
      {
	public void actionPerformed(ActionEvent e) {

	  try {

	    if (mainInput.getText().equals("!CRASH!"))
	      throw new Exception("crash forced by user");
                
	    java.awt.Component co = messageTabbedPane.getSelectedComponent();
	    
	    if (co == lobbyPane) {
	      send("yell " + mainInput.getText());            
	    } else if (co == logPane) {
	      send(mainInput.getText());
	    } else {
	      TalkContext tc = findTalkContext(co);
	      if (tc == null) return;

	      TreeMap<String, ClientData> id2client = sc.getServiceClient().getClients();
	      if (id2client.get(tc.clientId) == null) {
		  tc.textArea.append("\n" + tc.clientId + " " + rbs("not_available") + " >>>");
	      } else {
		tc.textArea.append("\n" + id + ": " + mainInput.getText());
		send("tell " + tc.clientId + " " + mainInput.getText());
	      }
	      tc.textArea.setCaretPosition(tc.textArea.getDocument().getLength());
	    }
	    
	    mainInput.setText("");
	    mainInput.setCaretPosition(mainInput.getDocument().getLength());
	    mainInput.requestFocus();
	  }
	  catch (Exception f) { serious(f); }
	}
      });

    /* If in tournament mode, there will be buttons to create either specifically a 3-player
       table or specifically a 4-player one.  Number of players allowed to sit at a table
       cannot be changed in this mode. - Ryan */
    if(!isTournamentMode()) {
      createButton.addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent evt) {
            try {
              boolean pub = tableNameInput.getText().equals("."+rbs("public"));
              
              Misc.msg("create " + pub);
              
              if (!tableNameInput.getText().isEmpty() && !pub) {
                
                String r = Misc.validId(tableNameInput.getText());
                if (r != null) {
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("table_name_error") + ": " + translate(r),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }
                
                String pw = tablePasswordInput.getText();
                r = Misc.validPassword(pw);
                if (r != null) {
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("table_password_error") + ": " + translate(r),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }

              } else {

                if (!tablePasswordInput.getText().isEmpty()) {
                  
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("empty_table_name_but_password"),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }
              }
              
              if (pub) {
                send("create " + Table.NO_GAME_FILE + " 3");
              } else {
                send("create " + Table.NO_GAME_FILE + " 3 " +                
                     tableNameInput.getText() + " " +
                     tablePasswordInput.getText());
              }
            }
            catch (Exception f) { serious(f); }
          }});

    } else {

      // If in tournament mode:

      create3Button.addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent evt) {
            try {
              boolean pub = tableNameInput.getText().equals("."+rbs("public"));

              Misc.msg("create " + pub);

              if(!tableNameInput.getText().isEmpty() && !pub) {

                String r = Misc.validId(tableNameInput.getText());
                if (r != null) {
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("table_name_error") + ": " + translate(r),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }

                String pw = tablePasswordInput.getText();
                r = Misc.validPassword(pw);
                if (r != null) {
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("table_password_error") + ": " + translate(r),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }

              } else {

                if (!tablePasswordInput.getText().isEmpty()) {

                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("empty_table_name_but_password"),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }
              }

              if (pub) {
                send("create " + Table.NO_GAME_FILE + " 3");
              } else {
                send("create " + Table.NO_GAME_FILE + " 3 " +
                     tableNameInput.getText() + " " +
                     tablePasswordInput.getText());
              }
            }
            catch (Exception f) { serious(f); }
          }});

      create4Button.addMouseListener(new MouseAdapter() {
          public void mouseClicked(MouseEvent evt) {
            try {
              boolean pub = tableNameInput.getText().equals("."+rbs("public"));

              Misc.msg("create " + pub);

              if(!tableNameInput.getText().isEmpty() && !pub) {

                String r = Misc.validId(tableNameInput.getText());
                if (r != null) {
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("table_name_error") + ": " + translate(r),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }

                String pw = tablePasswordInput.getText();
                r = Misc.validPassword(pw);
                if (r != null) {
                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("table_password_error") + ": " + translate(r),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }

              } else {

                if (!tablePasswordInput.getText().isEmpty()) {

                  JOptionPane.showMessageDialog(getThis(),
                                                rbs("empty_table_name_but_password"),
                                                rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
                  return;
                }
              }

              if (pub) {
                send("create " + Table.NO_GAME_FILE + " 4");
              } else {
                send("create " + Table.NO_GAME_FILE + " 4 " +
                     tableNameInput.getText() + " " +
                     tablePasswordInput.getText());
              }
            }
            catch (Exception f) { serious(f); }
          }});
    }
              
    // setup panel

    changeLanguageButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("language button clicked ");
	  String s = "";
	  if (englishCheckBox.isSelected()) { s += 'E'; }
	  if (germanCheckBox.isSelected())  { s += 'D'; }
	  if (polishCheckBox.isSelected())  { s += 'P'; }
	  if (frenchCheckBox.isSelected())  { s += 'F'; }
	  if (spanishCheckBox.isSelected()) { s += 'S'; }
	  if (czechCheckBox.isSelected())   { s += 'C'; }	  
	  if (s.length() == 0) s = "-"; // no language, computer?
	  if (s.length() > 3) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("Select_at_least_one"),
					  rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
	    return;
	  }

	  send("languages " + s);

	  int k = skatguiLangComboBox.getSelectedIndex();
	  settings.language = "";
	  if (k == 1) settings.language = "en";
	  if (k == 2) settings.language = "de";
	  if (k == 3) settings.language = "pl";
          if (k == 4) settings.language = "fr";
	  saveSettings();
	}});
    
    changePasswordButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("password change clicked ");

	  String r=Misc.validPassword(Misc.charArrayToString(changePasswordInput1.getPassword()));
	  if (r != null) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("illegal_current_password") + ": " + translate(r),
					  rbs("Change_Password_Error"), JOptionPane.ERROR_MESSAGE);
	    changePasswordInput1.requestFocus();
	    return;
	  }
          
	  r = Misc.validPassword(Misc.charArrayToString(changePasswordInput2.getPassword()));
	  if (r != null) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("illegal_new_password") + ": " + translate(r), 
					  rbs("Change_Password_Error"), JOptionPane.ERROR_MESSAGE);
	    changePasswordInput2.requestFocus();
	    return;
	  }
          
	  if (!Misc.charArrayToString(changePasswordInput3.getPassword())
	      .equals(Misc.charArrayToString(changePasswordInput2.getPassword()))) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("new_passwords_don't_match"), 
					  rbs("Change_Password_Error"), JOptionPane.ERROR_MESSAGE);
	    changePasswordInput3.requestFocus();
	    return;
	  }

	  send("password " + Misc.charArrayToString(changePasswordInput1.getPassword()) +
	       " " +  Misc.charArrayToString(changePasswordInput2.getPassword()));
	}});


    changeEmailButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
	  Misc.msg("email change clicked ");

	  String r = Misc.validEmail(changeEmailInput1.getText());
	  if (r != null) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("illegal_current_email_address") + ": " + translate(r),
					  rbs("Change_Email_Error"), JOptionPane.ERROR_MESSAGE);
	    changeEmailInput1.requestFocus();
	    return;
	  }

	  r = Misc.validEmail(changeEmailInput2.getText());
	  if (r != null) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("illegal_new_email_address") + ": " + translate(r),
					  rbs("Change_Email_Error"), JOptionPane.ERROR_MESSAGE);
	    changeEmailInput2.requestFocus();
	    return;
	  }
          
	  if (!changeEmailInput2.getText().equals(changeEmailInput3.getText())) {
	    JOptionPane.showMessageDialog(getThis(),
					  rbs("new_email_addresses_don't_match"), 
					  rbs("Change_Email_Error"), JOptionPane.ERROR_MESSAGE);
	    changeEmailInput3.requestFocus();
	    return;
	  }

	  send("email " + changeEmailInput1.getText() + " " + changeEmailInput2.getText());
	}});

    sizeSpinner.addChangeListener(new SpinnerChangeListener());

    beepCheckbox.addChangeListener(new BeepChangeListener());
    
    // tournament pane functionality

    tourReg3Button.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
          setReg(true);
	}});
    
    tourReg4Button.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
          setReg(false);
	}});

    // tourCompleteButton.addMouseListener(new MouseAdapter() {
    //     public void mouseClicked(MouseEvent evt) {
    //       int p = tourScheduleText.getCaretPosition();
    //       // compute caret line
    //       String s = tourScheduleText.getText();
    //       int l = 0, m = Math.min(s.length(), p);
    //       for (int i=0; i < m; i++)
    //         if (s[i] == '\n')
    //           ++l;
    //       complete(l);
    //     }});

    tourCreateButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {
          TournamentData td = new TournamentData();

          try {
            td.name = tourNameInput.getText();
            td.creator = id;
            td.seed = tourSeedInput.getText();
            td.password = tourPasswdInput.getText();
            td.numRounds = (Integer)tourNumRoundsSpinner.getValue();
            if (td.numRounds < 1 || td.numRounds > 30)
		throw new Exception(rbs("num_rounds_oo_range"));
            td.numRandomRounds = (Integer)tourNumRandRoundsSpinner.getValue();          
            td.numBlocksPerRound = (Integer)tourNumBlocksPerRoundSpinner.getValue();          
            td.only3tables = tourOnly3tablesCheckBox.isSelected();
            td.startDate = tourDateInput.getText();
            td.dayOffsets = new int[td.numRounds];
            td.startTimes = new int[td.numRounds];
            
            // parse schedule
            
            String t = tourScheduleText.getText();
            String[] parts = t.split("\n");
            
            if (parts.length != td.numRounds + 1)
		throw new Exception(rbs("num_lines_bad"));
              
            for (int i=1; i < parts.length; i++) {
              String[] p = parts[i].trim().split("\\s+");

              if (!p[0].equals(""+i))
		  throw new Exception(rbs("round_space") + i + " " + rbs("missing"));

              try { td.dayOffsets[i-1] = Integer.parseInt(p[1]); }
              catch (Throwable x) { throw new Exception(rbs("round_space") + i + rbs("space_day_!integer")); }

              String times[] = p[2].split(":");
              if (times.length != 3) throw new Exception(rbs("round_space") + i + rbs("space_start_time_bad"));

              int h, m, s;
              try { h = Integer.parseInt(times[0]); }
              catch (Throwable x) { throw new Exception(rbs("round_space") + i + rbs("space_start_time_bad")); }
              try { m = Integer.parseInt(times[1]); }
              catch (Throwable x) { throw new Exception(rbs("round_space") + i + rbs("space_start_time_bad")); }
              try { s = Integer.parseInt(times[2]); }
              catch (Throwable x) { throw new Exception(rbs("round_space") + i + rbs("space_start_time_bad")); }

              if (h < 0 || h > 23 || m < 0 || m > 59 || s < 0 || s > 59)
                throw new Exception(rbs("round_space") + i + rbs("space_start_time_bad"));
            
              td.startTimes[i-1] = h*3600 + m*60 + s;
            }

            td.secPerRound = roundSeconds();

            String r = td.sane();
            if (r != null)
		throw new Exception(rbs("tour_data_bad_colon_space") + r);
          }
          catch (Throwable e) {
            JOptionPane.showMessageDialog(getThis(),
                                          e.getMessage(),
                                          rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
            return;
          }

	    send(rbs("tour_create_space") + td.toInfoString());
          mainTabbedPane.setSelectedComponent(mainPanel);
        }});


    tableCreateButton.addMouseListener(new MouseAdapter() {
	public void mouseClicked(MouseEvent evt) {

          String name = tableNameInput2.getText();
          String seed = tableSeedInput.getText();
          String seat1 = tableSeatInput1.getText();
          String seat2 = tableSeatInput2.getText();
          String seat3 = tableSeatInput3.getText();
          String seat4 = tableSeatInput4.getText();          
          int blocks = (Integer)tableNumBlocksSpinner.getValue();
          int players = 3;
            
          try {
            String r = Misc.validId(name);
            if (r != null) throw new Exception(rbs("name_bad_colon_space") + translate(r));

            if (seed.length() != 0 && seed.length() < 4) throw new Exception(rbs("short_seed"));

            r = Misc.validId(seat1);
            if (r != null) throw new Exception(rbs("seat_bad_space") + "1: " + translate(r));
            r = Misc.validId(seat2);
            if (r != null) throw new Exception(rbs("seat_bad_space") + "2: " + translate(r));
            r = Misc.validId(seat3);
            if (r != null) throw new Exception(rbs("seat_bad_space") + "3: " + translate(r));

            if (!seat4.equals("")) {
              players = 4;
              r = Misc.validId(seat4);
              if (r != null) throw new Exception(rbs("seat_bad_space") + "4: " + translate(r));
            }
          }
          catch (Throwable e) {
            JOptionPane.showMessageDialog(getThis(),
                                          e.getMessage(),
                                          rbs("Error_Message"), JOptionPane.ERROR_MESSAGE);
            return;
          }

          send("ttcreate " + name + " " + players + " " + blocks + " " +
               seat1 + " " + seat2 + " " + seat3 + " " + seat4 + " " + seed);

          mainTabbedPane.setSelectedComponent(mainPanel);          
        }});

    // finishing up frame initialization
    
    addWindowListener(this);
    
    setResizable(false);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    reportHandlers("before resize");
    
    glob.resize(settings.scalePerc * 0.01f);
 
    reportHandlers("after resize");
    
    setTitle();
    setVisible(true);

    sc = new ServerClient();

    timer = new javax.swing.Timer(1000, new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          for (TablePanelBase tp : tablePanels) {
            SwingUtilities.
              invokeLater(new UpdateTablePanelRunnable(tp,
                                                       TablePanelUpdateAction.TIMER,
                                                       null,
                                                       isTournamentMode()));
          }

          for (TablePanelWindow tw : tableWindows) {
            SwingUtilities.
              invokeLater(new UpdateTablePanelRunnable((TablePanelBase)tw.getContentPane(),
                                                       TablePanelUpdateAction.TIMER,
                                                       null,
                                                       isTournamentMode()));
          }
        }
      });

    timer.start();
    
    gfxLogin = id.equals("");

    if (gfxLogin) {

      if (settings.saveLogin) {
        userIdInput.setText(settings.userId);
        passwordInput.setText(settings.password);
      }

      hostCombobox.setSelectedIndex(settings.hostInd);
      portCombobox.setSelectedIndex(settings.portInd);

      storeLoginCheckbox.setSelected(settings.saveLogin);

      // login tab active
      mainTabbedPane.setSelectedIndex(0);
      mainTabbedPane.setEnabledAt(0, true);
      mainTabbedPane.setEnabledAt(1, false);
      mainTabbedPane.setEnabledAt(2, false);      

    } else {
      
      // main tab active
      // remove login panel, enable setup/main
      
      mainTabbedPane.remove(loginPanel);
      mainTabbedPane.setEnabledAt(SETUP_INDEX, true);
      mainTabbedPane.setEnabledAt(MAIN_INDEX, true);
      mainTabbedPane.setEnabledAt(TOURNAMENT_INDEX, ENABLE_TOUR_TAB);      
      mainTabbedPane.setSelectedIndex(MAIN_INDEX);
      
      setTalkContext(logPane, "", true);
      setTitle();

      String r = sc.run(this, host_ + ":" + port_, host_, port_, id_, password_, cmds);
      
      if (r != null) {

        if (r.startsWith(ServerClient.ERR_VERSION)) {

          Misc.openURL(Misc.issHome+"/skatgui.jar");

          JOptionPane.showMessageDialog(getThis(),
                                        rbs("skatgui_available"),
                                        rbs("Title_Information"),
                                        JOptionPane.INFORMATION_MESSAGE);
          
        } else if (r.startsWith(ServerClient.ERR_CONNECTION)) {
            
          Misc.openURL(Misc.issHome + "/" + rbs("index.html"));
          JOptionPane.showMessageDialog(getThis(),
                                        rbs("connection_error"),
                                        rbs("Title_Information"),
                                        JOptionPane.INFORMATION_MESSAGE);

        } else {

          Misc.openURL(Misc.issHome + "/" + rbs("index.html"));
          JOptionPane.showMessageDialog(getThis(),
                                        rbs("login_error"),
                                        rbs("Title_Information"),
                                        JOptionPane.INFORMATION_MESSAGE);
        }
        
        Misc.msg(r);
        System.exit(-1);
      }

      id = sc.getClientId(); // may have changed (group)

      setTitle();
      checkVersion();
      
      send("news " + langStr);
      
      exitOnDisconnect = true;
    }


    reportHandlers("at end");
  }

  void doLogin()
  {
    // graphical login
      
    if (!gfxLogin) {
      Misc.err("pressing the login button should not be possible");
    }

    // collect login information from widgets

    String r;
          
    r = Misc.validId(userIdInput.getText());
    if (r != null) {
      JOptionPane.showMessageDialog(getThis(),
				    rbs("illegal_user_id") + ": " + translate(r),
				    rbs("Title_Login_Error"), JOptionPane.ERROR_MESSAGE);
      userIdInput.requestFocus();
      return;
    }

    r = Misc.validPassword(Misc.charArrayToString(passwordInput.getPassword()));
    if (r != null) {
      JOptionPane.showMessageDialog(getThis(),
				    rbs("illegal_password") + ": " + translate(r),
				    rbs("Title_Login_Error"), JOptionPane.ERROR_MESSAGE);
      passwordInput.requestFocus();
      return;
    }

    id = userIdInput.getText();
    password = Misc.charArrayToString(passwordInput.getPassword());

    Misc.msg("id=" + id + " " + "pw=" + password);

    int port = ports[portCombobox.getSelectedIndex()];
    host = hosts[hostCombobox.getSelectedIndex()];
    
    Misc.msg("connecting to : " + host + ":" + port);
          
    r = sc.run(getThis(), host + ":" + port, host, port, id, password, cmds);

    Misc.msg("conection result: " + r);
    
    if (r == null) {

      id = sc.getClientId(); // may have change (group logon)
      
      // login succeeded

      // send error report if file exists

      exitOnDisconnect = true;
      FileReader input = null;
              
      try { input = new FileReader(Misc.getReportFile()); }
      catch (FileNotFoundException e) {}

      if (input != null) {

	// error report exists, ask for permission to send it

        String[] textMessages = new String[2];
        textMessages[0] = rbs("Yes");
        textMessages[1] = rbs("No");
        
	final JOptionPane optionPane =
	  new JOptionPane(String.format(rbs("bugreport"), Misc.getReportFile()),
			  JOptionPane.QUESTION_MESSAGE,
			  JOptionPane.YES_NO_OPTION,
                          null,
                          textMessages);

        final JDialog dialog = new JDialog(getThis(),
					   rbs("send_report"),
					   true);
	dialog.setContentPane(optionPane);
	dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                
	optionPane.addPropertyChangeListener(new PropertyChangeListener() {
	    public void propertyChange(PropertyChangeEvent e) {
	      String prop = e.getPropertyName();
                      
	      if (dialog.isVisible()
		  && (e.getSource() == optionPane)
		  && (JOptionPane.VALUE_PROPERTY.equals(prop))) {

		//If you were going to check something
		//before closing the window, you'd do
		//it here.

		dialog.setVisible(false);
	      }
	    }
	  });
        dialog.pack();
	dialog.setLocationRelativeTo(getThis());
	dialog.setVisible(true);
                
        if (((String)optionPane.getValue()).equals(textMessages[0])) {
                  
	  Misc.msg("YES, I WANT TO SEND BUGREPORT");

	  BufferedReader bufRead = new BufferedReader(input);

	  // read file into string
	  StringBuffer sb = new StringBuffer();

	  sb.append("bugreport from " + id + " for skatgui version "
		    + version() + "\n");

	  try {
	    for (;;) {
	      int c = input.read();
	      if (c < 0) break;
	      sb.append((char)c);
	    }

	    String text = sb.toString();
	    final int MAX_BUGREPORT_LEN = 50000;
	    int l = text.length();
	    if (l > MAX_BUGREPORT_LEN) {
	      // cut text to size if too long
	      text = text.substring(l-MAX_BUGREPORT_LEN);
	    }
                  
	    sc.send("report " + Misc.encodeCRLF(text));
	  }
	  catch (Exception e) {
	    Misc.msg("exception when sending bugreport " + e.toString());
	  }

	} else {

	  Misc.msg("NO, I DON'T WANT TO SEND BUGREPORT");
	}

	// remove report file
	try {
	  input.close();
	  File of = new File(Misc.getReportFile());        
	  of.delete();
	}
	catch (Exception e) {
	  Misc.msg("deleting report file " + Misc.getReportFile() +
		   " failed: " + e.toString());
	}
      }

      setTitle();

      settings.hostInd = hostCombobox.getSelectedIndex();
      settings.portInd = portCombobox.getSelectedIndex();
      saveSettings();

      // remove login panel, enable setup/main

      mainTabbedPane.remove(loginPanel);
      mainTabbedPane.setEnabledAt(SETUP_INDEX, true);
      mainTabbedPane.setEnabledAt(TOURNAMENT_INDEX, ENABLE_TOUR_TAB);
      mainTabbedPane.setEnabledAt(MAIN_INDEX, true);
      mainTabbedPane.setSelectedIndex(MAIN_INDEX);

      setTalkContext(logPane, "", true); // ISS
      setTitle();

      checkVersion();

      send("news " + langStr);
      
    } else {

      // login error

      JOptionPane.showMessageDialog(getThis(), translate(r), rbs("Title_Login_Error"), JOptionPane.ERROR_MESSAGE);

      if (r.startsWith(ServerClient.ERR_VERSION)) {
	System.exit(0); // to start new version
      }
      
      mainTabbedPane.setSelectedIndex(0);
      mainTabbedPane.setEnabledAt(0, true);
      mainTabbedPane.setEnabledAt(1, false);
    }
  }

  void saveSettings()
  {
    settings.saveLogin = storeLoginCheckbox.isSelected();

    if (settings.saveLogin) {
      settings.userId = id;
      settings.password = password;
    } else {
      settings.userId = settings.password = "";
    }
    
    String r = settings.write(settingsFile);
    if (r != null)
      Misc.err(r);
  }
  
  // send to environment and append to server text area

  private void send(String s)
  {
    logTextArea.append(rbs("break_sent_colon_space") + s);
    logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
    sc.send(s);
  }

  private void setTitle()
  {
    setTitle("skatgui " + version() + " " + id); // + "@" + host + ":" + port);
  }
  
  public void run()
  {
    while (true) {
      Misc.sleep(1000);
    }
  }

  //   class UpdateUserListRunnable implements Runnable
  //   {
  //     class UserListModel extends javax.swing.AbstractListModel
  //     {
  //       public Vector<String> users = new Vector<String>();

  //       public int getSize() { return users.size(); }
  //       public Object getElementAt(int i) {
  //         return users.get(i);
  //       }
  //     }

  //     UserListModel tlm = new USerListModel();
    
  //     UpdateUserListRunnable(TreeMap<String, String> newMap) {

  //       // create copy of client data vector

  //       for (String s : newMap.keySet()) {
  //         tlm.tables.add(s);
  //       }
  //     }

  //     public void run() {
      
  //       // new model for client list

  //       // privateTableList.setModel(tlm);
  //     }
  //   }

  //   class UpdateTableListRunnable implements Runnable
  //   {
  //     class TableListModel extends javax.swing.AbstractListModel
  //     {
  //       public Vector<String> tables = new Vector<String>();

  //       public int getSize() { return tables.size(); }
  //       public Object getElementAt(int i) {
  //         return tables.get(i);
  //       }
  //     }

  //     TableListModel tlm = new TableListModel();
    
  //     UpdateTableListRunnable(TreeMap<String, String> newMap) {

  //       // create copy of client data vector

  //       for (String s : newMap.keySet()) {
  //         tlm.tables.add(s);
  //       }
  //     }

  //     public void run() {
      
  //       // new model for client list

  //       // privateTableList.setModel(tlm);
  //     }
  //   }

  // accessor for variable tournamentMode - Ryan
  boolean isTournamentMode() {
    return tournamentMode;
  }

  class UpdateTablePanelRunnable implements Runnable
  {
    TablePanelBase tablePanelBase;
    TablePanelUpdateAction action;
    String[] params;
    boolean tournamentMode;
    
    UpdateTablePanelRunnable(TablePanelBase panel,
                             TablePanelUpdateAction action,
                             String[] params,
                             boolean tournamentMode)
    {
      this.action = action;
      this.params = params;
      this.tournamentMode = tournamentMode;
      this.tablePanelBase = panel;
    }

    public void run() {
      
      try {
        tablePanelBase.game2Window(action, params);
      }

      catch (Exception e) { serious(e); }
    }
  }

  private void updateTablePanelLater(Table table,
                                     TablePanelUpdateAction action,
                                     String params[])
  {
    SwingUtilities.
      invokeLater(new UpdateTablePanelRunnable(findTablePanel(table, true),
                                               action,
                                               params,
                                               tournamentMode));
  }


  /** search table in panels and also in windows (if inBoth true)
   * @return null, null if not found
   */

  // This method looks for both types of table panels. - Ryan
  private TablePanelBase findTablePanel(Table table, boolean inBoth)
  {
    for (TablePanelBase p : tablePanels) {

      if (p.getTable() == table) {
        return p;
      }
    }

    if (!inBoth) return null;
    
    TablePanelWindow w = findTableWindow(table);
    if (w == null) return null;
    return (TablePanelBase)w.getContentPane();
  }

  /** @return table window for table, null if not found */
  private TablePanelWindow findTableWindow(Table table)
  {
    for (TablePanelWindow w : tableWindows) {
      if (w.table == table) {
	return w;
      }
    }
    return null;
  }
  
  class UpdateClientWindowRunnable implements Runnable
  {
    ClientWindowUpdateAction action;
    String[] params;
    Object[] moreParams;
    
    UpdateClientWindowRunnable(ClientWindowUpdateAction action,
                               String[] params, Object[] moreParams)
    {
      this.action = action;
      this.params = params;
      this.moreParams = moreParams;      
    }
    
    public void run() {
      clientWindowUpdateNow(action, params, moreParams);
    }
  }


  // dispatch update in swing thread
  private void clientWindowUpdateLater(ClientWindowUpdateAction action,
                                       String params[], Object[] moreParams)
  {
    SwingUtilities.invokeLater(new UpdateClientWindowRunnable(action, params, moreParams));    
  }

  // dispatch update in swing thread
  private void clientWindowUpdateWait(ClientWindowUpdateAction action,
                                      String params[], Object[] moreParams)
  {
    try {
      SwingUtilities.invokeAndWait(new UpdateClientWindowRunnable(action, params, moreParams));
    }
    catch (Exception e) {
      Misc.msg(Misc.stack2string(e));
    }
      
  }

  TalkContext findTalkContext(String name)
  {
    for (TalkContext tc : talkContexts) {
      if (tc.clientId.equals(name))
	return tc;
    }
    return null;
  }

  TalkContext findTalkContext(java.awt.Component co)
  {
    for (TalkContext tc : talkContexts) {
      if (tc.scrollPane == co)
	return tc;
    }
    return null;
  }

  void highlightTableButtons(Color color)
  { 
    // reset table button background
    
    for (TablePanelWindow tw : tableWindows) {
      if (!tw.table.getViewerName().equals(Table.NO_NAME)) {
        ((TablePanel)tw.getContentPane()).setShowMainButtonBG(color);
      }
    }

    // no SMW button in tournament tables to highlight - Ryan
    for (TablePanelBase tp : tablePanels) {
      if (tp instanceof TablePanel) {
        ((TablePanel)tp).setShowMainButtonBG(color);
      }
    }
  }
  
  
  // update window (run in swing thread)

  void clientWindowUpdateNow(ClientWindowUpdateAction action, String params[], Object[] moreParams)
  {
    switch (action) {
      
    case ADD_TO_LOBBY_TEXT:

      if(!isTournamentMode()) {
        if (mainTabbedPane.getSelectedIndex() != MAIN_INDEX)
          highlightTableButtons(Color.YELLOW);
      }
      
      lobbyTextArea.append("\n" + translate(params[0]));
      // lobbyTextArea.append("\n[" + Misc.currentTime() + "] " + params[0]);      
      lobbyTextArea.setCaretPosition(lobbyTextArea.getDocument().getLength());

      if (!params[0].startsWith(id+":"))
        Misc.beep(300, 100, 20);

      if (messageTabbedPane.getSelectedIndex() != 1) {
        // indicate new arrival
        messageTabbedPane.setBackgroundAt(1, Color.YELLOW);
      }
      break;

    case ADD_TO_TALK_TEXT:

      /* In live tournament mode, do not need users distracted by private messages.  Again,
         this will probably be reinstated for live tourney mode. - Ryan */
      if(!isTournamentMode()) {
        if (mainTabbedPane.getSelectedIndex() != MAIN_INDEX)      
          highlightTableButtons(Color.ORANGE);
      }

      TalkContext tc = setTalkContext(null, params[0], false);
      tc.textArea.append("\n" + params[0] + ": " + params[1]);
      tc.textArea.setCaretPosition(tc.textArea.getDocument().getLength());
      Misc.beep(500, 100, 20);

      if (tc.scrollPane != messageTabbedPane.getSelectedComponent()) {
        // indicate new arrival
        messageTabbedPane.setBackgroundAt(getTabIndex(tc.scrollPane), Color.ORANGE);
      }
      
      break;

    case ADD_TO_ISS_TEXT:
      logTextArea.append("\n" + translate(params[0]));
      break;
      
    case SET_ERROR:

      // report error in dialog
      
      JOptionPane.showMessageDialog(this, translate(params[0]), rbs("Error_Message"),
                                    JOptionPane.ERROR_MESSAGE);
      break;

      
    case ADD_UPDATE_USER:

      if (userIdToPos.get(params[0]) != null) {
        // update
        int row = userIdToPos.get(params[0]);
        for (int i=0; i < USER_TABLE_COLUMNS; i++) {
          userTableModel.setValueAt(params[i], row, i);
        }
	
      } else {
        // addition
        userTableModel.addRow(params);
        userIdToPos.put(params[0], userTableNextRow);
        userTableNextRow++;
      }
      break;

    case REMOVE_USER:
      {
        Integer rowi = userIdToPos.get(params[0]);
        if (rowi == null) Misc.err("rowi null");
        int row = rowi;
      
        userIdToPos.remove(params[0]);
      
        // copy last row into one that is to be removed

        userTableNextRow--;

        if (userTableNextRow != row) {
          for (int i=0; i < USER_TABLE_COLUMNS; i++) {
            userTableModel.setValueAt(userTableModel.getValueAt(userTableNextRow, i), row, i);
          }

          userIdToPos.put((String)userTableModel.getValueAt(row, 0), row);
        }
      
        // remove last row
        userTableModel.removeRow(userTableNextRow);
        break;
      }
      
    case ADD_UPDATE_TABLE:

      Misc.msg("ADD_UPDATE_TABLE: " + params[0]);

      if (tableIdToPos.get(params[0]) != null) {

        // update
        int r = tableIdToPos.get(params[0]);
        for (int i=0; i < TABLE_TABLE_COLUMNS; i++) {
          tableTableModel.setValueAt(params[i], r, i);
        }
	
      } else {
	
        // addition
        tableTableModel.addRow(params);
        tableIdToPos.put(params[0], tableTableNextRow);
        tableTableNextRow++;
      }
      break;

    case REMOVE_TABLE:
      {
        Integer rowi2 = tableIdToPos.get(params[0]);
        if (rowi2 == null) Misc.err("rowi2 null");
        int row = rowi2;
      
        tableIdToPos.remove(params[0]);
      
        // copy last row into one that is to be removed

        tableTableNextRow--;

        if (tableTableNextRow != row) {
          for (int i=0; i < TABLE_TABLE_COLUMNS; i++) {
            tableTableModel.setValueAt(tableTableModel.getValueAt(tableTableNextRow, i), row, i);
          }

          tableIdToPos.put((String)tableTableModel.getValueAt(row, 0), row);
        }
      
        // remove last row
        tableTableModel.removeRow(tableTableNextRow);
        break;
      }

    case ADD_UPDATE_TOUR:

      String name = params[0];
      
      Misc.msg("ADD_UPDATE_TOUR: " + name);

      if (tourIdToPos.get(name) != null) {

        // update
        int r = tourIdToPos.get(name);
        for (int i=0; i < TOUR_TABLE_COLUMNS; i++) {
          Misc.msg("P=" + params[i]);
          tourTableModel.setValueAt(params[i], r, i);
        }
	
      } else {
	
        // addition
        tourTableModel.addRow(params);
        tourIdToPos.put(params[0], tourTableNextRow);
        tourTableNextRow++;
      }
      break;

    case REMOVE_TOUR:
      {
        String t = params[0]; 
        Misc.msg("REMOVE_TOUR: " + t);

        if (tourIdToPos.get(t) == null)
          Misc.err("tour to be removed not found");

        int row = tourIdToPos.get(t);
        
        tourIdToPos.remove(t);
      
        // copy last row into one that is to be removed

        tourTableNextRow--;

        if (tourTableNextRow != row) {
          for (int i=0; i < TOUR_TABLE_COLUMNS; i++) {
            tourTableModel.setValueAt(tourTableModel.getValueAt(tourTableNextRow, i), row, i);
          }

          tourIdToPos.put((String)tourTableModel.getValueAt(row, 0), row);
        }
      
        // remove last row
        tourTableModel.removeRow(tourTableNextRow);
        break;
      }

    case INVITE:
      
      final JOptionPane optionPane =
        new JOptionPane(String.format(rbs("invitation_dialog"), params[0], params[1]),
                        JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.YES_NO_OPTION);
      
      final JDialog dialog = new JDialog(getThis(),
                                         rbs("invitation_title"),
                                         true);
      dialog.setContentPane(optionPane);
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      
      optionPane.addPropertyChangeListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent e) {
            String prop = e.getPropertyName();
            
            if (dialog.isVisible()
                && (e.getSource() == optionPane)
                && (JOptionPane.VALUE_PROPERTY.equals(prop))) {
              
              //If you were going to check something
              //before closing the window, you'd do
              //it here.
              
              dialog.setVisible(false);
            }
          }
        });
      dialog.pack();
      dialog.setLocationRelativeTo(getThis());
      dialog.setVisible(true);
      
      int value = ((Integer)optionPane.getValue()).intValue();
      if (value == JOptionPane.YES_OPTION) {
        clearNameSelection(); // don't invite selected players
        send("join " + params[1] + " " + params[2]);
      }        
      break;
      
    case CREATE_TABLE_WINDOW:
      {
        Table table = (Table)moreParams[0];

        TablePanelBase p;
        
        String names = getSelectedNames();
        clearNameSelection();

        // If not in tourney mode, just create a normal table panel. - Ryan
        if (!isTournamentMode()) {
          p = new TablePanel(getThis(),
                             null,
                             id, 
                             table,
                             cardDeckPath, cardDeckStringFromSelection(),
                             names);
        } else {

          // If in tourney mode, create the other type of table panel.
          p = new TournamentTablePanel2(getThis(),
                                        null,
                                        id,
                                        table,
                                        cardDeckPath, cardDeckStringFromSelection(),
                                        names);
        }

        /* In tournament mode as in non-tournament mode, users can open up their table
           in a new window. - Ryan */
        if (!asWindowCheckbox.isSelected()) {
          // create panel
          String n = table.getViewerName();
          if (n.equals(Table.NO_NAME)) {
            n = rbs("Observer");
          };
          mainTabbedPane.add(n+"@"+table.getId(), p);
          mainTabbedPane.setSelectedComponent(p);
          tablePanels.add(p);
          glob.resize(settings.scalePerc*0.01f);
        } else {          
          // create window
          TablePanelWindow tw = new TablePanelWindow(p, table);
          p.setTableFrame(tw);
          tableWindows.add(tw);
          String n = table.getViewerName();
          if (n.equals(Table.NO_NAME)) {
            n = rbs("Observer");
          } else {
            n = rbs("Player") + " " + n;
          }
          tw.setTitle(n +" @ " + (table.getTourny() ? "Tournament " : "") +
                      rbs("Table") + " " + table.getId());
          glob.add(tw);
          glob.resize(settings.scalePerc*0.01f);
          tw.invalidate();
          tw.validate();
          tw.setVisible(true);
          tw.toFront();
        }

        break;
      }

    case DESTROY_TABLE_WINDOW:
      {
        Table table = (Table)moreParams[0];
        TablePanelBase p = findTablePanel(table, false);

        if (tablePanels.remove(p)) {
          mainTabbedPane.remove(p);
          break;
        }

        JFrame tw = findTableWindow(table);
        if (tw == null) break; // not found!?

        glob.remove(tw);
	htmlResults = new HTMLResultTable(table);
	// table.saveTableHistory();
        tableWindows.remove(tw);
        tw.setVisible(false);        
        tw.dispose();
        break;
      }
      
    default:
      Misc.err("unknown case");
    }
    repaint();
  }


  public void handleServiceMsg(ServiceClient.ServiceMsg msg, String line)
  {
    if ((msg instanceof ServiceClient.TableAddedOrUpdateMsg)) {

      final ServiceClient.TableAddedOrUpdateMsg tmsg =
	(ServiceClient.TableAddedOrUpdateMsg)msg;

      String[] pls = new String[] {
	tmsg.tableId, tmsg.gameNum+"",
	"?", "?", "?", "?"
      };

      if (tmsg.playerNum < 4) pls[5] = "#";

      for (int i=0; i < tmsg.playerNum; i++) {

	String group = Table.groupName(tmsg.players.get(i));
	ClientData cd = sc.getServiceClient().getClientData(group, false);
        
	if (cd != null)
	  pls[2+i] = tmsg.players.get(i) + " " +cd.languages;
        else if (tmsg.players.get(i).equals(Table.NO_NAME))
	  pls[2+i] = "<"+rbs("join") + ">"; // empty seat -> join
        else
	  pls[2+i] = tmsg.players.get(i); // disconnected
      }
      
      clientWindowUpdateWait(ClientWindowUpdateAction.ADD_UPDATE_TABLE, pls, null);
      
    } else if (msg instanceof ServiceClient.TableRemovedMsg) {

      final ServiceClient.TableRemovedMsg tmsg = (ServiceClient.TableRemovedMsg)msg;
      clientWindowUpdateWait(ClientWindowUpdateAction.REMOVE_TABLE,
			     new String[] { tmsg.tableId }, null);
      
    } else if (msg instanceof ServiceClient.TableStateMsg) {

      // new table state

      final ServiceClient.TableStateMsg tmsg = (ServiceClient.TableStateMsg)msg;
      // update table window
      updateTablePanelLater(tmsg.table, TablePanelUpdateAction.INFO, null);
        
    } else if ((msg instanceof ServiceClient.TourAddedOrUpdateMsg)) {

      final ServiceClient.TourAddedOrUpdateMsg tmsg = (ServiceClient.TourAddedOrUpdateMsg)msg;
      TournamentData td = tmsg.td;
      
      String[] params = new String[] {
	td.name,
        td.numRounds +"/" + td.gamesPerRound() + "/" + (td.secPerRound/60) + "/" + td.seats(),
        "",
        "",
        td.numPlayers+""
      };

      String nextTime = "";
      if (td.state != TournamentData.STATE_FINISHED) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(td.nextTimeInMillis());
        nextTime = String.format(locEn, "%d-%d-%d %02d:%02d:%02d",
                                 now.get(Calendar.YEAR),
                                 now.get(Calendar.MONTH)+1,
                                 now.get(Calendar.DAY_OF_MONTH),
                                 now.get(Calendar.HOUR_OF_DAY),
                                 now.get(Calendar.MINUTE),
                                 now.get(Calendar.SECOND));
      }
      
      if (td.state == TournamentData.STATE_WAIT_START) {

        if (td.currentRound < 0)
	    params[2] = rbs("Join_Period");
        else
	    params[2] = rbs("Break");
        params[3] = rbs("Start_rd_space") + (td.currentRound + 2) + " " + nextTime;
        
      } else if (td.state == TournamentData.STATE_WAIT_END) {

	  params[2] = rbs("Playing");
	  params[3] = rbs("End_rd_space") + (td.currentRound+1) + " " + nextTime;

      } else if (td.state == TournamentData.STATE_FINISHED) {

	  params[2] = rbs("Finished");
        params[3] = "";

      }

      clientWindowUpdateWait(ClientWindowUpdateAction.ADD_UPDATE_TOUR, params, null);
      
    } else if (msg instanceof ServiceClient.TourRemovedMsg) {

      final ServiceClient.TourRemovedMsg tmsg = (ServiceClient.TourRemovedMsg)msg;
      clientWindowUpdateWait(ClientWindowUpdateAction.REMOVE_TOUR, new String[] { tmsg.tourId }, null);
      
    } else if (msg instanceof ServiceClient.InviteMsg) {

      // client was invited to a table

      final ServiceClient.InviteMsg tmsg = (ServiceClient.InviteMsg)msg;

      clientWindowUpdateWait(ClientWindowUpdateAction.INVITE,
			     new String[] { tmsg.from, tmsg.tableId, tmsg.tablePassword },
			     null);

    } else if (msg instanceof ServiceClient.CreateMsg) {

      // client visits new table

      final ServiceClient.CreateMsg tmsg = (ServiceClient.CreateMsg)msg;

      // create new window in GUI thread

      clientWindowUpdateWait(ClientWindowUpdateAction.CREATE_TABLE_WINDOW,
			     null, new Object[] { tmsg.table });
        
    } else if (msg instanceof ServiceClient.DestroyMsg) {

      // client no longer watches table

      final ServiceClient.DestroyMsg tmsg = (ServiceClient.DestroyMsg)msg;

      // destroy table window in GUI thread

      clientWindowUpdateWait(ClientWindowUpdateAction.DESTROY_TABLE_WINDOW,
			     null, 
			     new Object[] { tmsg.table });

    } else if (msg instanceof ServiceClient.DisconnectMsg) {

      if (exitOnDisconnect) {
	Misc.msg("SERVER DISCONNECTED!");
	quitAction();
      }
      
    } else if (msg instanceof ServiceClient.ClientArrivedOrUpdateMsg) {

      ServiceClient.ClientArrivedOrUpdateMsg tmsg = (ServiceClient.ClientArrivedOrUpdateMsg)msg;

      String[] pline = new String[] {
	tmsg.cd.id, tmsg.cd.languages, ((tmsg.cd.group == 0) ? "-" : (""+tmsg.cd.cloneNum)),
        (int)(Math.round(tmsg.cd.rating))+""
      };
      
      clientWindowUpdateWait(ClientWindowUpdateAction.ADD_UPDATE_USER, pline, null);
      
      if (tmsg.cd.id.equals(id)) {

	// update language buttons

	englishCheckBox.setSelected(tmsg.cd.languages.indexOf('E') >= 0);
	germanCheckBox.setSelected(tmsg.cd.languages.indexOf('D') >= 0);	
	polishCheckBox.setSelected(tmsg.cd.languages.indexOf('P') >= 0);
	frenchCheckBox.setSelected(tmsg.cd.languages.indexOf('F') >= 0);
	spanishCheckBox.setSelected(tmsg.cd.languages.indexOf('S') >= 0);
	czechCheckBox.setSelected(tmsg.cd.languages.indexOf('C') >= 0);
      }

      TalkContext tc = findTalkContext(tmsg.cd.id);
      if (tc == null) return;
      tc.textArea.append("\n<<< " + tmsg.cd.id + " " + rbs("returned") + " >>>");
      
    } else if (msg instanceof ServiceClient.ClientDepartedMsg) {

      ServiceClient.ClientDepartedMsg tmsg = (ServiceClient.ClientDepartedMsg)msg;

      clientWindowUpdateWait(ClientWindowUpdateAction.REMOVE_USER,
			     new String[] { tmsg.clientId }, null );

      TalkContext tc = findTalkContext(tmsg.clientId);
      if (tc == null) return;
      tc.textArea.append("\n<<< " + tmsg.clientId + " " + rbs("left") + " >>>");
      
    } else if (msg instanceof ServiceClient.TableTellMsg) {

      // table tell
      
      ServiceClient.TableTellMsg tmsg = (ServiceClient.TableTellMsg)msg;
      updateTablePanelLater(tmsg.table, TablePanelUpdateAction.TELL,
                            new String[] { tmsg.fromId, tmsg.text });
      return;
      
    } else if (msg instanceof ServiceClient.TablePlayMsg) {

      // play move at table
      
      ServiceClient.TablePlayMsg tmsg = (ServiceClient.TablePlayMsg)msg;
      updateTablePanelLater(tmsg.table, TablePanelUpdateAction.MOVE, new String[] { tmsg.player, tmsg.move });
      return;
      
    } else if (msg instanceof ServiceClient.TableStartMsg) {

      // game started
      
      ServiceClient.TableStartMsg tmsg = (ServiceClient.TableStartMsg)msg;
      updateTablePanelLater(tmsg.table, TablePanelUpdateAction.START, null);
      return;
      
    } else if (msg instanceof ServiceClient.TableEndMsg) {

      // game ended
      
      ServiceClient.TableEndMsg tmsg = (ServiceClient.TableEndMsg)msg;
      updateTablePanelLater(tmsg.table, TablePanelUpdateAction.END, null);
      return;
      
    } else if (msg instanceof ServiceClient.TableErrorMsg) {

      // table error message

      ServiceClient.TableErrorMsg tmsg = (ServiceClient.TableErrorMsg)msg;
      updateTablePanelLater(tmsg.table, TablePanelUpdateAction.ERROR,
                            new String[] { tmsg.text });
      return;

    } else if (msg instanceof ServiceClient.YellMsg) {

      // yell: append message to lobby text

      ServiceClient.YellMsg tmsg = (ServiceClient.YellMsg)msg;
      clientWindowUpdateLater(ClientWindowUpdateAction.ADD_TO_LOBBY_TEXT,
			      new String[] { tmsg.fromId + ": " + tmsg.text}, null );
      return;

    } else if (msg instanceof ServiceClient.TellMsg) {

      // tell: append message to talk context

      ServiceClient.TellMsg tmsg = (ServiceClient.TellMsg)msg;

      Misc.msg("TELL " + tmsg.fromId + " " + tmsg.text);
      
      clientWindowUpdateLater(ClientWindowUpdateAction.ADD_TO_TALK_TEXT,
			      new String[] { tmsg.fromId, tmsg.text}, null );
      return;

    } else if (msg instanceof ServiceClient.ErrorMsg) {

      // error messages are displayed in active error text field

      ServiceClient.ErrorMsg tmsg = (ServiceClient.ErrorMsg)msg;
      clientWindowUpdateLater(ClientWindowUpdateAction.SET_ERROR,
			      new String[] { tmsg.text }, null );
      return;

    } else if (msg instanceof ServiceClient.TextMsg) {

      // text messages are displayed in the ISS text area

      ServiceClient.TextMsg tmsg = (ServiceClient.TextMsg)msg;
      clientWindowUpdateLater(ClientWindowUpdateAction.ADD_TO_ISS_TEXT,
			      new String[] { tmsg.textId + ":\n" + tmsg.text }, null );
      return;

    } else if (msg instanceof ServiceClient.TimeMsg) {

      // current time is displayed in the ISS text area

      ServiceClient.TimeMsg tmsg = (ServiceClient.TimeMsg)msg;
      clientWindowUpdateLater(ClientWindowUpdateAction.ADD_TO_ISS_TEXT,
			      new String[] { "ISS " + rbs("time") + ": " + tmsg.date + " " + tmsg.time },
                              null );
      return;

    } else if (msg instanceof ServiceClient.FingerMsg) {

      // finger message displayed in the ISS text area

      ServiceClient.FingerMsg tmsg = (ServiceClient.FingerMsg)msg;
      clientWindowUpdateLater(ClientWindowUpdateAction.ADD_TO_ISS_TEXT,
			      new String[] { rbs("break_Info_on_space") + tmsg.name + " " + tmsg.info }, null );
      return;

    } else {
      
      // append line to text area
      
	logTextArea.append(rbs("break_!handled_colon_space") + line);
      logTextArea.setCaretPosition(logTextArea.getDocument().getLength());

      Misc.msg("LINE NOT HANDLED: " + line);
    }
  }


  void setUserTableListener()
  {
    // replace existing mouse listeners by customized one
    
    MouseListener[] mls = (userTable.getListeners(MouseListener.class));

    for (MouseListener ml : mls) {
      Misc.msg("REMOVED ML");
      userTable.removeMouseListener(ml);
    }

    userTable.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e)
        {
          Point p = e.getPoint();
          int row = userTable.rowAtPoint(p);
          int column = userTable.columnAtPoint(p); // This is the view column!
          
          Misc.msg("clicked " + row + " " + column);

          ListSelectionModel model = userTable.getSelectionModel();

          if (true || SwingUtilities.isLeftMouseButton(e)) {

            // left click: toggle selection

            // Get the ListSelectionModel of the JTable

            if (model.isSelectedIndex(row)) {
              model.removeSelectionInterval(row, row);
            } else {
              model.addSelectionInterval(row, row);
            }

          } else {

            // others: clear selection
            model.clearSelection();
          }
        }
      });
  }
  
  /** This method is called from within the constructor to
   * initialize the form.
   */

  private void initComponents()
  {
      tourReg3Button.setText(rbs("Reg3"));
      tourReg4Button.setText(rbs("Reg4"));
      tourNameLabel.setText(rbs("Name") + ":");
      tourSeedLabel.setText(rbs("Seed_opt") + ":");
      tourPasswdLabel.setText(rbs("Password_opt") + ":");
      tableNameLabel2.setText(rbs("Name") + ":");
      tableSeedLabel.setText(rbs("Seed_opt") + ":");
      tableNumBlocksLabel.setText(rbs("Blocks") + ":");
      tourNumRoundsLabel.setText(rbs("Rounds") + ":");
      tourNumBlocksPerRoundLabel.setText(rbs("Blocks_per_round") + ":");
      tourNumRandRoundsLabel.setText(rbs("Random_rounds") + ":");
      tourOnly3tablesLabel .setText(rbs("Only_3Tables") + ":");
      tourGameTimeLabel.setText(rbs("Hand_Time_min") + ":");
      tourMinLabel.setText(rbs("minutes"));
      tourDateLabel.setText(rbs("Date_yyyymmdd") + ":");
      tourScheduleLabel.setText(rbs("Round_Schedule") + ":");
      tourCreateButton.setText(rbs("Create"));
      tourCompleteButton.setText(rbs("Complete"));
      tableCreateButton.setText(rbs("Create")); 
      tableSeatLabel1.setText(rbs("Seat") + " 1:");
      tableSeatLabel2.setText(rbs("Seat") + " 2:");
      tableSeatLabel3.setText(rbs("Seat") + " 3:");
      tableSeatLabel4.setText(rbs("Seat") + " 4 " + rbs("(opt)") + ":");

    mainTabbedPane = new javax.swing.JTabbedPane();
    loginPanel = new javax.swing.JPanel();
    jLabel5 = new javax.swing.JLabel(rbs("International_Skat_Server"));
    jLabel5.setFont(new java.awt.Font("Dialog", 1, 28));
     
    loginDataPanel = new javax.swing.JPanel(new MigLayout());
    storeLoginCheckbox = new javax.swing.JCheckBox(rbs("Store_Login_Data"));
    asWindowCheckbox = new javax.swing.JCheckBox(rbs("As-Window"));
    asWindowCheckbox.setToolTipText(rbs("Tooltip-As-Window"));
    userIdLabel = new javax.swing.JLabel(rbs("User_Id"));
    userIdInput = new javax.swing.JTextField(10);
    passwordLabel = new javax.swing.JLabel(rbs("Password"));
    passwordInput = new javax.swing.JPasswordField(10);
    loginButton = new javax.swing.JButton(rbs("Login"));
    portCombobox = new javax.swing.JComboBox();
    hostCombobox = new javax.swing.JComboBox();    
    aboutButton = new javax.swing.JButton(rbs("About"));
    jPanel12 = new javax.swing.JPanel(new MigLayout());
    jPanel3 = new javax.swing.JPanel(new MigLayout());
    messageTabbedPane = new javax.swing.JTabbedPane();
    logPane = new javax.swing.JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    logTextArea = new javax.swing.JTextArea();
    lobbyPane = new javax.swing.JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    lobbyTextArea = new javax.swing.JTextArea();
    mainInput = new javax.swing.JTextField();
    jPanel1 = new javax.swing.JPanel(new MigLayout());
    jPanel2 = new javax.swing.JPanel(new MigLayout());
    jPanel22 = new javax.swing.JPanel(new MigLayout());

    tableTable = new javax.swing.JTable();
    tourTable = new javax.swing.JTable();
    userTable = new javax.swing.JTable();

    if(!isTournamentMode()) {
      createButton = new javax.swing.JButton(rbs("Create_Table"));
    } else {
      create3Button = new javax.swing.JButton(rbs("Create_3Player_Tournament_Table"));
      create4Button = new javax.swing.JButton(rbs("Create_4Player_Tournament_Table"));
    }

    tableNameInput = new javax.swing.JTextField("."+rbs("public"), 6);
    tablePasswordInput = new javax.swing.JTextField(5);
    jLabel3 = new javax.swing.JLabel(rbs("Table_Name")+":");
    jLabel3a = new javax.swing.JLabel(rbs("Password")+":");
    
    tableScrollPane = new javax.swing.JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    tourScrollPane = new javax.swing.JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    userScrollPane = new javax.swing.JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    talkButton = new javax.swing.JButton(rbs("Button-Talk"));
    talkButton.setToolTipText(rbs("Tooltip-Talk"));
    quitButton = new javax.swing.JButton(rbs("Quit"));
    ratingsButton = new javax.swing.JButton(rbs("Ratings"));
    showTableButton = new javax.swing.JButton(rbs("Button-Show-Table"));
    showTableButton.setToolTipText(rbs("Tooltip-Show-Table"));
    userInfoButton = new javax.swing.JButton(rbs("User_Info"));
    helpButton = new javax.swing.JButton(rbs("Help"));
    setupPanel = new javax.swing.JPanel(new MigLayout("nogrid"));
    tourPanel = new javax.swing.JPanel(new MigLayout("nogrid"));
    jPanel5 = new javax.swing.JPanel(new MigLayout());
    changePasswordLabel1 = new javax.swing.JLabel(rbs("Current_password")+":");
    changePasswordLabel2 = new javax.swing.JLabel(rbs("New_password")+":");
    changePasswordLabel3 = new javax.swing.JLabel(rbs("Retype_new_password")+":");
    changePasswordInput1 = new javax.swing.JPasswordField(8);
    changePasswordInput2 = new javax.swing.JPasswordField(8);
    changePasswordInput3 = new javax.swing.JPasswordField(8);
    changePasswordButton = new javax.swing.JButton(rbs("Set"));
    jPanel6 = new javax.swing.JPanel(new MigLayout());
    changeEmailLabel1 = new javax.swing.JLabel(rbs("Current_address")+":");
    changeEmailLabel2 = new javax.swing.JLabel(rbs("New_address")+":");
    changeEmailLabel3 = new javax.swing.JLabel(rbs("Retype_new_address")+":");
    changeEmailInput1 = new javax.swing.JTextField(20);
    changeEmailInput2 = new javax.swing.JTextField(20);
    changeEmailInput3 = new javax.swing.JTextField(20);
    changeEmailButton = new javax.swing.JButton(rbs("Set"));
    changeLanguageButton = new javax.swing.JButton(rbs("Set"));
    sizeSpinner = new javax.swing.JSpinner();
    beepCheckbox = new javax.swing.JCheckBox(rbs("Warning"));
    skatguiLangComboBox = new javax.swing.JComboBox();
    jLabel1 = new javax.swing.JLabel("SkatGUI:");
    jLabel2 = new javax.swing.JLabel(rbs("You_speak")+":");

    englishCheckBox = new JCheckBox("English (E)");
    germanCheckBox = new JCheckBox("Deutsch (D)");
    polishCheckBox = new JCheckBox("Polski (P)");
    frenchCheckBox = new JCheckBox("Français (F)");
    spanishCheckBox = new JCheckBox("Español (S)");
    czechCheckBox = new JCheckBox("Čeština (C)");

    userTableHeaders = new String[] {
      rbs("User"),
      rbs("Lang"),
      "#",
      rbs("Rating"),
    };

    tableTableHeaders = new String[] {
      rbs("Table"),
      rbs("Games"),
      rbs("P1"),
      rbs("P2"),
      rbs("P3"),
      rbs("P4")
    };

    tourTableHeaders = new String[] {
      rbs("Tournament"),
      rbs("rds/gpr/tpr/seats"),
      rbs("Status"),
      rbs("Next_Event"),
      rbs("Players")
    };

    origTableFont = userTable.getFont();
    
    userScrollPane.setViewportView(userTable);
    tableScrollPane.setViewportView(tableTable);
    tourScrollPane.setViewportView(tourTable);

    // main panel !!!
    
    jPanel7 = new JPanel(new MigLayout("nogrid,filly,flowy, ins 0", "[center]")); //"filly,nogrid", "[fill]" for resizable cont.

    JPanel jPanel8 = new JPanel(new MigLayout("nogrid,fillx,ins 0", "[center]"));

    talkButton.setMargin(new Insets(2, 5, 2, 5));
    ratingsButton.setMargin(new Insets(2, 5, 2, 5));
    quitButton.setMargin(new Insets(2, 5, 2, 5));    
    userInfoButton.setMargin(new Insets(2, 5, 2, 5));
    helpButton.setMargin(new Insets(2, 5, 2, 5));    
    showTableButton.setMargin(new Insets(2, 5, 2, 5));
    showTableButton.setMargin(new Insets(2, 5, 2, 5));
    if(!isTournamentMode()) {
      createButton.setMargin(new Insets(2, 5, 2, 5));
    }
    else {
      create3Button.setMargin(new Insets(2, 5, 2, 5));
      create4Button.setMargin(new Insets(2, 5, 2, 5));
    }

    jPanel8.add(talkButton);  
    jPanel8.add(userInfoButton);  
    jPanel8.add(helpButton, "wrap");
    jPanel8.add(showTableButton);
    jPanel8.add(ratingsButton);
    jPanel8.add(quitButton);

    jPanel7.add(userScrollPane, "growx, growy");//"width pref:pref:max, growy");
    jPanel7.add(jPanel8);//"width pref:pref:max, growy");

    if(isTournamentMode()) {
      jPanel22 = new JPanel(new MigLayout("ins 0, flowy, align right, gap 5!", "[shrink]", "[shrink][shrink][shrink]"));
      jPanel22.add(create3Button);
      jPanel22.add(create4Button);
    }

    jPanel2 = new JPanel(new MigLayout("nogrid, ins 0, fill"));

    jPanel2.add(tableScrollPane, "width pref:pref:max, height pref:pref:pref, growx, wrap");
    jPanel2.add(tourScrollPane, "width pref:pref:max, height pref:pref:pref, growx, wrap");    

    // jPanel2.add(jLabel3, "split,center");
    // jPanel2.add(tableNameInput);
    // jPanel2.add(jLabel3a);      
    // jPanel2.add(tablePasswordInput);

    if(!isTournamentMode()) {
      jPanel2.add(createButton, "center");
      jPanel2.add(asWindowCheckbox, "wrap");      
    } else {
      jPanel2.add(jPanel22, "center, gapright 5!");
      jPanel2.add(asWindowCheckbox, "wrap");

      // jPanel2.add(create3Button, "center, split, egx 1");
      // jPanel2.add(asWindowCheckbox, "wrap");
      // jPanel2.add(create4Button, "shrink, egx 1, wrap");
    }
    
    jPanel2.add(messageTabbedPane, "growx, growy, wrap");
    jPanel2.add(mainInput, "growx");

    mainInput.setBorder(BorderFactory.
                        createTitledBorder(javax.swing.BorderFactory.createTitledBorder("Input")));

    mainPanel = new JPanel(new MigLayout("nogrid, insets 0px, fill"));//, "[fill][fill]", "[fill]"));
      
    mainPanel.add(jPanel7, "grow y");
    mainPanel.add(jPanel2, "w 100%, grow y");

    
    // login panel
    
    portCombobox.setModel(new DefaultComboBoxModel(portStrings));
    hostCombobox.setModel(new DefaultComboBoxModel(hosts));    

    JPanel p1 = new JPanel(new MigLayout());
    p1.add(userIdLabel, "gapy 20");
    p1.add(userIdInput, "wrap");
    p1.add(passwordLabel);
    p1.add(passwordInput, "wrap");

    JPanel p2 = new JPanel(new MigLayout());
    p2.add(storeLoginCheckbox, "gapy 20");
    p2.add(aboutButton, "wrap");
    p2.add(hostCombobox, "gapy 20");
    p2.add(portCombobox, "gapy 20");
    p2.add(loginButton);

    loginPanel.setLayout(new MigLayout("nogrid, center"));

    // load logo
    
    loginPanel.add(new LogoCanvas(), "gapy 20, center, wrap 15");
    loginPanel.add(jLabel5, "gapy 20, center, wrap 15");    
    loginPanel.add(p1, "center, wrap 10");
    loginPanel.add(p2, "center");
    
    // message tabbed pane
    
    messageTabbedPane.setBorder(BorderFactory.createTitledBorder(rbs("Messages")));
    messageTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
    
    logTextArea.setColumns(60);// !!!
    logTextArea.setEditable(false);
    logTextArea.setLineWrap(true);
    logTextArea.setRows(18);
    logPane.setViewportView(logTextArea);
    
    messageTabbedPane.addTab("ISS", logPane);
    
    lobbyTextArea.setBorder(null);
    lobbyTextArea.setColumns(60); //!!!
    lobbyTextArea.setEditable(false);
    lobbyTextArea.setLineWrap(true);
    lobbyTextArea.setRows(5); //!!!
    lobbyPane.setViewportView(lobbyTextArea);
    
    messageTabbedPane.addTab("Lobby", lobbyPane);

    // setup panel
    
    jPanel5.setBorder(BorderFactory.
                      createTitledBorder(BorderFactory.createTitledBorder(rbs("Change_Password"))));

    jPanel5.add(changePasswordLabel1);
    jPanel5.add(changePasswordInput1, "wrap");
    jPanel5.add(changePasswordLabel2);
    jPanel5.add(changePasswordInput2, "wrap");
    jPanel5.add(changePasswordLabel3);
    jPanel5.add(changePasswordInput3);
    jPanel5.add(changePasswordButton, "wrap");


    jPanel6.setBorder(BorderFactory.
                      createTitledBorder(BorderFactory.createTitledBorder(rbs("Change_Email_Address"))));
    jPanel6.add(changeEmailLabel1);
    jPanel6.add(changeEmailInput1, "wrap");
    jPanel6.add(changeEmailLabel2);
    jPanel6.add(changeEmailInput2, "wrap");
    jPanel6.add(changeEmailLabel3);
    jPanel6.add(changeEmailInput3);
    jPanel6.add(changeEmailButton, "wrap");

    jPanel4.setBorder(BorderFactory.
                      createTitledBorder(BorderFactory.createTitledBorder(rbs("Language_Preferences"))));

    jPanel41.add(jLabel2, "gap 15, span 1 2");
    jPanel41.add(englishCheckBox);
    jPanel41.add(germanCheckBox);
    jPanel41.add(polishCheckBox, "wrap");
    jPanel41.add(frenchCheckBox);
    jPanel41.add(spanishCheckBox);
    jPanel41.add(czechCheckBox, "wrap");

    skatguiLangComboBox.setModel(new DefaultComboBoxModel(new String[] {
          "default", "English", "Deutsch", "Polski", "Français"
        }));
    
    jPanel42.add(jLabel1);
    jPanel42.add(skatguiLangComboBox);
    jPanel42.add(changeLanguageButton, "gap 15");

    jPanel4.add(jPanel41, "wrap");
    jPanel4.add(jPanel42);

    sizeSpinner.setPreferredSize(new Dimension(100, sizeSpinner.getPreferredSize().height));
    sizeSpinner.setValue(new Integer(settings.scalePerc));

    beepCheckbox.setSelected(settings.beep != 0);
    
    String[] cardChoices = {
      rbs("CARDS_" + cardDecks[0]),
      rbs("CARDS_" + cardDecks[1]),      
      rbs("CARDS_" + cardDecks[2]),
      rbs("CARDS_" + cardDecks[3]),      
      rbs("CARDS_" + cardDecks[4]),
    };

    cardSelection = new JComboBox(cardChoices);
    cardSelection.setSelectedIndex(1); // E4

    cardDeckPanel.add(cardSelection);
    cardDeckPanel.setBorder(javax.swing.BorderFactory.
                            createTitledBorder(javax.swing.BorderFactory.createTitledBorder(rbs("Card_Deck"))));

    setupPanel.add(cardDeckPanel);
    
    winSizePanel.add(sizeSpinner);
    winSizePanel.setBorder(javax.swing.BorderFactory.
                           createTitledBorder(javax.swing.BorderFactory.createTitledBorder(rbs("Window_magnification_percentage"))));
    
    setupPanel.add(winSizePanel);

    beepPanel.add(beepCheckbox);
    beepPanel.setBorder(javax.swing.BorderFactory.
                        createTitledBorder(javax.swing.BorderFactory.createTitledBorder(rbs("Timeout_warning"))));

    beepPanel.revalidate();
    
    setupPanel.add(beepPanel, "wrap");

    
    setupPanel.add(jPanel5, "wrap");
    setupPanel.add(jPanel6, "wrap");
    setupPanel.add(jPanel4);

    createTablePanel.setBorder(javax.swing.BorderFactory.
                               createTitledBorder(javax.swing.BorderFactory.createTitledBorder(rbs("Create_Tournament_Table"))));

    createTablePanel.add(tableNameLabel2);
    createTablePanel.add(tableNameInput2, "wrap");
    createTablePanel.add(tableSeedLabel);
    createTablePanel.add(tableSeedInput, "wrap");
    createTablePanel.add(tableNumBlocksLabel);
    createTablePanel.add(tableNumBlocksSpinner, "wrap");
    tableNumBlocksSpinner.setPreferredSize(new Dimension(60, tableNumBlocksSpinner.getPreferredSize().height));
    tableNumBlocksSpinner.setValue(12);
    
    createTablePanel.add(tableSeatLabel1); createTablePanel.add(tableSeatInput1, "wrap");
    createTablePanel.add(tableSeatLabel2); createTablePanel.add(tableSeatInput2, "wrap");
    createTablePanel.add(tableSeatLabel3); createTablePanel.add(tableSeatInput3, "wrap");
    createTablePanel.add(tableSeatLabel4); createTablePanel.add(tableSeatInput4, "wrap");    

    createTablePanel.add(tableCreateButton, "span, center");

    
    createTourPanel.setBorder(javax.swing.BorderFactory.
                              createTitledBorder(javax.swing.BorderFactory.createTitledBorder(rbs("Create_Tournament"))));

    createTourPanel.add(tourNameLabel);
    createTourPanel.add(tourNameInput, "wrap");
    createTourPanel.add(tourSeedLabel);
    createTourPanel.add(tourSeedInput, "wrap");
    createTourPanel.add(tourPasswdLabel);
    createTourPanel.add(tourPasswdInput, "wrap");

    createTourPanel.add(tourNumRoundsLabel);    
    createTourPanel.add(tourNumRoundsSpinner, "wrap");
    tourNumRoundsSpinner.setPreferredSize(new Dimension(60, tourNumRoundsSpinner.getPreferredSize().height));
    tourNumRoundsSpinner.setValue(5);
    
    createTourPanel.add(tourNumRandRoundsLabel);    
    createTourPanel.add(tourNumRandRoundsSpinner, "wrap");
    tourNumRandRoundsSpinner.setPreferredSize(new Dimension(60, tourNumRandRoundsSpinner.getPreferredSize().height));
    tourNumRandRoundsSpinner.setValue(1);    

    createTourPanel.add(tourNumBlocksPerRoundLabel);    
    createTourPanel.add(tourNumBlocksPerRoundSpinner, "wrap");
    tourNumBlocksPerRoundSpinner.setPreferredSize(new Dimension(60, tourNumBlocksPerRoundSpinner.getPreferredSize().height));
    tourNumBlocksPerRoundSpinner.setValue(12);    
    
    createTourPanel.add(tourOnly3tablesLabel);    
    createTourPanel.add(tourOnly3tablesCheckBox, "wrap");

    createTourPanel.add(tourGameTimeLabel);
    createTourPanel.add(tourGameTimeInput, "wrap");

    createTourPanel.add(tourDateLabel);
    createTourPanel.add(tourDateInput, "wrap");

    createTourPanel.add(tourScheduleLabel, "span 2, wrap");
    createTourPanel.add(tourScheduleText, "span, center, growx, wrap");

    createTourPanel.add(tourReg3Button, "span, center, split 4");
    createTourPanel.add(tourReg4Button); 
    createTourPanel.add(tourCompleteButton);
    createTourPanel.add(tourCreateButton, "wrap");

    tourPanel.add(createTourPanel);
    tourPanel.add(createTablePanel);
    
    // tabbed pane
    
    mainTabbedPane.add(rbs("Login"), loginPanel);
    mainTabbedPane.add(rbs("Setup"), setupPanel);
    mainTabbedPane.add(rbs("Tournaments"),  tourPanel);    
    mainTabbedPane.add(rbs("Main"),  mainPanel);

    setContentPane(mainTabbedPane);
  }


  public void resize(float f)
  {
    Misc.msg("resize: " + f);

    setSize((int)(WINDOW_WIDTH*f), (int)(WINDOW_HEIGHT*f));
    invalidate();
    validate();
    
    // adjust table sizes
    
    Font tableFont = userTable.getFont();
    Misc.msg("S=" + tableFont.getSize());
    FontMetrics fm = this.getFontMetrics(tableFont);

    // user table
    
    String[] usrCont = new String[] { "MMMMxxxx", "EDS", "10", "1230" };

    for (int i=0; i < 4; i++) {
      int w = Math.max(fm.stringWidth("I"+usrCont[i]), fm.stringWidth("I"+userTableHeaders[i]));
      userTable.getColumnModel().getColumn(i).setPreferredWidth(w);
    }

    int h = (int)((tableFont.getSize() + 1)* 1.2);
    userTable.setRowHeight(h);
                           
    // table table
    String[] tabCont = new String[] { "MxMxMx", "199", "MxMxMxMx EDS", "MxMxMxMx EDS","MxMxMxMx EDS", "MxMxMxMx EDS" };

    for (int i=0; i < 6; i++) {
      int w = Math.max(fm.stringWidth("I"+tabCont[i]), fm.stringWidth("I"+tableTableHeaders[i]));
      tableTable.getColumnModel().getColumn(i).setPreferredWidth(w);
    }

    // tour table
    String[] tourCont = new String[] { "Edm2010", "5/36/75/3", "join period", "start rd 1 @ 15.06,14:00 MDT", "13" };

    for (int i=0; i < 5; i++) {
      int w = Math.max(fm.stringWidth("I"+tourCont[i]), fm.stringWidth("I"+tourTableHeaders[i]));
      tourTable.getColumnModel().getColumn(i).setPreferredWidth(w);
    }

    // -----

    h = (int)((tableFont.getSize() + 1)* 1.2);
    tableTable.setRowHeight(h);

    int w = userTable.getPreferredSize().width+15;

    userScrollPane.setPreferredSize(new Dimension(w, (int)(100*f))); // doesn't matter, grows y anyway

    w = tableTable.getPreferredSize().width+10;    
    tableScrollPane.setPreferredSize(new Dimension(w, 11*(userTable.getFont().getSize()+2))); // ~ 10 tables

    h = (int)((tableFont.getSize() + 1)* 1.2);
    tourTable.setRowHeight(h);
    tourScrollPane.setPreferredSize(new Dimension(w, 5*(userTable.getFont().getSize()+2))); // ~ 4 tours
    
    // call table panel update

    for (TablePanelBase t : tablePanels) {
      t.resize(f);
    }

    setUserTableListener(); // somehow the BasicTableUI handler gets added - remove it here
      
    invalidate();
    validate();
    
    Misc.msg("SIZE= " + getSize().width + " " + getSize().height);
  }


  void setReg(boolean only3tables) {
    int rounds = 5;
    tourNumRoundsSpinner.setValue(rounds);
    tourNumRandRoundsSpinner.setValue(1);
    tourNumBlocksPerRoundSpinner.setValue(12);
    tourOnly3tablesCheckBox.setSelected(only3tables);

    if (TOUR_TEST)
      tourGameTimeInput.setText("0.0023166"); // seconds
    else
      tourGameTimeInput.setText("2.0");      

    int rs = roundSeconds();

    Calendar now = Calendar.getInstance();

    tourDateInput.setText(now.get(Calendar.YEAR) + "-" + (now.get(Calendar.MONTH)+1) + "-" + now.get(Calendar.DAY_OF_MONTH));

    tourScheduleLabel.setText(rbs("Round_Schedule")+" (" +
                              String.format(locEn, "%d:%02d",
                                            rs / 3600, (rs % 3600) / 60) + rbs("h_per_round_paren_colon"));

    String text = new String(rbs("rd_day_time_etc_break"));

    int start, maxDay, breaks;
    
    if (!TOUR_TEST) {
      
      start = 9*60*60;  // 9:00
      maxDay = 6*60*60; // 6h 
      breaks = 5*60;    // break in between rounds

    } else {

      // test 
      start = now.get(Calendar.HOUR_OF_DAY) * 3600 +
        now.get(Calendar.MINUTE) * 60 +
        now.get(Calendar.SECOND) + 10;
        
      maxDay = 6*60*60;          // 6h
      breaks = 5;                // break in between rounds
    }
    
    int dayDelta = 0;
    int currStart = start;
    int currTotal = 0;

    if (rs > maxDay) maxDay = rs;
    
    for (int i=0; i < rounds; i++) {

      if (currTotal - breaks + rs > maxDay) {
        dayDelta++;
        currTotal = 0;
        currStart = start;
      }

      text += String.format(locEn, "%2d  %d  %02d:%02d:%02d - %02d:%02d:%02d\n",
                            i+1, dayDelta,
                            (currStart / 3600) % 24, (currStart / 60) % 60, currStart % 60,
                            ((currStart+rs) / 3600) % 24, ((currStart + rs) / 60) % 60, (currStart + rs) % 60); 
      currStart += rs + breaks;
      currTotal += rs + breaks;
    }

    tourScheduleText.setText(text);
  }
  
  int roundSeconds()
  {
    int gamesPerRound = (Integer)tourNumBlocksPerRoundSpinner.getValue() *
      (tourOnly3tablesCheckBox.isSelected() ? 3 : 4);

    double t = 0;

    try { t = Double.parseDouble(tourGameTimeInput.getText()); }
    catch (Exception e) { tourGameTimeInput.setText("???"); }

    t *= 60; // seconds per game
    t *= gamesPerRound;

    // t = Math.ceil(t/300)*300; // round up to 5-min multiple
    return (int)t;
  }

  
  JPanel mainPanel, tourPanel;
  JButton aboutButton;

  JLabel changeEmailLabel1;
  JLabel changeEmailLabel2;
  JLabel changeEmailLabel3;
  JTextField changeEmailInput1;
  JTextField changeEmailInput2;
  JTextField changeEmailInput3;
  JButton changeEmailButton;

  JLabel changePasswordLabel1;
  JLabel changePasswordLabel2;
  JLabel changePasswordLabel3;
  JPasswordField changePasswordInput1;
  JPasswordField changePasswordInput2;
  JPasswordField changePasswordInput3;
  JButton changePasswordButton;

  JButton createButton;
  JButton create3Button;
  JButton create4Button;
  JCheckBox czechCheckBox;
  JCheckBox englishCheckBox;
  JCheckBox frenchCheckBox;
  JCheckBox germanCheckBox;
  JCheckBox polishCheckBox;
  JCheckBox spanishCheckBox;
  JButton changeLanguageButton;
  JLabel jLabel1;
  JLabel jLabel2;
  JLabel jLabel3;
  JLabel jLabel3a;  
  JLabel jLabel5;
  JPanel jPanel1;
  JPanel jPanel12;
  JPanel jPanel2;
  JPanel jPanel22;
  JPanel jPanel3;
  JPanel jPanel4 = new JPanel(new MigLayout());
  JPanel jPanel41 = new JPanel(new MigLayout());
  JPanel jPanel42 = new JPanel(new MigLayout());
  JPanel jPanel5 = new JPanel(new MigLayout());
  JPanel jPanel6 = new JPanel(new MigLayout());
  JPanel jPanel7 = new JPanel(new MigLayout());
  JPanel cardDeckPanel = new JPanel(new MigLayout());
  JPanel winSizePanel = new JPanel(new MigLayout());
  JPanel beepPanel = new JPanel(new MigLayout());  
  JPanel createTourPanel = new JPanel(new MigLayout());
  JPanel createTablePanel = new JPanel(new MigLayout());  

  JButton tourReg3Button = new JButton();
  JButton tourReg4Button = new JButton();

  JLabel tourNameLabel = new JLabel();
  JTextField tourNameInput = new JTextField(10);
  JLabel tourSeedLabel = new JLabel();
  JTextField tourSeedInput = new JTextField(10);
  JLabel tourPasswdLabel = new JLabel();
  JTextField tourPasswdInput = new JTextField(10);

  JLabel tableNameLabel2 = new JLabel();
  JTextField tableNameInput2 = new JTextField(10);
  JLabel tableSeedLabel = new JLabel();
  JTextField tableSeedInput = new JTextField(10);
  JLabel tableNumBlocksLabel = new JLabel();
  JSpinner tableNumBlocksSpinner = new JSpinner();
  
  JLabel tourNumRoundsLabel = new JLabel();
  JSpinner tourNumRoundsSpinner = new JSpinner();

  JLabel tourNumBlocksPerRoundLabel = new JLabel();
  JSpinner tourNumBlocksPerRoundSpinner = new JSpinner();

  JLabel tourNumRandRoundsLabel = new JLabel();
  JSpinner tourNumRandRoundsSpinner = new JSpinner();

  JLabel    tourOnly3tablesLabel = new JLabel();
  JCheckBox tourOnly3tablesCheckBox = new JCheckBox();

  JLabel tourGameTimeLabel = new JLabel();
  JTextField tourGameTimeInput = new JTextField(4);
  JLabel tourMinLabel = new JLabel();

  JLabel tourDateLabel = new JLabel();
  JTextField tourDateInput = new JTextField(10);
  
  JLabel tourScheduleLabel = new JLabel();
  JTextArea tourScheduleText = new JTextArea(12, 20);

  JButton tourCreateButton = new JButton();
  JButton tourCompleteButton = new JButton();
  //tourCompleteButton.setEnabled(false);
  JButton tableCreateButton = new JButton();  
  
  JLabel tableSeatLabel1 = new JLabel();
  JTextField tableSeatInput1 = new JTextField(10);
  JLabel tableSeatLabel2 =  new JLabel();
  JTextField tableSeatInput2 = new JTextField(10);
  JLabel tableSeatLabel3 = new JLabel();
  JTextField tableSeatInput3 = new JTextField(10);
    JLabel tableSeatLabel4 = new JLabel();
  JTextField tableSeatInput4 = new JTextField(10);
  
  JScrollPane userScrollPane;
  JScrollPane tableScrollPane;
  JScrollPane tourScrollPane;  
  JScrollPane lobbyPane;
  JTextArea lobbyTextArea;
  JScrollPane logPane;
  JTextArea logTextArea;
  JButton loginButton;
  JPanel loginDataPanel = new JPanel(new MigLayout());
  JPanel loginPanel = new JPanel(new MigLayout());
  JTextField mainInput;
  JTabbedPane mainTabbedPane;
  JTabbedPane messageTabbedPane;
  JPasswordField passwordInput;
  JLabel passwordLabel;
  JComboBox portCombobox, hostCombobox;
  JButton quitButton;
  JButton ratingsButton;
  JButton showTableButton;
  JPanel setupPanel = new JPanel(new MigLayout());
  JComboBox skatguiLangComboBox;
  JCheckBox storeLoginCheckbox, asWindowCheckbox;
  JTextField tableNameInput;
  JTextField tablePasswordInput;
  JTable tableTable;
  JTable tourTable;
  JButton talkButton;
  JTextField userIdInput;
  JLabel userIdLabel;
  JButton userInfoButton;
  JButton helpButton;  
  JTable userTable;
  JSpinner sizeSpinner;
  JCheckBox beepCheckbox;
  Font origTableFont;
  String[] userTableHeaders, tableTableHeaders, tourTableHeaders;

  JComboBox cardSelection;

  void reportHandlers(String s)
  {
    MouseListener[] mls2 = (userTable.getListeners(MouseListener.class));
      
    for (MouseListener ml : mls2) {
      Misc.msg("ML activated " + s + ": " + ml);
    }
  }

  public int getOrigHeight() { return WINDOW_HEIGHT; }
  public int getOrigWidth() { return WINDOW_WIDTH; }  
  
  private Locale locEn = new Locale("en"); // for format
  private boolean TOUR_TEST = true;
}

