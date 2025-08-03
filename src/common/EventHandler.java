/* $Id: EventHandler.java 5874 2007-09-18 07:12:51Z mburo $
   
   (c) Michael Buro, licensed under the GPL2
*/

package common;

public interface EventHandler
{
  /** @return true iff handled */
  boolean handleEvent(EventMsg msg);
}

