/*
 *  A simple class for storing moves
 *
 * @author Jeff Long
*/


package common;

public class Move implements java.io.Serializable {

  public int source;     // The player who made the move (-1: world)
  public String action;

  public Move() { }
  
  public Move(int s, String a) {
    source = s;
    action = a;
  }

  public String toString() {

    String value = null;
    value = source + " " + action;
    return value;

  }

  public static void serialize(Move m, StringBuffer sb)
  {
    sb.append("\nMove");
    if (m == null) { sb.append("Null "); return; }
    sb.append(" " + m.source + " ");
    sb.append(m.action + " ");
    if (m.action == null || m.action.isEmpty())
      Misc.err("move action empty");
  }    
}
