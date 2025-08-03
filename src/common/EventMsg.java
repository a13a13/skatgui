/* $Id: EventMsg.java 5943 2007-10-06 18:41:01Z mburo $
   
   (c) Michael Buro, licensed under the GPL2
*/

package common;

import java.lang.Error;
import java.util.*;

public class EventMsg
{
  private String sender = null;
  private Vector<String> args = null;

  public EventMsg(String sender)
  {
    if (sender == null) throw new Error("sender null");
    this.sender = sender;
    args = new Vector<String>();
  }
  
  public EventMsg(String sender, String s1)
  {
    if (sender == null) throw new Error("sender null");
    this.sender = sender;
    args = new Vector<String>();
    if (s1 == null) throw new Error("string null");
    args.add(s1);
  }

  public EventMsg(String sender, String s1, String s2)
  {
    if (sender == null) throw new Error("sender null");
    this.sender = sender;
    args = new Vector<String>();
    if (s1 == null || s2 == null) throw new Error("string null");
    args.add(s1);
    args.add(s2);
  }

  public EventMsg(String sender, String s1, String s2, String s3)
  {
    if (sender == null) throw new Error("sender null");
    this.sender = sender;
    args = new Vector<String>();
    if (s1 == null || s2 == null || s3 == null)
      throw new Error("string null");
    args.add(s1);
    args.add(s2);
    args.add(s3);
  }

  public EventMsg(String sender, Vector<String> args) 
  {
    if (sender == null) throw new Error("sender null");
    if (args == null) throw new Error("args null");
    this.sender = sender;
    this.args = args;
  }

  public String getSender() { return sender; }

  public Vector<String> getArgs() { return args; }

  public EventArgEnum getEnum() { return new EventArgEnum(args); }

}
