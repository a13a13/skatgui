package gui;

import common.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import net.miginfocom.swing.MigLayout;

public class TournamentResultTable2 extends javax.swing.JTable
{
  String[] resultTableHeaders;
  Table table;
  java.util.ResourceBundle bundle;
  final static String resourceFile = "data/i18n/gui/Lists";

    public TournamentResultTable2(Table table) {
	this.table = table;
	bundle = java.util.ResourceBundle.getBundle(resourceFile);
	initComponents();
    }

  String rbs0(String s) { return s; }
  String rbs(String s) { return bundle.getString(s); }  

  public void initComponents() {

    int pn = table.getPlayerNum();

    if (pn == 3) {

      resultTableHeaders = new String[] {
        "#",
        rbs("B"),
        rbs("M"),
        rbs("Modifiers"),
        rbs("Score"),
	table.getScoreSheet().names[0],
        rbs("W"),
        rbs("L"),
        rbs("T"),        
        table.getScoreSheet().names[1],
        rbs("W"),
        rbs("L"),
        rbs("T"),        
        table.getScoreSheet().names[2],
        rbs("W"), 
        rbs("L"), 
        rbs("T"), 
        rbs("P")  
      };

    } else {

      resultTableHeaders = new String[] {
        "#",
        rbs("B"),
        rbs("M"),
        rbs("Modifiers"),
        rbs("Score"),
        table.getScoreSheet().names[0],
        rbs("W"),
        rbs("L"),
        rbs("T"),
        table.getScoreSheet().names[1],
        rbs("W"),
        rbs("L"),
        rbs("T"),        
        table.getScoreSheet().names[2],
        rbs("W"), // for "wins"
        rbs("L"), // for "losses"
        rbs("T"), // for "timeouts" 
        table.getScoreSheet().names[3],
        rbs("W"), // for "wins"
        rbs("L"), // for "losses"
        rbs("T"), // for "timeouts" 
        rbs("P")  // passed
      };
    }

    int rowN = table.getScoreSheet().size();
    if (table.getMaxGameNum() >= 0) rowN = table.getMaxGameNum();
    int colN = resultTableHeaders.length;
    
    Object[][] rows = new Object[rowN][];

    for (int i=0; i < rowN; i++) {
      rows[i] = new Object[colN];
    }

    this.setModel(new javax.swing.table.DefaultTableModel(rows, resultTableHeaders) {

        @Override
          public Class getColumnClass(int columnIndex) {
          return String.class;
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

    getColumnModel().getColumn(0).setCellRenderer(center);
    getColumnModel().getColumn(1).setCellRenderer(right);    
    getColumnModel().getColumn(2).setCellRenderer(right);
    getColumnModel().getColumn(3).setCellRenderer(right);
    getColumnModel().getColumn(4).setCellRenderer(right);    
    for (int i=0; i < pn; i++) {
      getColumnModel().getColumn(4*i+5).setCellRenderer(right);
      getColumnModel().getColumn(4*i+6).setCellRenderer(right);
      getColumnModel().getColumn(4*i+7).setCellRenderer(right);
      getColumnModel().getColumn(4*i+8).setCellRenderer(right);
    }
    getColumnModel().getColumn(4*pn+5).setCellRenderer(right);

    invalidate();
    validate();

    // fills first column with game numbers
    for (int index = 0; index < rowN; index++) {
      getModel().setValueAt(index+1, index, 0);
    }

    // fills rows with game data

    ScoreSheet ss = table.getScoreSheet();
    int n = ss.size();
    TableModel tm = getModel();
    StringBuffer m = new StringBuffer();
        
    for (int i=0; i < n; i++) {

      ScoreSheet.Row r = ss.getRow(i);
      int decl = r.declarer;
      if (decl >= 0) {
        
        int di = (decl + i + 1) % table.getPlayerNum();
        tm.setValueAt(r.cumulative[di].score, i, di*4+5);
        if (r.score > 0)
          tm.setValueAt(r.cumulative[di].wins, i, di*4+6);
        else
          tm.setValueAt(r.cumulative[di].losses, i, di*4+7);

        tm.setValueAt(r.baseValue, i, 1);
        tm.setValueAt(r.matadors,  i, 2);

        m.delete(0, m.length());
        if (r.hand)               m.append(rbs("H"));

	if (r.schneiderAnnounced)
	    // if schneider was announced, regardless of won or lost, represented by "A"
	    m.append("A");
	else {
	    if (r.schneider)
		m.append("S");
	}

	if (r.schwarzAnnounced)
	    // if schwarz was announced, regardless of won or lost, represented by "B"
	    m.append("B");
	else {
	    if (r.schwarz)
		m.append("Z");
	}

	/* no longer distinguishing between announced + won and announced + lost for schn. and schw.
	if (r.schneider) {
	    // declarer earned schneider
	    m.append("S");
	    if (r.schneiderAnnounced)
		m.append("A");
	} else {
	    // declarer did not earn schneider, but might have announced it
	    if (r.schneiderAnnounced)
		m.append("SB");
	}

	if (r.schwarz) {
	    // declarer earned schwarz
	    m.append("Z");
	    if (r.schwarzAnnounced)
		m.append("A");
	} else {
	    // declarer did not earn schwarz, but might have announced it
	    if (r.schwarzAnnounced)
		m.append("ZB");
	}
	*/

        if (r.open)               m.append("O");
        if (r.overbid)            m.append(rbs("V"));
        tm.setValueAt(m.toString(), i, 3);
        tm.setValueAt(r.score, i, 4);        
        
      } else {

        // pass
        tm.setValueAt("-", i, 1); // base        
        tm.setValueAt(r.cumuPass, i, table.getPlayerNum()*4 + 5);
      }

      // penalties

      if (r.p0 > 0) {
        int pi = (i + 0 + 1) % table.getPlayerNum();
        tm.setValueAt(r.cumulative[pi].penalties, i, pi*4+8);
      }
      if (r.p1 > 0) {
        int pi = (i + 1 + 1) % table.getPlayerNum();
        tm.setValueAt(r.cumulative[pi].penalties, i, pi*4+8);
      }
      if (r.p2 > 0) {
        int pi = (i + 2 + 1) % table.getPlayerNum();
        tm.setValueAt(r.cumulative[pi].penalties, i, pi*4+8);
      }
    }

    int row = table.getScoreSheet().size() - 1;

    // sets focus to the last row of the table, in case it's past the end of the scrollbar
    if (row >= 0) {
	this.changeSelection(row, 0, false, false);
	this.scrollRectToVisible(this.getCellRect(row, 0, true));
    }
  }

  public void resize(float f)
  {
    Font f2 = new Font("Dialog", 1, Math.round(12*f));
    setFont(f2);
    FontMetrics fm = this.getFontMetrics(f2);
    
    String[] columnWidths;

    if (table.getScoreSheet().playerNum == 3) {
      columnWidths = new String[] {
        "88", "99", "99", "HSAZAOVX", "-240",
        "MMMMMMM", "88", "88", "8",
        "MMMMMMM", "88", "88", "8",
        "MMMMMMM", "88", "88", "8",
        "88"
      };
    } else {
      columnWidths = new String[] {
        "88", "99", "99", "HSAZAOVX", "-240",
        "MMMMMMM", "88", "88", "8",
        "MMMMMMM", "88", "88", "8",
        "MMMMMMM", "88", "88", "8",
        "MMMMMMM", "88", "88", "8",        
        "88"
      };
    }

    for (int index = 0; index < this.getColumnModel().getColumnCount(); index++) {
      int width = Math.max(fm.stringWidth("I"+columnWidths[index]),
                           fm.stringWidth("I"+resultTableHeaders[index]));

      this.getColumnModel().getColumn(index).setPreferredWidth(width);
      this.getColumnModel().getColumn(index).setIdentifier(index);
    }

    int height = (int)Math.round((f2.getSize() + 1)*1.1);
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
