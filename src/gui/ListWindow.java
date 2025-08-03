package gui;

import common.*;
import java.util.*;
import java.lang.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import net.miginfocom.swing.MigLayout;

public class ListWindow extends ResizeFrame
{
  JFrame frame;
  Table table;
  int WINDOW_WIDTH, WINDOW_HEIGHT;
  TournamentResultTable2 resultTable;
  ResultTableTotals resultTableTotals;
  float lastf = 1.0f;
  JLabel legend;
    
  java.util.ResourceBundle bundle;
  final static String resourceFile = "data/i18n/gui/Lists";
    
  public ListWindow(JFrame frame, Table table, int WINDOW_WIDTH, int WINDOW_HEIGHT)
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
    JPanel panel = new JPanel(new MigLayout("center", "[grow]", "[grow][]"));
    JScrollPane scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	
    JScrollPane scroll2 = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	
    resultTable = new TournamentResultTable2(table);
    scroll.setViewportView(resultTable);
	
    resultTableTotals = new ResultTableTotals(table);
    scroll2.setViewportView(resultTableTotals);
	
    JButton hideButton = new JButton(rbs("Hide"));
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
	
    panel.add(scroll,     "h 75%, growx, span, wrap");
    panel.add(scroll2,    "h 18%, growx, span, wrap");
    panel.add(legend,     "h 10%, span, wrap");
    panel.add(hideButton, "center");
    setContentPane(panel);
    resize(lastf);
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
	
    invalidate();
    validate();
    // ((TablePanelBase)getContentPane()).resize(f);
    repaint();
  }
}
