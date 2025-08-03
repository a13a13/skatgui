/* Prints current totals for series under tournament-style result table encoded by
 * TournamentResultTable2.java.
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

public class ResultTableTotals extends javax.swing.JTable
{
    ArrayList<String> resultTableHeaders = new ArrayList<String>();
    Table table;
    java.util.ResourceBundle bundle;
    final static String resourceFile = "data/i18n/gui/Lists";

    public ResultTableTotals(Table table) {
	this.table = table;
	bundle = java.util.ResourceBundle.getBundle(resourceFile);
	initComponents();
    }

    String rbs0(String s) { return s; }
    String rbs(String s) { return bundle.getString(s); }

    public void initComponents() {

	int numPlayers = table.getPlayerNum();
	ScoreSheet scores = table.getScoreSheet();

	resultTableHeaders.add(rbs("Totals"));

	for (int index = 0; index < numPlayers; index++) {
	    resultTableHeaders.add(scores.names[index]);
	    resultTableHeaders.add(rbs("W"));
	    resultTableHeaders.add(rbs("L"));
	    resultTableHeaders.add(rbs("T"));
	}

	resultTableHeaders.add(rbs("P"));

	Object[] tableHeaders = resultTableHeaders.toArray();

	int numRows = 5;
	int numCols = tableHeaders.length;

	Object[][] rows = new Object[numRows][];

	for (int index = 0; index < numRows; index++)
	    rows[index] = new Object[numCols];

	this.setModel(new javax.swing.table.DefaultTableModel(rows, tableHeaders) {

		@Override
		    public Class getColumnClass(int columnIndex) { return String.class; }

		@Override
		    public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
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

	getColumnModel().getColumn(0).setCellRenderer(left);
	
	for (int index = 0; index < numPlayers; index++) {
	    for (int index2 = 1; index2 <= 4; index2++) {
		getColumnModel().getColumn(index*4 + index2).setCellRenderer(right);
	    }
	}

	getColumnModel().getColumn(getColumnModel().getColumnCount() - 1).setCellRenderer(right);

	invalidate();
	validate();

	TableModel tm = getModel();

	tm.setValueAt(rbs("Totals"), 0, 0);
	tm.setValueAt(rbs("Won_Less_Lost_by_50"), 1, 0);

	if (numPlayers == 3)
	    tm.setValueAt(rbs("Opponent_Loss_by_40"), 2, 0);
	else
	    tm.setValueAt(rbs("Opponent_Loss_by_30"), 2, 0);

	tm.setValueAt(rbs("Timeouts"), 3, 0);
	tm.setValueAt(rbs("End_Result"), 4, 0);

	if (scores.size() > 0) {
	    ScoreSheet.Row row = scores.getRow(scores.size() - 1);

	    for (int index = 0; index < numPlayers; index++) {
		tm.setValueAt(row.cumulative[index].score, 0, index*4 + 1);
		tm.setValueAt(row.cumulative[index].wins, 0, index*4 + 2);
		tm.setValueAt(row.cumulative[index].losses, 0, index*4 + 3);
		tm.setValueAt(row.cumulative[index].penalties, 0, index*4 + 4);
		
		tm.setValueAt(scores.winLossRow.cumulative[index].score, 1, index*4 + 1);
		
		tm.setValueAt(scores.defWinsRow.cumulative[index].score, 2, index*4 + 1);
	    
		tm.setValueAt(scores.timeoutRow.cumulative[index].score, 3, index*4 + 1);
		
		tm.setValueAt(scores.totalsRow.cumulative[index].score, 4, index*4 + 1);
	    }

	    tm.setValueAt(row.cumuPass, 0, this.getColumnModel().getColumnCount() - 1);
	}
    }

    public void resize(float f) {
	Font f2 = new Font("Dialog", 1, Math.round(12*f));
	setFont(f2);
	FontMetrics fm = this.getFontMetrics(f2);

	ArrayList<String> columnWidths = new ArrayList<String>();

	/* The first column of the table with the totals should be as wide as the
	   first five columns of the main result table, so that the player names and
	   their respective totals line up with the same names and totals in the
	   main table. */
	columnWidths.add("88M99M99MMxxxxxxxxMMxxxxM");
	
	for (int index = 0; index < table.getPlayerNum(); index++) {
	    columnWidths.add("MMMMMM");
	    columnWidths.add("88");
	    columnWidths.add("88");
	    columnWidths.add("8");
	}

	columnWidths.add("88");

	for (int index = 0; index < this.getColumnModel().getColumnCount(); index++) {

	    int width = Math.max(fm.stringWidth("I" + columnWidths.get(index)),
				 fm.stringWidth("I" + resultTableHeaders.get(index)));

	    this.getColumnModel().getColumn(index).setPreferredWidth(width);
	    this.getColumnModel().getColumn(index).setIdentifier(index);
	}

	int height = (int)Math.round((f2.getSize() + 1) * 1.1);
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