package gui;

import common.*;
import java.util.*;
import java.lang.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import net.miginfocom.swing.MigLayout;

public class ListWindow2 extends ResizeFrame
{
    JFrame frame;
    Table table;
    int WINDOW_WIDTH, WINDOW_HEIGHT;
    TournamentResultTable2 resultTable;
    ResultTableTotals resultTableTotals;
    float lastf = 1.0f;
    JLabel legend;
    JPanel innerPanel1;
    JPanel innerPanel2;
    
    java.util.ResourceBundle bundle;
    final static String resourceFile = "data/i18n/gui/Lists";
    
    public ListWindow2(JFrame frame, Table table, int WINDOW_WIDTH, int WINDOW_HEIGHT)
    {
	this.frame = frame;
	this.table = table;
	this.WINDOW_WIDTH = WINDOW_WIDTH;
	this.WINDOW_HEIGHT = WINDOW_HEIGHT;  
	bundle = java.util.ResourceBundle.getBundle(resourceFile);
	
	setTitle(rbs("Score_Sheet_Table") + table.getId());
	setResizable(false);
	setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
	// setLocationRelativeTo(frame);
	update();
	setVisible(false);
    }

    String rbs0(String s) { return s; }
    String rbs(String s) { return bundle.getString(s); }

    public void update()
    {
	JPanel panel = new JPanel(new MigLayout("debug, center", "[grow]", "[grow][]"));
	JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	JScrollPane scroll2 = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER,
					      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	
	resultTable = new TournamentResultTable2(table);
	scroll.setViewportView(resultTable);
	
	resultTableTotals = new ResultTableTotals(table);
	scroll2.setViewportView(resultTableTotals);
	
	JButton hideButton = new JButton("Hide");
	hideButton.addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent evt) {
		    Misc.msg("hide clicked");
		    setVisible(false);
		}});
	
	legend = new JLabel();
	StringBuffer legendText = new StringBuffer();
	
	legendText.append("<html><left><u>");
	legendText.append(rbs("Legend"));
	legendText.append("</u><br>");
	legendText.append("<font color=\"red\">");
	legendText.append(rbs("B"));
	legendText.append(" = ");
	legendText.append(rbs("BaseValue"));
	legendText.append(rbs("M"));
	legendText.append(" = ");
	legendText.append(rbs("Matadors_Explanation"));
	legendText.append("</font><br>");
	legendText.append("<font color=\"blue\">");
	legendText.append(rbs("Modifiers_Colon"));
	legendText.append(rbs("H"));
	legendText.append(" = ");
	legendText.append(rbs("Hand_Semicolon"));
	legendText.append(rbs("S"));
	legendText.append(" = ");
	legendText.append(rbs("Schneider_Semicolon"));
	legendText.append(rbs("Z"));
	legendText.append(" = ");
	legendText.append(rbs("Schwarz_Space"));
	legendText.append("(A = ");
	legendText.append(rbs("Schneider_Ann_Exp"));
	legendText.append("B = ");
	legendText.append(rbs("Schwarz_Ann_Exp"));
	legendText.append(rbs("O"));
	legendText.append(" = ");
	legendText.append(rbs("Ouvert_Semicolon"));
	legendText.append(rbs("V"));
	legendText.append(" = ");
	legendText.append(rbs("Overbid."));
	legendText.append("</font><br>");
	legendText.append("<font color=\"green\">");
	legendText.append(rbs("W"));
	legendText.append(" = ");
	legendText.append(rbs("Win_Semicolon"));
	legendText.append(rbs("L"));
	legendText.append(" = ");
	legendText.append(rbs("Loss_Semicolon"));
	legendText.append(rbs("T"));
	legendText.append(" = ");
	legendText.append(rbs("Timeout_Semicolon"));
	legendText.append(rbs("P"));
	legendText.append(" = ");
	legendText.append(rbs("All_Passed"));
	legendText.append("</font></left></html>");
	
	legend.setText(legendText.toString());

	innerPanel1 = new JPanel();
	innerPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

	innerPanel2 = new JPanel();
	innerPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

	resize(lastf);

	int tableHeight1 = Math.min((resultTable.getRowCount() + 1)*resultTable.getRowHeight(), 490);

	innerPanel1.add(scroll,       new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0,
											(int)(innerPanel1.getSize().getWidth()), tableHeight1));

	// innerPanel1.add(scroll,    new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0,
	//									     (int)(resultTable.getPreferredSize().getWidth()), tableHeight1));

	int tableHeight2 = (resultTableTotals.getRowCount() + 1)*resultTableTotals.getRowHeight();

	innerPanel2.add(scroll2,     new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0,
										       (int)(innerPanel2.getSize().getWidth()), tableHeight2));

	// innerPanel2.add(scroll2,  new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0,
	//									    (int)(resultTableTotals.getPreferredSize().getWidth()), tableHeight2));
	
	panel.add(innerPanel1,     "h 75%, shrinkx, center, wrap");
	panel.add(innerPanel2,     "h 18%, shrinkx, center, wrap");
	panel.add(legend,          "h 10%, growx, span, wrap");
	panel.add(hideButton,     "center");
	setContentPane(panel);
    }

  public void resize(float f)
  {
    lastf = f;
    // Misc.msg("RESIZE " + (int)(WINDOW_WIDTH*f) + " " + (int)(WINDOW_HEIGHT*f));
    setSize((int)(WINDOW_WIDTH*f), (int)(WINDOW_HEIGHT*f));

    Font f1 = new Font("Dialog", 1, Math.round(11*f));
    legend.setFont(f1);

    resultTable.resize(f);
    resultTableTotals.resize(f);

    int innerPanel1Width = 0;
    int innerPanel1Height;

    for (int index = 0; index < resultTable.getColumnModel().getColumnCount(); index++) {
	innerPanel1Width += resultTable.getColumn(index).getMinWidth();
    }

    innerPanel1Height = Math.min(resultTable.getModel().getRowCount()*resultTable.getRowHeight(), 490);

    resultTable.setSize(new Dimension(innerPanel1Width, innerPanel1Height));
    innerPanel1.setSize(innerPanel1Width, innerPanel1Height);
    innerPanel1.setMaximumSize(new Dimension(innerPanel1Width, innerPanel1Height));
    innerPanel1.setMinimumSize(new Dimension(innerPanel1Width, innerPanel1Height));

    int innerPanel2Width = 0;
    int innerPanel2Height;

    for (int index = 0; index < resultTableTotals.getColumnModel().getColumnCount(); index++) {
	innerPanel2Width += resultTableTotals.getColumn(index).getMinWidth();
    }

    innerPanel2Height = resultTableTotals.getModel().getRowCount()*resultTableTotals.getRowHeight();

    resultTableTotals.setSize(new Dimension(innerPanel2Width, innerPanel2Height));
    innerPanel2.setSize(innerPanel2Width, innerPanel2Height);
    innerPanel2.setMaximumSize(new Dimension(innerPanel2Width, innerPanel2Height));
    innerPanel2.setMinimumSize(new Dimension(innerPanel2Width, innerPanel2Height));
    
    invalidate();
    validate();
    // ((TablePanelBase)getContentPane()).resize(f);
    repaint();
  }
}
