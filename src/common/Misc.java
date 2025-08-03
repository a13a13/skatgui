/**
   $Id: Misc.java 11025 2012-01-17 07:09:32Z mburo $
   $Source: /usr/bodo1/cvs/cvssoftware/skatgui/src/skatgui/Misc.java,v $

   Miscellanous functions

   (c) Michael Buro, licensed under GPL2
*/

package common;

import java.util.*;
import java.awt.Desktop;
import java.util.regex.*;
import java.io.*;
import java.net.*;
import java.text.*;
import javax.sound.sampled.*;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;

public class Misc
{
  static boolean logging;
  static DataOutputStream out;
  public static String logBaseName;
  public static final String logFileExt = "-log.txt";
  public static final String reportFileExt = "-bugreport.txt";  

  //  static public String issHome = "http://skatgame.net/iss";
  static public String issHome = "http://skat.dnsalias.net/iss";  
  
  public static String getReportFile() { return logBaseName + reportFileExt; }
  public static String getLogFile() { return logBaseName + logFileExt; }

  public static String version() { return MajorVersion.major + "." + MinorVersion.minor; }

  public static String lock = new String("LOCK");
  public static Locale locEn = new Locale("en"); // for format

  static final String[] browsers = { "google-chrome", "firefox", "opera",
                                     "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla" };
  static final String errMsg = "Error attempting to launch web browser";

  private static String i18n(String s) {
    return "_" + s;
  }

  /** return true iff major revision equal */
  public static boolean versionMatch(String v) {
    String[] parts = v.split("\\.");
    Misc.msg("v="+parts[0] + " " + parts[1]);
    if (parts.length == 0 || parts[0] == null) return false;
    return parts[0].equals(MajorVersion.major+"");
  }

  public static int intOf(boolean f)
  {
    return f ? 1 : 0;
  }

  /** beep */
  public static void beep(int hz, int msecs, int volume)
  {
    float SAMPLE_RATE = 8000f;

    try {
      byte[] buf = new byte[1];
      AudioFormat af = new AudioFormat(SAMPLE_RATE,8,1,true,false);
      SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
      sdl.open(af);
      sdl.start();
      for (int i=0; i<msecs*8; i++) {
	double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
	buf[0] = (byte)(Math.sin(angle) * volume);
	sdl.write(buf,0,1);
      }
      sdl.drain();
      sdl.stop();
      sdl.close();
    }
    catch (Exception e) { }
  }


  /** read file to string */
  public static String readFile(String file) throws Exception
  {
    // FileReader input = null;
    StringBuffer sb = new StringBuffer();
    Reader input = new InputStreamReader(new FileInputStream(file), "UTF-8");
    
    //input = new FileReader(file);

    for (;;) {
      int c = input.read();
      if (c < 0) break;
      sb.append((char)c);
    }
    
    input.close();
    return sb.toString();
  }

  /** read file from url */
  public static String readFile(URL url) throws Exception
  {
    InputStream in = url.openStream();
    BufferedReader dis =
      new BufferedReader (new InputStreamReader(in));
    StringBuffer fBuf = new StringBuffer() ;
    String line;
    
    while ((line = dis.readLine()) != null) {
      fBuf.append (line + "\n");
    }
    
    in.close();
    return fBuf.toString();
  }
  
  /** read line, if line too long throw exception */
  public static String readLine(BufferedReader in, int maxChars)
    throws IOException,LineTooLongException
  {
    if (maxChars <= 0) // read line - no restriction
      return in.readLine();

    // read line (but at most maxChars characters)

    StringBuffer sb = new StringBuffer();
    int i;
    for (i=0; i < maxChars; i++) {
      int c = in.read();
      if (c < 0) return null; // eos
      char d = (char)c;
      if (d == '\n') break; // line complete
      if (d == '\r') continue;
      sb.append(d);
    }

    if (i >= maxChars) throw new LineTooLongException();

    return sb.toString();
  }

  public static byte[] computeMD5(String m)
  {
    MD5 md5 = new MD5();
    
    for (int j=0; j < m.length(); j++) {
      byte b = (byte)m.charAt(j);
      md5.engineUpdate(b);
    }

    return md5.engineDigest();
  }


  public static String validLanguages(String s)
  {
    if (s.equals("-")) return null; // no language
    
    for (int i=0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == 'E') continue;
      if (c == 'D') continue;
      if (c == 'F') continue;
      if (c == 'P') continue;
      if (c == 'S') continue;
      if (c == 'C') continue;
      return i18n("Lang_char_invalid_colon") + " " + c;
    }
    return null;
  }    
  

  /** send email, return null if OK, error message otherwise */
  public static String sendEmail(String receiver, String subject, String msg)
  {
    String r = validEmail(receiver);
    if (r != null) 
      return r + " : " + receiver;
    
    if (subject == null || subject.isEmpty())
      subject = "???";
    
    String[] cmd = new String[] {
      "mail", "-s", subject, receiver
    };

    Process p = null;
    try {
      p = Runtime.getRuntime().exec(cmd);
    } catch (IOException ex) {
      Misc.msg(ex.toString());
      return "sendmail error: exec failed";
    }

    OutputStream out = p.getOutputStream();

    int c;
    try {
      for (int i=0; i < msg.length(); i++) {
        out.write((byte)msg.charAt(i));
      }
      out.write((byte)'\n');
      out.close();
    } catch (IOException ex1) {
      return "write error";
    }

    InputStream in = p.getInputStream();
    try {
      while ((c = in.read()) != -1) {
        System.out.print((char) c);
      }
      in.close();        
    } catch (IOException ex1) {
    }

    int exitVal = 0;
    try {
      exitVal = p.waitFor();
    } catch (InterruptedException ex3) { 
    }

    if (exitVal != 0)
      return "exit value " + exitVal;

    return null;
  }


  /** for transmitting text as one line */
  public static String encodeCRLF(String s)
  {
    StringBuffer sb = new StringBuffer();

    // replace \r by 0x01 and \n by 0x02
    for (int i=0; i < s.length(); i++) {
      char c = s.charAt(i), d = c;
      if (c == '\r')
        d = '\u0001';
      else if (c == '\n')
        d = '\u0002';
      else if (c == '\u0001')
        d = 'X';
      else if (c == '\u0002')
        d = 'Y';
      sb.append(d);
    }

    return sb.toString();
  }


  /** recover line breaks */
  public static String decodeCRLF(String s)
  {
    StringBuffer sb = new StringBuffer();

    for (int i=0; i < s.length(); i++) {
      char d = s.charAt(i), c = d;
      if (d == '\u0001')
        c = '\r';
      else if (d == '\u0002')
        c = '\n';
      sb.append(c);
    }

    return sb.toString();
  }

  
  public static String charArrayToString(char[] cs)
  {
    String s = "";
    for (char c : cs) {
      s += c;
    }
    return s;
  }
  
  /** @return null if password is OK, errmsg otherwise */
  public static String validPassword(String id)
  {
    if (id == null)
      return i18n("Empty_pw");

    if (id.length() < 3)
      return i18n("Pw_needs_atleast_3_chars");

    for (int i=0; i < id.length(); ++i) {
      char c = id.charAt(i);
      if (c >= '!' && c <= '~') continue;
      return i18n("Pw_ASCII_only");
    }

    return null;
  }


  /** @return null if password is OK, errmsg otherwise */
  public static String validEmail(String id)
  {
    if (id == null || id.equals(""))
      return i18n("Empty_email");

    for (int i=0; i < id.length(); ++i) {
      char c = id.charAt(i);
      if (c >= '!' && c <= '~') continue;
      return i18n("Email_ASCII_only");
    }

    if (id.indexOf('@') < 0 || id.indexOf('.') < 0)
      return i18n("Email_missing_essential_chars");

    return null;
  }


  /** @return null if user id is OK, errmsg otherwise */
  public static String validId(String id)
  {
    if (id == null)
      return i18n("Empty_id");

    if (id.length() < 3 || id.length() > 8)
      return i18n("Name_needs_atleast_3_chars");

    char c = id.charAt(0);
    if (! ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')))
      return i18n("Name_needs_first_char_alphabetic");
    
    for (int i=0; i < id.length(); ++i) {
      c = id.charAt(i);
      if (c == ':' || c < '!' || c > '~') 
        return i18n("Name_ASCII_only");
    }

    return null;
  }

  /** @return the id minus :* (representing multiple instances of the same player) */
  public static String getUniqueId(String id) {
    int index = id.indexOf(":");
    if (index > 0) {
      return id.substring(0, index);
    }
    else {
      return id;
    }

  }

  
  public static char toHexChar(int i)
  {
    if ((0 <= i) && (i <= 9 ))
      return (char)('0' + i);
    else
      return (char)('a' + (i-10));
  }

  public static void sleep(int n) 
  {
    try { Thread.sleep(n); }
    catch(InterruptedException e) {;}
  }
  
  public static String center(String s, int len)
  {
    int l = s.length();
    
    if (len < l) {
      
      return s.substring(0, len);
      
    } else {
      
      String t = "";
      for (int i = 0; i < (len-l)/2; i++) t += ' ';
      t += s;
      for (int i = 0; i < (len-l+1)/2; i++) t += ' ';      
      return t;
    }
  }
 
  public static String[] vec2arr(Vector v)
  {
    int n = v.size();
    String[] a = new String[n];
    for (int i=0; i < n; ++i) {
      a[i] = (String) v.elementAt(i);
    }
    return a;
  }

  /**
   * Removes the first String item from the array
   * 
   * @param a The input array
   * @return The output array
   */
  public static String[] remove_first(String[] a) 
  {
    String[] result;	  
    int n = a.length;
	  
    if (n > 0) {
		
      // only remove the first token if there is at least one toke in the array
      result = new String[n-1];
	
      for (int i=0; i < n - 1; ++i) {
	result[i] = a[i+1];
      }
    } else {
		  
      // otherwise return empty array
      result = a;
    }
	  
    return result;
  }


  static public String[] split(String s, String regexp)
  {
    String[] parts = null;

    try { parts = s.split(regexp, 0); }
    catch (PatternSyntaxException e) {
      Misc.err("pattern syntax error: " + regexp);
    }

    return parts;
  }

  /** Splits message intended for a JOptionPane into lines so that line breaks do not have
      to be inserted manually. */
  static public String splitOptionPaneMessage(String msg, int charsPerLine, boolean usingHTML) {
    if (msg.length() <= charsPerLine)
      return msg;
    
    ArrayList<String> lineBreaks = new ArrayList<String>();
    lineBreaks.add("\n");
    lineBreaks.add("<br />");

    int charsInLine = 0;
    int lastBreakIndex = 0;
    StringBuffer newMsg = new StringBuffer();
    
    for (int index = 0; index < msg.length(); index++) {
      if (charsInLine == 0) {
        boolean appendedLine = false;
        
        for (String s : lineBreaks) {
          int j = msg.indexOf(s, lastBreakIndex);
          if (j == -1)
            break;
          if (j > index + charsPerLine)
            break;

          newMsg.append(msg.substring(index, j - 1));
          index = j + s.length() - 1;
          lastBreakIndex = j + 1;
          appendedLine = true;
        }

        if (appendedLine)
          continue;
      }

      for (String s : lineBreaks) {
        int j = msg.indexOf(s, lastBreakIndex);
        
        if (index == j) {
          newMsg.append(msg.substring(index, index + s.length() - 1));
          lastBreakIndex = index + 1;
          index += s.length() - 1;
          charsInLine = 0;
          continue;
        }
      }

      char symbol = msg.charAt(index);
      if (symbol == ' ') { // space
        int charsUntilNextSpace = msg.indexOf(" ", index + 1) - index;
        int charsInLineAtNextSpace = charsUntilNextSpace + charsInLine;

        if (charsInLineAtNextSpace > charsPerLine) {
          if (usingHTML)
            newMsg.append("<br />");
          else
            newMsg.append("\n");

          charsInLine = 0;
          lastBreakIndex = index + 1;
        } else {
          newMsg.append(symbol);
          charsInLine++;
        }
      } else {
        newMsg.append(symbol);
        charsInLine++;
      }
    }

    return newMsg.toString();
  }

  synchronized public static void msg(String msg)
  {
    /*
      if (msg.equals("H")) {
      StringWriter sw = new StringWriter();
      new Throwable("").printStackTrace(new PrintWriter(sw));
      System.out.println(sw.toString());
      }
    */
    
    System.out.println(msg);
    log(msg);
  }

  synchronized public static void warn(String msg)
  {
    msg("WARN: " + msg);
  }

  synchronized public static void err(String s)
  {
    err(s, 20);
  }

  synchronized public static void err(String s, int val)
  {
    try {
      throw new Exception("encountered error");
    }
    catch (Exception e) {
      s = s + "\n" + stack2string(e);
    }

    msg("ERR: " + s);

    if (logging) {

      // rename logfile to bugreport file

      try {
	out.close(); out.flush();
        (new File(getReportFile())).delete();
        File rf = new File(getReportFile());        
	File lf = new File(getLogFile());
	lf.renameTo(rf);
      }
      catch (Exception e) {
	System.out.println("Exception when renaming logfile " + e.toString());
	System.exit(-1);
      }
    }
    
    System.exit(val);
  }

  synchronized public static void exception(Throwable e)
  {
    Misc.msg(stack2string(e));
    Misc.err("EXCEPTION - STOP");
  }

  static void log(String s)
  {
    if (!logging) return;

    try {
      out.writeBytes("["+currentUTCdate()+"] " + s + "\n");
    }
    catch (Exception f) {
      System.err.println("error logging to file " + logBaseName + logFileExt);
      System.exit(-1);
    }
  }


  public static String currentUTCdate()
  {
    Date now = new Date();
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd/HH:mm:ss/z");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return df.format(now);
  }
  
  public static String currentTime()
  {
    Date now = new Date();
    DateFormat df = new SimpleDateFormat("HH:mm:ss");
    // df.setTimeZone(TimeZone.getTimeZone(""));
    return df.format(now);
  }
  
  /** @return number of set bits in x */
  public static int popCount(int x)
  {
    final int m1 = 0x55555555;
    final int m2 = 0x33333333;
    final int m4 = 0x0f0f0f0f;
    x -= (x >>> 1) & m1;
    x = (x & m2) + ((x >>> 2) & m2);
    x = (x + (x >>> 4)) & m4;
    x += x >>>  8;
    return (x + (x >>> 16)) & 0x3f;
  }

  // ~2 times slower
  public static int popCount2(int x)
  {
    int c = 0;
    while (x != 0) {
      x &= (x-1);
      c++;
    }
    return c;
  }

  /* n0 = # of cards only available in h0
     n1 = # of cards only available in h1
     n01 = # of shared cards
     s0 = # of cards to allocate to p0
     s1 = # of cards to allocate to p1
  */

  public static int combCount(int n0, int n1, int n01, int s0, int s1)
  {
    if (n0 > s0) return 0;
    if (n1 > s1) return 0;
    if ((n0 + n1 + n01) != (s0 + s1)) return 0;
    s0 -= n0;
    s1 -= n1;
    return choose(n01, s0);
  }
  
  public static long rotateLeft(long x, int n)
  {
    n &= 63;
    // Misc.msg("x=" + x + " n=" + n + " " + ((x << n) | (x >>> (64-n))));
    return (x << n) | (x >>> (64-n));
  }
  
  public static void setLogFile(String name)
  {
    logBaseName = name;
    name = logBaseName + logFileExt;
    File myFile = new File(name);

    try {
      myFile.delete();
      out = new DataOutputStream(new FileOutputStream(name, true));
    }
    catch (Exception f) {
      Misc.err("error opening logfile " + name);
      return;
    }
    logging = true;
    Misc.msg("logging to file " + name);
  }


  public static String stack2string(Throwable e) {
    try {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      return sw.toString();
    }
    catch(Throwable e2) {
      return "bad stack2string";
    }
  }

  // @return null if browser is launched
  // and error message otherwise
  public static String openURL(String url)
  {
    if (Desktop.isDesktopSupported()) {

      Desktop desktop = Desktop.getDesktop();

      if (desktop.isSupported(Desktop.Action.BROWSE)) {

	URI uri= null;

	try { uri = new URI(url); }
	catch (Throwable t) { return "illformed url: " + url; }
	try { desktop.browse(uri); }
	catch (Throwable t) { return "i/o error: " + url + " " + t.toString(); }
	return null;
      }
    }

    return "can't launch browser";
  }

  /** @return true if the on-bits in m are a subset of the on-bits in n */
  public static boolean bitSubset(int n, int m) {
    return (m & ~n) == 0;
    /*
      boolean answer = true;
      for (int i = 0; i < 32; i++) {
      if ( ((1 << i) & m) != 0) {
      // If the bit is set in m...
      if ( ((1 << i) & n) == 0) {
      // If it's not set in n, we're done
      answer = false;
      break;
      }
      }
      }
      return answer;
    */
  }

  // Calculates n-choose-m
  public static int choose(int n, int m)
  {
    if (m > n || m < 0 || n < 0) {
      return 0;
    }
    int ans=1;
    for (int x = 0; x < m; x++)
      ans=(ans*(n-x))/(x+1);
    return ans;
  }

  /** Unranks the deal corresponding to index in state s, and stores the result in h.  H must be of size 4.
      No regard to any particular player's perspective.
  */
  public static void unrank(int index, SimpleState s, int[] h) {

    int pubCards = Misc.getPublicCards(s);

    // Disassembling the mixed radix number into hand ranks
    for (int p = 0; p < 3; p++) {
      int mult = 1;      
      
      int numOut = 32 - Misc.popCount(pubCards);
      for (int p2 = 0; p2 < p+1; p2++) {
        if ( !((s.getGameDeclaration().ouvert) && (p2 == s.getDeclarer())) ) {
          numOut -= s.numCards(p2);
        }
      }
      for (int p2 = p+1; p2 < 3; p2++) {
        if ( !((s.getGameDeclaration().ouvert) && (p2 == s.getDeclarer())) ) {
          mult *= Misc.choose(numOut, s.numCards(p2));
          numOut -= s.numCards(p2);
        }
      }
      
      if ( !((s.getGameDeclaration().ouvert) && (p == s.getDeclarer())) ) {
        ranks[p] = index / mult;
        index -= (ranks[p] * mult);
      } 
      else { 
        ranks[p] = 0;
      }
    }

    for (int p = 0; p < 3; p++) {
      //Misc.msg("Trying to unrank: " + ranks[p]);
      if ( !((s.getGameDeclaration().ouvert) && (p == s.getDeclarer())) ) {
        h[p] = Hand.colexUnrank(ranks[p], s.numCards(p), pubCards);
        pubCards ^= h[p];
      }
      else {
        h[p] = s.getHand(p);
      }
    }

    // h[3] (the skat) must be whatever's left
    h[3] = -1 ^ s.getPlayedCards() ^ h[0] ^ h[1] ^ h[2];

  }

  /** @return The rank of a given world in its entirety, with no regard to any particular player's perspective.
   */
  static int[] hands = new int[3];
  static int[] ranks = new int[3];

  public static int rank(SimpleState s) {

    int finalRank = 0;

    // Remember, the skat is implicit given the other cards
    for (int p = 0; p < 3; p++) {
      hands[p] = s.getHand(p);
    }
   
    int pubCards = Misc.getPublicCards(s);

    for (int p = 0; p < 3; p++) {
    
      //Misc.msg("Rank should be: " + Hand.colexRank(hands[p], pubCards));
      if ((s.getGameDeclaration().ouvert) && (p == s.getDeclarer())) {
        ranks[p] = 0;
      }
      else {
        ranks[p] = Hand.colexRank(hands[p], pubCards);
        pubCards ^= hands[p];
      }
    }

    // Now forming the mixed radix number

    for (int p = 0; p < 3; p++) {
      int mult = 1;      
      
      int numOut = 32 - Misc.popCount(Misc.getPublicCards(s));
      for (int p2 = 0; p2 < p+1; p2++) {
        if ( !((s.getGameDeclaration().ouvert) && (p2 == s.getDeclarer())) ) {          
          numOut -= s.numCards(p2);
        }
      }
      for (int p2 = p+1; p2 < 3; p2++) {
        if ( !((s.getGameDeclaration().ouvert) && (p2 == s.getDeclarer())) ) {
          mult *= Misc.choose( numOut, s.numCards(p2));
          //Misc.msg("Mult is: " + mult);
          numOut -= s.numCards(p2);
        }
      }  
      
      finalRank += ranks[p] * mult;
      //Misc.msg("Final rank is now: " + finalRank);      
    }

    return finalRank;
    
  }

  public static HandDeal unrank(int index, SimpleState s, int p) {
    return unrank(index, s, p, false);
  }

  public static HandDeal unrankGlobal(int index, SimpleState s, int p) {
    return unrank(index, s, p, true);
  }

  /** @return A simple object containing the hands of the other 2 players and the skat, given a colex rank index, in the state s from perspective of player p.
      If global, we worry about the state's rank from the start of the game, before any cards have been played
  */

  public static HandDeal unrank(int index, SimpleState s, int p, boolean global) {
    //Misc.msg("Called unrank, with index: " + index);
    //Misc.msg("Player: " + p);
    // Misc.msg("State: " + s.toString());    

    int p2 = (p+1)%3;
    int p3 = (p2+1)%3;
    int p2hand = 0;
    int p3hand = 0;
    
    int k;
    if (!global) {
      k = s.numCards(p2);
    }
    else {
      k = 10;
    }
  
    int cardsOut;
    if (!global) {
      cardsOut = Misc.getKnownCards(s, p);
    }
    else {
      cardsOut = s.getHand(p) ^ s.getPlayedCards(p);
    }
    
    

    int n = 32 - (Hand.numCards(cardsOut));
    int skatrank = 0;
    int skat = 0;

    // First, deal with ouvert games
    if ( (p != s.getDeclarer()) && (s.getGameDeclaration().ouvert) ) {
      skat = Hand.colexUnrank(index, 2, cardsOut);
      if (s.getDeclarer() == p2) {
        p2hand = s.getHand(p2);
        p3hand = -1 ^ cardsOut ^ skat;
      }
      else {
        p3hand = s.getHand(p3);
        p2hand = -1 ^ cardsOut ^ skat;
      }
      HandDeal ouverthd = new HandDeal(p2hand, p3hand, skat);
      return ouverthd;
    }

    if (s.skatKnown(p)) {
      skat = s.getSkat();
      skatrank = 0;     
      
    }
    else {
      skatrank = index/Misc.choose(n-2, k);
      skat = Hand.colexUnrank(skatrank, 2, cardsOut);
    }
    int handrank = index - (skatrank * Misc.choose(n-2, k));
        
    cardsOut = cardsOut ^ skat;    
    p2hand = Hand.colexUnrank(handrank, k, cardsOut);    
    p3hand = -1 ^ cardsOut ^ p2hand;
    HandDeal hd = new HandDeal(p2hand, p3hand, skat);
   
    return hd;

  }  

  public static int rank(SimpleState s, int p) {
    return rank(s, p, false);
  }

  public static int rankGlobal(SimpleState s, int p) {
    return rank(s, p, true);
  }

  /** @return The rank of a given world from the perspective of player p, using a mixed radix number combination of skatrank and handrank and a colex ordering 
      If global, we worry about the state's rank from the start of the game, before any cards have been played
  */
  public static int rank(SimpleState s, int p, boolean global) {

    int rank = 0;
    int skat = 0;
    int hand = s.getHand(p);
    int knownCards;
    if (!global) {
      knownCards = Misc.getKnownCards(s, p);
    }
    else {
      knownCards = s.getHand(p) ^ s.getPlayedCards(p);
    }
    
    skat = s.getSkat();
    
    
    int skatrank = Hand.colexRank(skat, knownCards);

    if ( (p != s.getDeclarer()) && (s.getGameDeclaration().ouvert) ) {
      // In ouvert games, once we decide the skat, we know everything
      return skatrank;
    }
   
    int cardsOut = knownCards ^ skat;
    int p2 = (p+1)%3;
    int p2hand = s.getHand(p2);
    int p2handRank = Hand.colexRank(p2hand, cardsOut);

    //Misc.msg("Skatrank: " + skatrank);
    //Misc.msg("handrank: " + p2handRank);

    if (s.skatKnown(p)) {
      rank = p2handRank;
    }
    else {
      rank = skatrank * Misc.choose((30 - Hand.numCards(knownCards)), Hand.numCards(p2hand)) + p2handRank;
    }

    return rank;
  }

  /** @return The rank of a given world from the perspective of player p with regard to his own possible info sets, using a mixed radix number combination of skatrank and handrank and a colex ordering */
  public static int infoSetRank(SimpleState s, int p) {

    int rank = 0;
    int skat = 0;    
    int skatrank = 0;
    int knownCards = Misc.getPublicCards(s);

    if (s.skatKnown(p)) {
      skat = s.getSkat();
      skatrank = Hand.colexRank(skat, knownCards);
    }
    int phand = s.getHand(p);
    int phandRank = Hand.colexRank(phand, knownCards);
 

    if ( (p != s.getDeclarer()) && (s.getGameDeclaration().ouvert) ) {
      // In ouvert games, once we decide the skat, we know everything
      return Hand.colexRank(phand, knownCards);
    }
    else if ( (s.getGameDeclaration().ouvert) && (s.skatKnown(p)) ) {
      return skatrank;
    }
    else if (s.getGameDeclaration().ouvert) {
      return 0; // There is only one info set for the declarer in ouvert games if he doesn't know skat
    }

    //Misc.msg("Skatrank: " + skatrank);
    //Misc.msg("handrank: " + p2handRank);

    if (!s.skatKnown(p)) {
      rank = phandRank;
    }
    else {
      // Have to add the skat into known cards if we know the skat and are forming the mixed radix number
      knownCards ^= skat;
      phandRank = Hand.colexRank(phand, knownCards);
      rank = skatrank * Misc.choose((32 - Hand.numCards(knownCards)), Hand.numCards(phand)) + phandRank;
    }

    return rank;
  }

  /** Returns the rank from 0 to 12-choose-2 of the given discard for the 10-card hand h
   */
  public static int discardRank(int h, int discard) {
    int cardsOut = -1 ^ h ^ discard;
    return Hand.colexRank(discard, cardsOut);
  }

  /** Returns the discard corresponding to rank r from the 12-card hand h
   */
  public static int discardUnrank(int h, int r) {
    int cardsOut = -1 ^ h;
    return Hand.colexUnrank(r, 2, cardsOut);
  }

  /** @return The cards known by all players in SimpleState s.
   */
  public static int getPublicCards(SimpleState s) {

    int cards = 0;    
    cards = cards ^ s.getPlayedCards();      
    
    if (s.getGameDeclaration().ouvert) {
      cards ^= s.getHand(s.getDeclarer());
    }
    return cards;

  }

  /** @return The cards known by player p in SimpleState s, not regarding the skat 
   */
  public static int getKnownCards(SimpleState s, int p) {

    int cards = 0;
    cards = cards ^ s.getHand(p);
    for (int i = 0; i < 3; i++) {
      cards = cards ^ s.getPlayedCards(i);
      if ( (i != p) && (i == s.getDeclarer()) && (s.getGameDeclaration().ouvert)) {
        cards = cards ^ s.getHand(s.getDeclarer());
      }
    }

    return cards;

  }

  /** The total number of deals that are possible at the current stage of game g. 
   */
  public static int numWorldsTotal(SimpleGame g) {
    int answer = 0;
    SimpleState s = g.getCurrentState();
    int out = -1 ^ Misc.getPublicCards(s);

    int numOut = Misc.popCount(out);

    // The cards in the skat
    answer = Misc.choose(numOut, 2);
    numOut -= 2;

    if (s.getGameDeclaration().ouvert) {
      for (int p = 0; p < 3; p++) {
        if (p != s.getDeclarer()) {
          answer *= Misc.choose(numOut, s.numCards(p));
          numOut -= s.numCards(p);
        }
      }
    }
    else {
      for (int p = 0; p < 3; p++) {        
        answer *= Misc.choose(numOut, s.numCards(p));
        numOut -= s.numCards(p);
      }
    }
    return answer;
  }

  /** The number of information sets that player p has at the current state of the game g.
   */
  public static int numWorlds(SimpleGame g, int p) {
    SimpleState s = g.getCurrentState();

    int publicCards = Misc.getPublicCards(s);
    int out = -1 ^ publicCards;

    if (s.getGameDeclaration().ouvert) {
      if (p != s.getDeclarer()) {
        return Misc.choose(Misc.popCount(out), s.numCards(p));
      }
      else {
        if (s.skatKnown(p)) {
          return Misc.choose(Misc.popCount(out), 2);
        }
        else {
          return 1;
        }
      }
    }

    if (!s.skatKnown(p)) {    
      return Misc.choose(Misc.popCount(out), s.numCards(p));
    }
    else {
      return ( Misc.choose(Misc.popCount(out), 2) * Misc.choose((Misc.popCount(out) - 2), s.numCards(p)) );
    }

  }

  // Taylor expansion
  static float fastExp5(float x)
  {
    return (120.0f+x*(120.0f+x*(60.0f+x*(20.0f+x*(5.0f+x)))))*0.0083333333f;
  }
  
  // max relative error in -8..8 : 1.000033
  static public float fastExp(float x) {
    boolean neg = x < 0;
    if (neg) x = -x;
    float y;
    
    if       (x < 1.0f) y =    1.6487212707f * fastExp5(x-0.5f);
    else if  (x < 2.0f) y =    4.4816890703f * fastExp5(x-1.5f);
    else if  (x < 3.0f) y =   12.1824939607f * fastExp5(x-2.5f);
    else if  (x < 4.0f) y =   33.1154519586f * fastExp5(x-3.5f);
    else if  (x < 5.0f) y =   90.0171313005f * fastExp5(x-4.5f);
    else if  (x < 6.0f) y =  244.6919322f    * fastExp5(x-5.5f);
    else if  (x < 7.0f) y =  665.141633044f  * fastExp5(x-6.5f);
    else                y = 1808.042414456f  * fastExp5(x-7.5f);

    if (neg) return 1.0f/y;
    return y;
  }

  static public double pow2(double x) { return x*x; }
  static public double pow3(double x) { return x*x*x; }
  static public double pow4(double x) { return pow2(pow2(x)); }

  static public int booleanToInt(boolean b) {
    if (b)
      return 1;
    else
      return 0;
  }


  // fractional error in math formula less than 1.2 * 10 ^ -7.
  // although subject to catastrophic cancellation when z in very close to 0
  // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
  public static double erf(double z) {
    double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

    // use Horner's method
    double ans = 1 - t * Math.exp( -z*z   -   1.26551223 +
                                   t * ( 1.00002368 +
                                         t * ( 0.37409196 +
                                               t * ( 0.09678418 +
                                                     t * (-0.18628806 +
                                                          t * ( 0.27886807 +
                                                                t * (-1.13520398 +
                                                                     t * ( 1.48851587 +
                                                                           t * (-0.82215223 +
                                                                                t * ( 0.17087277))))))))));
    if (z >= 0) return  ans;
    else        return -ans;
  }
}
