// $Id$

// (c) Michael Buro, licensed under GPLv3

package common;

public interface MsgHandler
{
  public void handleMsg(String who, String what, long sessionId);
}
