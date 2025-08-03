/* $Id: LimitedTextArea.java 4779 2006-08-21 04:25:49Z orts_mburo $
   
   (c) Michael Buro, licensed under GPLv3
*/

package common;

import javax.swing.*;

public class JLimitedTextArea extends JTextArea
{
  private int limit = 10000;

  public JLimitedTextArea() { super(); }
  public JLimitedTextArea(int a, int b) { super(a, b); }
  public JLimitedTextArea(String s) { super(s); }
  public JLimitedTextArea(String s, int a, int b) { super(s, a, b); }

  public void setLimit(int l) { limit = l; }

  private void truncate()
  {
    String s = getText();
    int l = s.length();
    if (l > limit) setText(s.substring(l-limit, l));
  }

  public void clear()
  {
    setText("");
  }

  public void append(String s)
  {
    int l1 = s.length();
    int l2 = getText().length();

    if (l1+l2 <= limit)
      super.append(s);
    else {
      // super.setText((getText() + s).substring(l1+l2-limit, l1+l2));
      super.append(s);
      super.replaceRange("", 0, l1+l2-limit);
    }
  }

  public void insert(String s, int a)
  {
    super.insert(s,a);
    truncate();
  }

  public void replaceRange(String s, int start, int end)
  {
    super.replaceRange(s,start,end);
    truncate();
  }

  public void setText(String s)
  {
    int l = s.length();
    if (l <= limit)
      super.setText(s);
    else
      super.setText(s.substring(l-limit, l));
  }

}
