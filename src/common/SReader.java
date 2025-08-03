/**
   $Id: SReader.java 4919 2006-09-04 21:00:00Z jan $
   $Source$

   Miscellanous functions

   (c) Michael Buro, licensed under GPL2      
*/

package common;

import java.io.*;

public class SReader extends StringReader
{
  static final boolean DBG = false;
  static final char commentChar = '#';

  public SReader(String s) { super(s); line = 1; }

  public int nextInt()
  {
    return Integer.parseInt(nextWord());
  }

  public int nextInt(boolean skipComments)
  {
    return Integer.parseInt(nextWord(skipComments));
  }

  public String nextWord()
  {
    return nextWord(false);
  }

  public String nextWord(boolean skipComment)
  {
    // return previous string if present
    
    if (prev != null) {
      // use previous word
      String p = prev;
      prev = null;
      return p;
    }
    
    // skip spaces (and # comments if flag is true)

    int c;
    boolean inComment = false;
    
    try {
      do {
	c = read();
        if (DBG)
          Misc.msg("read1: " + c + " " + (char)c + " " + skipComment);
        if (c == commentChar) { inComment = true; }
        if (c == '\n') { line++; inComment = false; }
      } while (c == ' ' || c == '\n' || (skipComment && inComment && c >= 0));

      if (c < 0)
	return null;

      // read word

      StringBuffer sb = new StringBuffer();

      while (c >= 0 && c != ' ' && c != '\n' &&
             (!skipComment || c != commentChar)) {
	sb.append((char)c);
	c = read();
        if (DBG) 
          Misc.msg("read2: " + c);        
      }

      if (skipComment && c == commentChar) {
        // skip rest of line
        do {
          c = read();
          if (c == '\n') break;
        } while (c >= 0);
      }
      
      if (c == '\n') line++;
    
      return sb.toString();
    }

    catch (IOException e) {
      return null; // should not happen
    } 
  }

  public int getLine()
  {
    return line;
  }
  
  /** reread string */
  public void pushBack(String s)
  {
    prev = s;
  }

  /** return remainder of stream */
  public String rest()
  {
    StringBuffer sb = new StringBuffer();
    int c;

    try {
      for (;;) {
	c = read();
	if (c < 0) break;
	sb.append((char)c);
      }
    }
    catch (IOException e) {
      return null; // should not happen
    } 

    return sb.toString();
  }

  String prev = null;
  int line = 0;
}
