/* $Id: Geometry.java 4878 2006-08-29 09:10:42Z orts_mburo $
   
   (c) Michael Buro, licensed under the GPL2
*/

package common;

import java.util.*;

public class Geometry
{
  private int w, h, x, y;

  public int get_w() { return w; }
  public int get_h() { return h; }
  public int get_x() { return x; }
  public int get_y() { return y; }

  public Geometry(int w, int h, int x, int y)
  {
    this.w = w;
    this.h = h;
    this.x = x;
    this.y = y;
  }
  
  public static Geometry valueOf(String s) throws IllegalArgumentException
  {
    boolean ok = true;
    StringTokenizer st = new StringTokenizer(s);
    String estr = "Geometry.valueOf(String): ";
    int w=0, h=0, x=0, y = 0;

    for (;;) {
      
      if (!st.hasMoreTokens()) { ok = false; estr += "no w"; break; }
      w = Integer.parseInt(st.nextToken());
      
      if (!st.hasMoreTokens()) { ok = false; estr += "no h"; break; }
      h = Integer.parseInt(st.nextToken());
      
      if (!st.hasMoreTokens()) { ok = false; estr += "no x"; break; }
      x = Integer.parseInt(st.nextToken());
      
      if (!st.hasMoreTokens()) { ok = false; estr += "no y"; break; }
      y = Integer.parseInt(st.nextToken());

      if (st.hasMoreTokens()) { ok = false; estr += "too many args"; break; }
      break;
    }

    if (!ok) throw new IllegalArgumentException(estr);

    return new Geometry(w,h,x,y);
  }
  
  public String toString()
  {
    return
      String.valueOf(w) + " " +
      String.valueOf(h) + " " +
      String.valueOf(x) + " " +
      String.valueOf(y);
  }
}

