/* $Id: JHistTextField.java 4828 2006-08-27 06:28:58Z orts_mburo $
   
   (c) Michael Buro, licensed under GPLv3
*/

package common;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JHistTextField extends JTextField implements KeyListener,FocusListener
{
  static final int HIST_NUM = 50;
  protected String[] history = new String[HIST_NUM];
  protected int hist_index = 0;
  protected boolean has_focus = false;
  
  public JHistTextField() { init(); }
  public JHistTextField(int a) { super(a); init(); }
  public JHistTextField(String a) { super(a); init(); }
  public JHistTextField(String a, int b) { super(a,b); init(); }      

  public void init() 
  { 
    addFocusListener(this); addKeyListener(this); 
  }

  public boolean has_focus() { return this.has_focus; }
   
  public void focusGained(FocusEvent e) 
  {
    has_focus = true;
  }

  public void focusLost(FocusEvent e) 
  {
    has_focus = false;
  }

  public void keyPressed(KeyEvent e) 
  {
    int code = e.getKeyCode();
    String s;

    if (code == KeyEvent.VK_ESCAPE) 
      setText("");

    else if (code == KeyEvent.VK_UP) {

      if (hist_index == 0)
	history[0] = getText();

      if (hist_index < history.length-1 && history[hist_index+1] != null) {
	hist_index++;
	s = history[hist_index];
	if (s == null) s = "";
	setText(s);
	setCaretPosition(getDocument().getLength());
      } else
	Toolkit.getDefaultToolkit().beep();
      
    } else if (code == KeyEvent.VK_DOWN) {      

      if (hist_index > 0) {
	hist_index--;
	s = history[hist_index];
	if (s == null) s = "";
	setText(s);
	setCaretPosition(getDocument().getLength());
      } else
	Toolkit.getDefaultToolkit().beep();
    }    
  }

  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) {}  

  public void push()
  {
    // Global.dbgmsg("PUSH: " + getText());
    
    // append string to history
    
    if (!getText().equals(history[1])) {
      for (int i=history.length-2; i >= 0; i--)
	history[i+1] = history[i];
      history[1] = getText();
    }
    
    hist_index = 0;
    setText("");
  }
 
}
