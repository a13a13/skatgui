// public client information

// (c) Michael Buro, licensed under GPLv3

package common;

public class UserInfo
{
  public String id;
  public int permission;
  public double rating;

  public String toString() { return id + " " + permission + " " + rating; }

  public void fromReader(SReader r) {
    id = r.nextWord();
    if (id == null) Misc.err("ClientInfo id missing");

    String s = r.nextWord();
    if (s == null) Misc.err("ClientInfo permission missing");    
    permission = Integer.parseInt(s);
    
    s = r.nextWord();
    if (s == null) Misc.err("ClientInfo rating missing");    
    rating = Double.parseDouble(s);
  }
}
