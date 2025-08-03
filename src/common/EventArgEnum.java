/* $Id: EventArgEnum.java 5770 2007-09-03 19:08:48Z mburo $
   
   (c) Michael Buro, licensed under the GPL2
*/

package common;

import java.util.*;

public class EventArgEnum
{
  private int index;
  private Vector<String> vec;

  public EventArgEnum(Vector<String> args)
  {
    index = 0;
    vec = args;
  }  	
  
  public boolean has_more_args()
  {
    return index < vec.size();
  }  

  public String next_arg()
  {
    return vec.get(index++);
  } 

}
