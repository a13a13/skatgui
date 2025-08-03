package gui;

import common.*;
import java.util.*;
import java.lang.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.table.*;

import net.miginfocom.swing.MigLayout;

public class InviteWindow extends ResizeFrame
{
  JFrame frame;
  Table table;
  int WINDOW_WIDTH, WINDOW_HEIGHT;
  TournamentResultTable2 resultTable;
  ResultTableTotals resultTableTotals;
  float lastf = 1.0f;
    
  TablePanel tp; // for bundle, invite message, talk window
  //final static String resourceFile = "data/i18n/gui/Lists";
  String[] userTableHeaders;
  JTable userTable;
  UserTableModel userTableModel;
  
  String rbs(String s) {
    try {
      return tp.bundle.getString(s);
    }
    catch (Throwable e) {
      return s;
    }
  }

  String rbs0(String s) { return s; } // for development
  
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

  void setUserTableListener()
  {
    // replace existing mouse listeners by customized one
    
    MouseListener[] mls = (userTable.getListeners(MouseListener.class));

    for (MouseListener ml : mls) {
      userTable.removeMouseListener(ml);
    }

    userTable.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e)
        {
          Point p = e.getPoint();
          int row = userTable.rowAtPoint(p);
          int column = userTable.columnAtPoint(p); // This is the view column!
          
          ListSelectionModel model = userTable.getSelectionModel();

          if (true || SwingUtilities.isLeftMouseButton(e)) {

            // left click: toggle selection

            String sel = (String)userTable.getModel().getValueAt(row, 3);
            if (sel.equals("")) {
              sel = "1";
              model.addSelectionInterval(row, row);
            } else if (sel.equals("1")) {
              sel = "2";
            } else if (sel.equals("2")) {
              sel = "";
              model.removeSelectionInterval(row, row);
            }

            userTable.getModel().setValueAt(sel, row, 3);
            
          } else {

            // others: clear selection
            model.clearSelection();
          }
        }
      });
  }

  public InviteWindow(JFrame frame, TablePanel tp, 
                      Table table, int WINDOW_WIDTH, int WINDOW_HEIGHT)
  {
    this.frame = frame;
    this.tp = tp;
    this.table = table;
    this.WINDOW_WIDTH = WINDOW_WIDTH;
    this.WINDOW_HEIGHT = WINDOW_HEIGHT;  
	
    setTitle(rbs("Invite_Players_Table") + " " + table.getId());
    setResizable(false);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    update((String[][])null);
    setVisible(false);
  }
    
  public void update(String[][] userData)
  {
    JPanel panel = new JPanel(new MigLayout("center", "[grow]", "[grow][]"));
    //    JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
    //                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    JPanel namePanel = new JPanel(new MigLayout("center", "[grow]", "[grow][]"));

    // create user table
    
    userTable = new JTable();

    userTableHeaders = new String[] {
      rbs("User"),
      rbs("Languages"),
      rbs("Rating"),
      rbs("Invited")
    };

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
    
    userTableModel = new UserTableModel();
    userTable.setModel(userTableModel);
    userTable.getColumnModel().getColumn(1).setCellRenderer(userCenter);
    userTable.getColumnModel().getColumn(2).setCellRenderer(userRight);
    userTable.getColumnModel().getColumn(3).setCellRenderer(userCenter);

    setUserTableListener();
    
    if (userData != null) {
    
      for (int i=0; i < userData.length; i++) {

        userTableModel.addRow(new String[] {
            userData[i][0], userData[i][1],
            userData[i][2], "" });
      }
    }

    JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    scroll.setViewportView(userTable);
      
    JButton inviteButton = new JButton(rbs("Invite"));
    JButton cancelButton = new JButton(rbs("Cancel"));    

    inviteButton.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent evt) {
          Misc.msg("invite clicked");

          StringBuffer sb = new StringBuffer();
    
          int[] rowIndices = userTable.getSelectedRows();
          if (rowIndices != null) {
            for (int i=0; i < rowIndices.length; i++) {
              if (((String)userTable.getModel().getValueAt(rowIndices[i], 3)).equals("2")) {
                sb.append(" " + (String)userTable.getModel().getValueAt(rowIndices[i], 0));
              }
              sb.append(" " + (String)userTable.getModel().getValueAt(rowIndices[i], 0));                  }
          }

          tp.send("invite " + sb.toString());
          tp.textArea.append("\n# @invite" + sb.toString());
          
          setVisible(false);
        }});

    cancelButton.addMouseListener(new MouseAdapter() {
        public void mouseClicked(MouseEvent evt) {
          Misc.msg("cancel clicked");
          setVisible(false);
        }});

    // panel.add(scroll,     "h 75%, growx, span, wrap");
    panel.add(scroll, "growx, span, wrap");
    panel.add(inviteButton, "center");
    panel.add(cancelButton, "center");    
    setContentPane(panel);
    resize(lastf);
  }
    
  public void resize(float f)
  {
    lastf = f;
    // Misc.msg("RESIZE " + (int)(WINDOW_WIDTH*f) + " " + (int)(WINDOW_HEIGHT*f));
    setSize((int)(WINDOW_WIDTH*f), (int)(WINDOW_HEIGHT*f));
	

    Font f1 = new Font("Dialog", 1, Math.round(11*f));
    // legend.setFont(f1);
	
    invalidate();
    validate();
    // ((TablePanelBase)getContentPane()).resize(f);
    repaint();
  }
}
